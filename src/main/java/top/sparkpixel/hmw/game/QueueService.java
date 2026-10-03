package top.sparkpixel.hmw.game;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import top.sparkpixel.hmw.HoneyMissileWarsPlugin;
import top.sparkpixel.hmw.arena.ArenaVariant;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Matchmaking: the waiting room world IS the queue. The first /hmw join starts
 * a countdown; everyone who joins afterwards waits in the same batch. When the
 * countdown ends a fresh arena is cloned and the batch is teleported in and the
 * battle starts immediately (no lobby phase). Falls back to the configured
 * vanilla wait-zone world when no waiting room template exists.
 */
public final class QueueService {

    private final HoneyMissileWarsPlugin plugin;
    private final Deque<UUID> queue = new ConcurrentLinkedDeque<>();
    private final Map<UUID, ArenaVariant> preferences = new ConcurrentHashMap<>();
    private int countdownTicks = -1;

    public QueueService(HoneyMissileWarsPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // queue management
    // ------------------------------------------------------------------

    /** Adds a player to the queue, moves them into the waiting room world and starts the countdown. */
    public void enqueue(Player player, ArenaVariant preferred) {
        if (isQueued(player.getUniqueId())) {
            return;
        }
        this.queue.addLast(player.getUniqueId());
        if (preferred != null) {
            this.preferences.put(player.getUniqueId(), preferred);
        } else {
            this.preferences.remove(player.getUniqueId());
        }
        boolean first = this.countdownTicks < 0;
        if (first) {
            this.countdownTicks = this.plugin.hmwConfig().queueCountdownSeconds() * 20;
        }
        teleportToWaitingArea(player);
        player.sendMessage(this.plugin.messages().format("queue.joined",
                "<yellow>You are in the queue.</yellow> <gray>The match will start automatically."
                        + "</gray>"));
        showQueueStatus(player);
        if (first) {
            broadcast(this.plugin.messages().format("queue.countdown-started",
                    "<green>Countdown started - the match begins in {seconds}s</green>",
                    "{seconds}", String.valueOf(this.plugin.hmwConfig().queueCountdownSeconds())));
        }
    }

    public void removeFromQueue(Player player) {
        this.queue.remove(player.getUniqueId());
        this.preferences.remove(player.getUniqueId());
    }

    /** Puts a player back at the front of the queue (batch rollback). */
    public void requeueFirst(Player player) {
        this.queue.addFirst(player.getUniqueId());
    }

    public boolean isQueued(UUID playerId) {
        return this.queue.contains(playerId);
    }

    public int size() {
        return this.onlineQueued();
    }

    private int onlineQueued() {
        int count = 0;
        for (UUID id : this.queue) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                count++;
            }
        }
        return count;
    }

    private void showQueueStatus(Player player) {
        int max = this.plugin.hmwConfig().maxPlayers();
        int size = onlineQueued();
        player.sendActionBar(this.plugin.messages().formatRaw("queue.status",
                "<aqua>Queue <current>/<max></aqua>",
                "{current}", String.valueOf(size),
                "{max}", String.valueOf(max)));
    }

    /**
     * Moves a queued player into the waiting room world (or the legacy
     * wait-zone world while the waiting room template is missing/loading).
     */
    private void teleportToWaitingArea(Player player) {
        this.plugin.worldService().ensureWaitingRoomWorld().whenComplete((world, error) -> {
            if (error != null) {
                this.plugin.getLogger().warning("Failed to load the waiting room world: " + error);
            }
            Location target = world != null ? waitingAreaIn(world) : waitZone();
            if (target != null && player.isOnline()) {
                player.teleport(target);
            }
        });
    }

    /** Spawn point inside the waiting room world (configurable). */
    private Location waitingAreaIn(World world) {
        if (this.plugin.hmwConfig().waitZoneUseSpawn()) {
            return world.getSpawnLocation();
        }
        return new Location(world,
                this.plugin.hmwConfig().waitZoneX(),
                this.plugin.hmwConfig().waitZoneY(),
                this.plugin.hmwConfig().waitZoneZ(),
                this.plugin.hmwConfig().waitZoneYaw(),
                this.plugin.hmwConfig().waitZonePitch());
    }

    // ------------------------------------------------------------------
    // per-tick driver (called from GameManager.tick)
    // ------------------------------------------------------------------

    public void tick() {
        if (this.countdownTicks < 0) {
            // idle: show the queue status to everyone waiting (once per second)
            int online = onlineQueued();
            if (online > 0 && Bukkit.getCurrentTick() % 20 == 0) {
                for (UUID id : this.queue) {
                    Player player = Bukkit.getPlayer(id);
                    if (player != null && player.isOnline()) {
                        showQueueStatus(player);
                    }
                }
            }
            return;
        }

        if (onlineQueued() == 0) {
            this.countdownTicks = -1; // everybody left - cancel silently
            return;
        }
        this.countdownTicks--;
        if (this.countdownTicks > 0 && this.countdownTicks % 20 == 0) {
            int seconds = this.countdownTicks / 20;
            broadcast(this.plugin.messages().format("queue.countdown",
                    "<aqua>Match starting in {seconds}s</aqua>",
                    "{seconds}", String.valueOf(seconds)));
        }
        if (this.countdownTicks <= 0) {
            this.countdownTicks = -1;
            launch();
        }
    }

    /** Takes up to max-players from the queue and launches a fresh instance. */
    private void launch() {
        int max = this.plugin.hmwConfig().maxPlayers();
        int minToStart = this.plugin.hmwConfig().minPlayersToStart();
        List<Player> players = new java.util.ArrayList<>();
        ArenaVariant preferred = null;
        while (players.size() < max && !this.queue.isEmpty()) {
            UUID id = this.queue.poll();
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline()) {
                this.preferences.remove(id);
                continue;
            }
            players.add(player);
            ArenaVariant preference = this.preferences.remove(id);
            if (preferred == null) {
                preferred = preference;
            }
        }
        if (players.size() < minToStart) {
            // not enough anymore - put everyone back (preserving order) and restart the countdown
            for (int i = players.size() - 1; i >= 0; i--) {
                this.queue.addFirst(players.get(i).getUniqueId());
            }
            this.countdownTicks = this.plugin.hmwConfig().queueCountdownSeconds() * 20;
            broadcast(this.plugin.messages().format("queue.not-enough",
                    "<red>Not enough players to start, restarting the countdown.</red>"));
            return;
        }
        this.plugin.gameManager().createQueueGame(players,
                preferred != null ? preferred : this.plugin.hmwConfig().defaultArena());
    }

    private void broadcast(Component message) {
        for (UUID id : this.queue) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
                showQueueStatus(player);
            }
        }
    }

    // ------------------------------------------------------------------
    // legacy waiting area (fallback when no waiting room template exists)
    // ------------------------------------------------------------------

    /** The configured waiting area in a vanilla world, or null when it does not exist. */
    private Location waitZone() {
        World world = Bukkit.getWorld(this.plugin.hmwConfig().waitZoneWorld());
        if (world == null) {
            return null;
        }
        return waitingAreaIn(world);
    }
}
