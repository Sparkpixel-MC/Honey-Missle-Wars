package top.sparkpixel.hmw.game;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import top.sparkpixel.hmw.HoneyMissileWarsPlugin;
import top.sparkpixel.hmw.arena.ArenaGenerator;
import top.sparkpixel.hmw.arena.ArenaVariant;
import top.sparkpixel.hmw.item.MissileSet;
import top.sparkpixel.hmw.world.SlimeWorldService;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry + allocation of game instances, matchmaking queue wiring, hub
 * return logic and rejoin handling.
 * This is the entry point a frontend (NPC, queue plugin, /hmw join) talks to.
 */
public final class GameManager {

    /** Rejoin bookkeeping for players who disconnect mid-game. */
    private record RejoinEntry(String worldName, Team team, long deadline) {
    }

    /** Arena preference forwarded by the HMWLobby companion plugin via BungeeCord. */
    private record PendingArena(ArenaVariant arena, long expiresAt) {
    }

    private static final long PENDING_ARENA_TIMEOUT_MS = 20_000;

    private final HoneyMissileWarsPlugin plugin;
    private final QueueService queueService;
    private final Map<String, Game> games = new HashMap<>();
    private final Map<UUID, RejoinEntry> rejoinable = new HashMap<>();
    private final Map<UUID, PendingArena> pendingArenas = new ConcurrentHashMap<>();
    private int nextId = 1;

    public GameManager(HoneyMissileWarsPlugin plugin) {
        this.plugin = plugin;
        this.queueService = new QueueService(plugin);
    }

    public Collection<Game> games() {
        return List.copyOf(this.games.values());
    }

    public QueueService queueService() {
        return this.queueService;
    }

    public Game gameOf(Player player) {
        for (Game game : this.games.values()) {
            if (game.session(player) != null) {
                return game;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // allocation
    // ------------------------------------------------------------------

    /**
     * Entry point for /hmw join and external frontends.
     * Queue mode: everyone waits in the waiting room world until the countdown
     * launches a fresh instance (nobody waits inside an arena lobby).
     * Lobby mode (queue disabled): joins an open lobby or creates one directly.
     */
    public void joinGame(Player player, ArenaVariant preferred) {
        if (gameOf(player) != null) {
            player.sendMessage(this.plugin.messages().format("join.already",
                    "<red>You are already in a game. Use /hmw leave first.</red>"));
            return;
        }
        if (this.plugin.hmwConfig().queueEnabled()) {
            this.queueService.enqueue(player, preferred);
            return;
        }
        Game found = findJoinable(preferred);
        if (found != null) {
            found.join(player);
            return;
        }
        if (this.games.size() >= this.plugin.hmwConfig().maxInstances()) {
            player.sendMessage(this.plugin.messages().format("join.full",
                    "<red>All <max> game instances are busy right now, try again in a moment.</red>",
                    "{max}", String.valueOf(this.plugin.hmwConfig().maxInstances())));
            return;
        }
        createInstance(List.of(player), preferred != null ? preferred : this.plugin.hmwConfig().defaultArena(),
                false);
    }

    private Game findJoinable(ArenaVariant preferred) {
        for (Game game : this.games.values()) {
            if (game.phase() != GamePhase.WAITING || game.isFull()) {
                continue;
            }
            if (preferred != null && game.settings().arena != preferred) {
                continue;
            }
            return game;
        }
        return null;
    }

    /**
     * Launches a fresh instance for a matchmaking batch: clones the template,
     * joins everyone, assigns random teams and starts the countdown.
     */
    public void createQueueGame(List<Player> players, ArenaVariant variant) {
        if (this.games.size() >= this.plugin.hmwConfig().maxInstances()) {
            for (Player player : players) {
                player.sendMessage(this.plugin.messages().format("join.full",
                        "<red>All <max> game instances are busy right now, try again in a moment.</red>",
                        "{max}", String.valueOf(this.plugin.hmwConfig().maxInstances())));
            }
            // put the batch back into the queue (preserve order)
            for (int i = players.size() - 1; i >= 0; i--) {
                this.queueService.requeueFirst(players.get(i));
            }
            return;
        }
        createInstance(players, variant, true);
    }

    /**
     * Creates a new game world (async template clone) and puts the given
     * players into it. When {@code autoStart} the match starts right away with
     * random teams (queue batches); otherwise players pick teams in the lobby.
     */
    private void createInstance(List<Player> players, ArenaVariant variant, boolean autoStart) {
        int id = this.nextId++;
        String worldName = SlimeWorldService.GAME_WORLD_PREFIX + id;
        for (Player player : players) {
            player.sendMessage(this.plugin.messages().format("join.creating",
                    "<yellow>Preparing a fresh arena for you...</yellow>"));
        }
        this.plugin.worldService().createGameWorld(worldName).whenComplete((world, error) -> {
            if (error != null) {
                this.plugin.getLogger().severe("Failed to create game world " + worldName + ": " + error);
                for (Player player : players) {
                    player.sendMessage(this.plugin.messages().format("join.failed",
                            "<red>Could not create a game instance, please contact an admin.</red>"));
                }
                return;
            }
            List<Player> stillOnline = players.stream().filter(Player::isOnline).toList();
            if (stillOnline.isEmpty()) {
                this.plugin.worldService().disposeGameWorld(worldName);
                return;
            }
            MissileSet set = this.plugin.hmwConfig().defaultMissileSet();
            Game game = new Game(this.plugin, id, worldName, world, variant, set);
            this.games.put(worldName, game);
            if (autoStart) {
                // queue path: build the arena first while the batch is still in the
                // waiting room, then teleport everyone in and start the battle
                // immediately - there is no lobby phase
                ArenaGenerator.generate(world, variant, 3, () -> {
                    List<Player> batch = players.stream().filter(Player::isOnline).toList();
                    if (batch.isEmpty()) {
                        disposeGame(game);
                        return;
                    }
                    for (Player player : batch) {
                        game.join(player);
                    }
                    game.randomTeams(null);
                    String startError = game.startImmediately();
                    if (startError != null) {
                        game.broadcast(this.plugin.messages().mini(startError));
                        disposeGame(game);
                    }
                });
                return;
            }
            for (Player player : stillOnline) {
                game.join(player);
            }
        });
    }

    public void leaveGame(Player player) {
        if (this.queueService.isQueued(player.getUniqueId())) {
            this.queueService.removeFromQueue(player);
            player.sendMessage(this.plugin.messages().format("queue.left",
                    "<gray>You left the queue.</gray>"));
            sendToHub(player);
            return;
        }
        Game game = gameOf(player);
        if (game == null) {
            player.sendMessage(this.plugin.messages().format("leave.not-in-game",
                    "<red>You are not in a game.</red>"));
            return;
        }
        if (game.phase() == GamePhase.RUNNING) {
            // /trigger endGame replacement: leaving mid-battle does not end the
            // match for everyone; the game ends once a team runs dry (tick check).
            game.broadcast(this.plugin.messages().format("leave.left",
                    "<gray><player> left the game.</gray>",
                    "{player}", player.getName()));
        }
        game.removePlayer(player);
        sendToHub(player);
        maybeDisposeIdle(game);
    }

    private void maybeDisposeIdle(Game game) {
        if (game.phase() == GamePhase.WAITING && game.playerCount() == 0) {
            disposeGame(game);
        }
    }

    // ------------------------------------------------------------------
    // hub return
    // ------------------------------------------------------------------

    /** Sends a player back to the hub world or the configured proxy server. */
    public void sendToHub(Player player) {
        String returnServer = this.plugin.hmwConfig().returnServer();
        if (returnServer != null && !returnServer.isBlank()) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bytes);
                out.writeUTF("Connect");
                out.writeUTF(returnServer);
                player.sendPluginMessage(this.plugin, "BungeeCord", bytes.toByteArray());
            } catch (IOException ex) {
                this.plugin.getLogger().warning("BungeeCord/Velocity connect failed: " + ex.getMessage());
            }
            player.setGameMode(GameMode.ADVENTURE);
            player.setFlying(false);
            player.setAllowFlight(false);
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            return;
        }
        World hub = Bukkit.getWorld(this.plugin.hmwConfig().hubWorld());
        if (hub == null && !Bukkit.getWorlds().isEmpty()) {
            hub = Bukkit.getWorlds().get(0);
        }
        if (hub == null) {
            return;
        }
        Location spawn = hub.getSpawnLocation();
        player.teleport(spawn);
        player.setGameMode(GameMode.ADVENTURE);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        player.setFireTicks(0);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    // ------------------------------------------------------------------
    // lifecycle
    // ------------------------------------------------------------------

    public void tick() {
        this.queueService.tick();
        for (Game game : List.copyOf(this.games.values())) {
            game.tick();
        }
    }

    public void disposeGame(Game game) {
        if (this.games.remove(game.worldName()) != null) {
            game.markDisposed();
            for (Player player : game.players()) {
                game.removePlayer(player);
                sendToHub(player);
            }
            this.plugin.worldService().disposeGameWorld(game.worldName());
            this.plugin.getLogger().info("Disposed game instance " + game.worldName());
        }
    }

    /** Called when a player quits; keeps a rejoin window open for running games. */
    public void handleQuit(Player player) {
        this.queueService.removeFromQueue(player);
        this.pendingArenas.remove(player.getUniqueId());
        Game game = gameOf(player);
        if (game == null) {
            this.rejoinable.remove(player.getUniqueId());
            return;
        }
        PlayerSession session = game.session(player);
        Team team = session != null ? session.team : null;
        game.removePlayer(player);
        if (game.phase() == GamePhase.RUNNING && team != null
                && this.plugin.hmwConfig().rejoinTimeoutSeconds() > 0) {
            long deadline = System.currentTimeMillis()
                    + this.plugin.hmwConfig().rejoinTimeoutSeconds() * 1000L;
            this.rejoinable.put(player.getUniqueId(), new RejoinEntry(game.worldName(), team, deadline));
        }
        maybeDisposeIdle(game);
    }

    /** Called when a player joins the server; restores them into a running game. */
    public void handleJoin(Player player) {
        RejoinEntry entry = this.rejoinable.remove(player.getUniqueId());
        if (entry == null) {
            joinFromLobby(player);
            return;
        }
        if (System.currentTimeMillis() > entry.deadline()) {
            joinFromLobby(player);
            return;
        }
        Game game = this.games.get(entry.worldName());
        if (game == null || game.phase() != GamePhase.RUNNING) {
            joinFromLobby(player);
            return;
        }
        PlayerSession session = new PlayerSession(player.getUniqueId(), entry.team());
        game.joinRunning(player, session);
        player.sendMessage(this.plugin.messages().format("rejoin",
                "<yellow>Welcome back! You have been returned to the battle.</yellow>"));
    }

    /** BungeeCord dedicated-server mode: everyone connecting joins the queue right away. */
    private void joinFromLobby(Player player) {
        if (!this.plugin.hmwConfig().autoJoinOnConnect() || gameOf(player) != null) {
            return;
        }
        joinGame(player, takePendingArena(player.getUniqueId()));
    }

    // ------------------------------------------------------------------
    // BungeeCord lobby forwarding (HMWLobby companion plugin)
    // ------------------------------------------------------------------

    /** Called by the BungeeCord channel listener with a forwarded arena preference. */
    public void receiveLobbyForward(UUID playerId, String arenaName) {
        ArenaVariant arena = null;
        try {
            arena = ArenaVariant.valueOf(arenaName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            // blank/unknown arena id: fall back to the configured default arena
        }
        this.pendingArenas.put(playerId,
                new PendingArena(arena, System.currentTimeMillis() + PENDING_ARENA_TIMEOUT_MS));
    }

    private ArenaVariant takePendingArena(UUID playerId) {
        PendingArena pending = this.pendingArenas.remove(playerId);
        if (pending == null || System.currentTimeMillis() > pending.expiresAt()) {
            return null;
        }
        return pending.arena();
    }

    public void shutdown() {
        for (Game game : List.copyOf(this.games.values())) {
            game.shutdownNow();
            disposeGame(game);
        }
        this.games.clear();
        this.rejoinable.clear();
    }
}
