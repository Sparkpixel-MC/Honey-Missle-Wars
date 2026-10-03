package top.sparkpixel.hmw.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import top.sparkpixel.hmw.Fills;
import top.sparkpixel.hmw.HoneyMissileWarsPlugin;
import top.sparkpixel.hmw.arena.ArenaGenerator;
import top.sparkpixel.hmw.arena.ArenaVariant;
import top.sparkpixel.hmw.integration.HmwApi;
import top.sparkpixel.hmw.item.MissileSet;
import top.sparkpixel.hmw.missile.MissileType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * One match instance: lobby phase in the map's lobby, countdown with in-place
 * arena regeneration, the battle itself and the ending sequence.
 * Faithful port of chop:loop / lobbyloop / gameloop / start2 / end.
 */
public final class Game {

    // Lobby constants (map lobby)
    public static final Location LOBBY_SPAWN = new Location(null, 0.5, 72, 0.5, 0f, 0f);

    private final HoneyMissileWarsPlugin plugin;
    private final int id;
    private final String worldName;
    private World world;
    private final GameSettings settings;
    private final GameScoreboard scoreboard = new GameScoreboard();
    private final Random random = new Random();

    private GamePhase phase = GamePhase.WAITING;
    private final Map<UUID, PlayerSession> sessions = new HashMap<>();
    private final long[] lastSelfBreakTick = {-1, -1};
    private final int[] breach = {0, 0};

    private int tick;
    private int countdownTicks;
    private int itemTimer = 1;
    private int itemSpawnTime = 130;
    private int autoStartTicks = -1;
    private boolean autoStartAnnounced;
    private boolean soloMode;
    private Team winner;
    private int endTicks;
    private BukkitTask arenaTask;
    private BukkitTask fireworksTask;
    private boolean disposed;

    public Game(HoneyMissileWarsPlugin plugin, int id, String worldName, World world,
                ArenaVariant arena, MissileSet missileSet) {
        this.plugin = plugin;
        this.id = id;
        this.worldName = worldName;
        this.world = world;
        this.settings = GameSettings.fromConfig(plugin, arena, missileSet);
        this.plugin.lobbyDisplayService().refresh(this, missileSet);
        updateSidebar();
    }

    // ------------------------------------------------------------------
    // basic accessors
    // ------------------------------------------------------------------

    public int id() {
        return this.id;
    }

    public String worldName() {
        return this.worldName;
    }

    public World world() {
        return this.world;
    }

    public GamePhase phase() {
        return this.phase;
    }

    public GameSettings settings() {
        return this.settings;
    }

    public Team winner() {
        return this.winner;
    }

    public boolean isDisposed() {
        return this.disposed;
    }

    public PlayerSession session(Player player) {
        return this.sessions.get(player.getUniqueId());
    }

    public Team teamOf(Player player) {
        PlayerSession session = this.sessions.get(player.getUniqueId());
        return session == null ? null : session.team;
    }

    public List<Player> players() {
        List<Player> players = new ArrayList<>();
        for (UUID uuid : this.sessions.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return players;
    }

    public List<Player> teamPlayers(Team team) {
        List<Player> players = new ArrayList<>();
        for (PlayerSession session : this.sessions.values()) {
            if (session.team == team) {
                Player player = Bukkit.getPlayer(session.playerId);
                if (player != null && player.isOnline()) {
                    players.add(player);
                }
            }
        }
        return players;
    }

    public int teamSize(Team team) {
        int count = 0;
        for (PlayerSession session : this.sessions.values()) {
            if (session.team == team) {
                count++;
            }
        }
        return count;
    }

    public int playerCount() {
        return players().size();
    }

    public boolean isFull() {
        return playerCount() >= this.plugin.hmwConfig().maxPlayers();
    }

    // ------------------------------------------------------------------
    // lobby phase
    // ------------------------------------------------------------------

    /** Adds a player to the lobby (port of chop:player_handling/new_lobby). */
    public void join(Player player) {
        PlayerSession session = new PlayerSession(player.getUniqueId(), null);
        this.sessions.put(player.getUniqueId(), session);
        this.scoreboard.assign(player, null);

        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        lobbyEffects(player);
        player.teleport(lobbySpawn());
        player.setRespawnLocation(lobbySpawn(), true);

        player.showTitle(Title.title(
                Component.text("Honey Missile Wars", NamedTextColor.GOLD),
                Component.text("by Chopper2112, Supersette, and kruthers", NamedTextColor.RED),
                Title.Times.times(java.time.Duration.ofMillis(500),
                        java.time.Duration.ofMillis(3000),
                        java.time.Duration.ofMillis(500))));
        player.sendMessage(this.plugin.messages().format("welcome",
                "<yellow>Welcome to Honey Missile Wars!</yellow> <gray>Stand on a team pad to pick a side,"
                        + " then press the Start button.</gray>"));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.9f);
        spawnWelcomeFirework(player);

        if (this.phase == GamePhase.WAITING) {
            this.plugin.lobbyDisplayService().refresh(this, this.settings.missileSet);
        }
        updateSidebar();
    }

    private Location lobbySpawn() {
        Location spawn = this.LOBBY_SPAWN.clone();
        spawn.setWorld(this.world);
        return spawn;
    }

    private void lobbyEffects(Player player) {
        long infinite = 100_000 * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, (int) infinite, 100, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, (int) infinite, 100, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, (int) infinite, 100, true, false, false));
    }

    private void spawnWelcomeFirework(Player player) {
        Firework firework = this.world.spawn(player.getLocation(), Firework.class);
        var meta = firework.getFireworkMeta();
        meta.addEffect(org.bukkit.FireworkEffect.builder()
                .with(org.bukkit.FireworkEffect.Type.STAR)
                .withColor(Color.fromRGB(16774912))
                .withFade(Color.fromRGB(1241087))
                .build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);
        firework.detonate();
    }

    /** Team selection pads (lobbyloop): join/leave via terracotta pads. */
    public void joinTeam(Player player, Team team) {
        PlayerSession session = this.sessions.get(player.getUniqueId());
        if (session == null || this.phase != GamePhase.WAITING) {
            return;
        }
        session.team = team;
        this.scoreboard.assign(player, team);
        player.sendActionBar(Component.text(team == Team.RED ? "You joined Red team." : "You joined Green team.",
                team.textColor()));
        player.playSound(player.getLocation(), Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.5f);
        updateSidebar();
    }

    public void leaveTeam(Player player) {
        PlayerSession session = this.sessions.get(player.getUniqueId());
        if (session == null || this.phase != GamePhase.WAITING) {
            return;
        }
        session.team = null;
        this.scoreboard.assign(player, null);
        player.sendActionBar(this.plugin.messages().formatRaw("game.team-left",
                "<gray>You left your team</gray>"));
        player.playSound(player.getLocation(), Sound.BLOCK_PISTON_CONTRACT, 1.0f, 0.8f);
        updateSidebar();
    }

    public void randomTeams(Player initiator) {
        if (this.phase != GamePhase.WAITING) {
            return;
        }
        List<Player> unassigned = new ArrayList<>();
        for (Player player : players()) {
            if (teamOf(player) == null) {
                unassigned.add(player);
            }
        }
        java.util.Collections.shuffle(unassigned, this.random);
        boolean red = this.random.nextBoolean();
        for (Player player : unassigned) {
            joinTeam(player, red ? Team.RED : Team.GREEN);
            red = !red;
        }
        if (initiator != null) {
            broadcast(this.plugin.messages().format("game.random-teams",
                    "<yellow>{player} created random teams.</yellow>",
                    "{player}", initiator.getName()));
            broadcastSound(Sound.BLOCK_PISTON_EXTEND, 1.0f, 0.0f);
        }
    }

    // ------------------------------------------------------------------
    // starting phase
    // ------------------------------------------------------------------

    /** Start button / auto start. Returns an error message (MiniMessage) or null on success. */
    public String startCountdown() {
        if (this.phase != GamePhase.WAITING) {
            return this.plugin.messages().raw("game.start-error-running",
                    "<red>The game is already running.</red>");
        }
        int red = teamSize(Team.RED);
        int green = teamSize(Team.GREEN);
        int total = red + green;
        if (total < this.plugin.hmwConfig().minPlayersToStart()) {
            return this.plugin.messages().raw("game.start-error-min-players",
                    "<red>There must be at least {min} player(s) to start.</red>",
                    "{min}", String.valueOf(this.plugin.hmwConfig().minPlayersToStart()));
        }
        if (red == 0 && green == 0) {
            return this.plugin.messages().raw("game.start-error-empty",
                    "<red>There must be at least one player to start.</red>");
        }
        this.autoStartTicks = -1;
        this.autoStartAnnounced = false;
        this.soloMode = red == 0 || green == 0;

        this.phase = GamePhase.STARTING;
        this.countdownTicks = this.plugin.hmwConfig().countdownSeconds() * 20;
        broadcastTitle(this.plugin.messages().formatRaw("game.starting-title", "<gold>Game Starting</gold>"),
                this.plugin.messages().formatRaw("game.starting-subtitle", "<gray>Prepare for war...</gray>"),
                5, 100, 20);
        broadcastSound(Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.0f);

        // rebuild the chosen arena in place while players wait in the lobby
        World arenaWorld = this.world;
        ArenaVariant variant = this.settings.arena;
        this.arenaTask = ArenaGenerator.generate(arenaWorld, variant, 3, () -> this.arenaTask = null);
        updateSidebar();
        return null;
    }

    /**
     * Queue path: the arena was generated while the batch was still in the
     * waiting room, so skip the 3-2-1 countdown and start the battle as soon
     * as everyone is teleported in. Returns an error message (MiniMessage)
     * or null on success.
     */
    public String startImmediately() {
        if (this.phase != GamePhase.WAITING) {
            return this.plugin.messages().raw("game.start-error-running",
                    "<red>The game is already running.</red>");
        }
        int red = teamSize(Team.RED);
        int green = teamSize(Team.GREEN);
        if (red + green < this.plugin.hmwConfig().minPlayersToStart()) {
            return this.plugin.messages().raw("game.start-error-min-players",
                    "<red>There must be at least {min} player(s) to start.</red>",
                    "{min}", String.valueOf(this.plugin.hmwConfig().minPlayersToStart()));
        }
        this.autoStartTicks = -1;
        this.autoStartAnnounced = false;
        this.soloMode = red == 0 || green == 0;
        beginGame();
        return null;
    }

    public void cancelCountdown() {
        if (this.phase != GamePhase.STARTING) {
            return;
        }
        if (this.arenaTask != null) {
            this.arenaTask.cancel();
            this.arenaTask = null;
        }
        this.phase = GamePhase.WAITING;
        broadcastTitle(this.plugin.messages().formatRaw("game.cancelled-title", "<red>Cancelled</red>"),
                Component.empty(), 0, 20, 20);
        broadcastSound(Sound.BLOCK_PISTON_CONTRACT, 1.0f, 0.0f);
        updateSidebar();
    }

    /** Port of chop:lobby/start/start2. */
    private void beginGame() {
        this.phase = GamePhase.RUNNING;
        this.tick = 0;
        this.itemTimer = 1;
        this.breach[0] = 0;
        this.breach[1] = 0;
        this.lastSelfBreakTick[0] = -1;
        this.lastSelfBreakTick[1] = -1;

        broadcastSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        broadcastTitle(this.plugin.messages().formatRaw("game.started-title", "<green>Game Started</green>"),
                this.plugin.messages().formatRaw("game.started-subtitle", "<gray>Blow 'em up!</gray>"), 0, 60, 20);

        // clean leftovers before the battle
        for (org.bukkit.entity.Entity entity : this.world.getEntities()) {
            if (entity instanceof org.bukkit.entity.TNTPrimed
                    || entity instanceof org.bukkit.entity.minecart.ExplosiveMinecart
                    || entity instanceof org.bukkit.entity.Fireball
                    || entity instanceof org.bukkit.entity.minecart.RideableMinecart) {
                entity.remove();
            }
        }

        int activePlayers = 0;
        for (Player player : players()) {
            PlayerSession session = this.sessions.get(player.getUniqueId());
            if (session == null) {
                continue;
            }
            if (session.team == null) {
                player.setGameMode(GameMode.SPECTATOR);
                player.teleport(new Location(this.world, Team.RED.spawnX() + 0.5, Team.SPAWN_Y + 1,
                        Team.RED.spawnZ() + 0.5));
                continue;
            }
            activePlayers++;
            deployPlayer(player, session, true);
        }

        this.itemSpawnTime = this.settings.itemSpawnTimeTicks(Math.max(1, activePlayers));
        this.itemTimer = 1;

        this.world.setGameRule(org.bukkit.GameRule.FALL_DAMAGE, this.settings.fallDamage);
        this.scoreboard.showHealthDisplay();

        // first turn of "break" flags: own-wall mining right at start never counts
        this.tick = 1;
        distributeItems();
        updateSidebar();

        HmwApi.callGameStart(this);
    }

    /** Sends a player into battle at their team spawn with the full kit. */
    private void deployPlayer(Player player, PlayerSession session, boolean fresh) {
        Team team = session.team;
        player.setGameMode(GameMode.SURVIVAL);
        player.setFlying(false);   // leave the ghost wait state behind
        player.setAllowFlight(false);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                ? player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue() : 20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        if (fresh) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
        }
        player.getInventory().setItem(0, this.plugin.itemFactory().bow());
        if (this.settings.pickaxe) {
            player.getInventory().setItem(1, this.plugin.itemFactory().pickaxe());
        }
        if (this.settings.elytra) {
            player.getInventory().setChestplate(this.plugin.itemFactory().elytra());
            player.sendMessage(this.plugin.messages().format("game.elytra-warning",
                    "<gray><bold>Warning: Everyone has an Elytra, but it only lasts for 3 seconds!</bold></gray>"));
        }
        giveArmor(player, session);

        Location spawn = new Location(this.world, team.spawnX() + 0.5, Team.SPAWN_Y + 1, team.spawnZ() + 0.5,
                team.spawnYaw(), 0f);
        player.teleport(spawn);
        player.setRespawnLocation(spawn, true);
        session.alive = true;
        session.respawnTicksLeft = -1;
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 2.0f);
    }

    private void giveArmor(Player player, PlayerSession session) {
        if (session.team == null) {
            return;
        }
        var factory = this.plugin.itemFactory();
        ItemStack[] armor = player.getInventory().getArmorContents();
        Material[] pieces = {Material.LEATHER_BOOTS, Material.LEATHER_LEGGINGS,
                Material.LEATHER_CHESTPLATE, Material.LEATHER_HELMET};
        for (int i = 0; i < 4; i++) {
            if (armor[i] == null || armor[i].getType() == Material.AIR) {
                armor[i] = factory.armorPiece(pieces[i], session.team);
            }
        }
        player.getInventory().setArmorContents(armor);
    }

    // ------------------------------------------------------------------
    // per-tick driver
    // ------------------------------------------------------------------

    public void tick() {
        if (this.disposed) {
            return;
        }
        this.tick++;
        switch (this.phase) {
            case WAITING -> tickWaiting();
            case STARTING -> tickStarting();
            case RUNNING -> tickRunning();
            case ENDING -> tickEnding();
        }
    }

    private void tickWaiting() {
        // auto-start countdown once both teams are occupied
        if (this.autoStartTicks >= 0) {
            int red = teamSize(Team.RED);
            int green = teamSize(Team.GREEN);
            if (red == 0 || green == 0) {
                this.autoStartTicks = -1;
                this.autoStartAnnounced = false;
                return;
            }
            this.autoStartTicks--;
            int secondsLeft = (this.autoStartTicks + 19) / 20;
            if (secondsLeft > 0 && this.autoStartTicks % 20 == 0) {
                broadcast(this.plugin.messages().format("game.auto-start",
                        "<yellow>Game auto-starts in {seconds}s</yellow>",
                        "{seconds}", String.valueOf(secondsLeft)));
            }
            if (this.autoStartTicks <= 0) {
                String error = startCountdown();
                if (error != null) {
                    this.autoStartTicks = -1;
                }
            }
        } else if (this.plugin.hmwConfig().autoStartSeconds() > 0 && !this.autoStartAnnounced) {
            if (teamSize(Team.RED) > 0 && teamSize(Team.GREEN) > 0) {
                this.autoStartTicks = this.plugin.hmwConfig().autoStartSeconds() * 20;
                this.autoStartAnnounced = true;
            }
        }
    }

    private void tickStarting() {
        int total = this.plugin.hmwConfig().countdownSeconds() * 20;
        int elapsed = total - this.countdownTicks;
        // show 3 / 2 / 1 during the last three seconds (start_timer behaviour)
        if (elapsed >= 20 && elapsed <= 60 && elapsed % 20 == 0) {
            int shown = (total - elapsed) / 20;
            NamedTextColor color = switch (shown) {
                case 3 -> NamedTextColor.GREEN;
                case 2 -> NamedTextColor.YELLOW;
                default -> NamedTextColor.RED;
            };
            broadcastTitle(Component.empty(), Component.text("- " + shown + " -", color), 0, 20, 5);
            broadcastSound(Sound.BLOCK_PISTON_EXTEND, 1.0f, 0.7f);
        }
        this.countdownTicks--;
        if (this.countdownTicks <= 0) {
            beginGame();
        }
    }

    private void tickRunning() {
        // item distribution cycle
        this.itemTimer++;
        if (this.itemTimer >= this.itemSpawnTime) {
            distributeItems();
            this.itemTimer = 1;
        }

        int interval = this.plugin.hmwConfig().breachScanInterval();
        if (this.tick % interval == 0) {
            scanBreaches();
            scanPortals();
            if (this.phase != GamePhase.RUNNING) {
                return; // a scan may have ended the game
            }
        }

        if (this.tick % 20 == 0) {
            refillKits();
            applyOptions();
            cleanupEntities();
        }
        if (this.tick % 10 == 0) {
            checkVoid();
        }
        if (this.tick % this.plugin.hmwConfig().epsilonCeilingInterval() == 0) {
            epsilonCeiling();
        }
        if (this.tick % 40 == 0 && !this.soloMode) {
            if (teamSize(Team.RED) == 0 || teamSize(Team.GREEN) == 0) {
                end(null, this.plugin.messages().raw("game.reason-not-enough", "Game ended: not enough players"));
                return;
            }
        }

        // respawn timers
        for (PlayerSession session : this.sessions.values()) {
            if (session.respawnTicksLeft >= 0) {
                Player player = Bukkit.getPlayer(session.playerId);
                if (player == null || !player.isOnline()) {
                    continue;
                }
                int left = session.respawnTicksLeft;
                if (left > 0 && left % 20 == 0 && left <= this.settings.respawnDelayTicks()) {
                    player.sendActionBar(this.plugin.messages().formatRaw("game.respawn-in",
                            "<green>Respawning in {seconds}...</green>",
                            "{seconds}", String.valueOf(left / 20)));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                }
                if (left <= 0) {
                    respawn(session, player);
                } else {
                    session.respawnTicksLeft = left - 1;
                }
            }
        }

        updateSidebar();
    }

    private void tickEnding() {
        this.endTicks--;
        if (this.endTicks <= 0) {
            finishAndReturn();
        }
    }

    // ------------------------------------------------------------------
    // running-phase helpers
    // ------------------------------------------------------------------

    /** One roll for the whole match; every team member receives the item. */
    private void distributeItems() {
        MissileSet set = this.settings.missileSet;
        Object entry = set.random(this.random);
        for (Player player : players()) {
            PlayerSession session = this.sessions.get(player.getUniqueId());
            if (session == null || session.team == null || !session.alive
                    || player.getGameMode() != GameMode.SURVIVAL) {
                continue;
            }
            if (entry instanceof MissileType type) {
                if (hasMissileEgg(player, type) && !this.settings.missileStacking) {
                    player.sendActionBar(this.plugin.messages().formatRaw("items.already-missile",
                            "<red>You already have a <name> so you didn't get a new one.</red>",
                            "{name}", type.displayName()));
                } else {
                    give(player, this.plugin.itemFactory().missileEgg(type, session.team));
                }
            } else if (entry == MissileSet.Special.ARROW) {
                int owned = countMaterial(player, Material.ARROW);
                if (this.settings.missileStacking) {
                    give(player, this.plugin.itemFactory().arrowStack(3));
                } else if (owned < 3) {
                    give(player, this.plugin.itemFactory().arrowStack(3 - owned));
                } else {
                    player.sendActionBar(this.plugin.messages().formatRaw("items.already-arrows",
                            "<red>You already have 3 arrows so you didn't get more.</red>"));
                }
            } else {
                Material material = switch ((MissileSet.Special) entry) {
                    case FIREBALL -> Material.BLAZE_SPAWN_EGG;
                    case GRENADE -> Material.EGG;
                    default -> Material.SNOWBALL;
                };
                if (countMaterial(player, material) > 0 && !this.settings.missileStacking) {
                    player.sendActionBar(this.plugin.messages().formatRaw("items.already-special",
                            "<red>You already have that item so you didn't get another one.</red>"));
                } else if (entry == MissileSet.Special.FIREBALL) {
                    give(player, this.plugin.itemFactory().fireballItem());
                } else if (entry == MissileSet.Special.GRENADE) {
                    give(player, this.plugin.itemFactory().grenadeItem());
                } else {
                    give(player, this.plugin.itemFactory().shieldItem());
                }
            }
        }
    }

    private boolean hasMissileEgg(Player player, MissileType type) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && type.id().equals(this.plugin.itemFactory().missileId(item))) {
                return true;
            }
        }
        return false;
    }

    private int countMaterial(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void give(Player player, ItemStack stack) {
        var leftover = player.getInventory().addItem(stack);
        for (ItemStack rest : leftover.values()) {
            this.world.dropItemNaturally(player.getLocation(), rest);
        }
    }

    /** Missing kit pieces are re-issued every second (chop:gameloop auto-refill). */
    private void refillKits() {
        for (Player player : players()) {
            PlayerSession session = this.sessions.get(player.getUniqueId());
            if (session == null || session.team == null || !session.alive
                    || player.getGameMode() != GameMode.SURVIVAL) {
                continue;
            }
            if (!player.getInventory().contains(Material.BOW)) {
                player.getInventory().setItem(0, this.plugin.itemFactory().bow());
            }
            if (this.settings.pickaxe && !player.getInventory().contains(Material.IRON_PICKAXE)) {
                player.getInventory().setItem(1, this.plugin.itemFactory().pickaxe());
            }
            giveArmor(player, session);
        }
    }

    private void applyOptions() {
        for (Player player : players()) {
            PlayerSession session = this.sessions.get(player.getUniqueId());
            if (session == null || session.team == null) {
                continue;
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 200, 100, true, false, false));
            if (session.alive) {
                if (this.settings.jumpBoost > 0) {
                    int amplifier = switch (this.settings.jumpBoost) {
                        case 1 -> 1;
                        case 2 -> 3;
                        default -> 5;
                    };
                    player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, amplifier, true, false, false));
                }
                if (this.settings.speedBoost > 0) {
                    int amplifier = switch (this.settings.speedBoost) {
                        case 1 -> 0;
                        case 2 -> 1;
                        default -> 3;
                    };
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, amplifier, true, false, false));
                }
            }
        }
    }

    private void cleanupEntities() {
        for (org.bukkit.entity.Entity entity : this.world.getEntities()) {
            // structures may contain stray chicken payloads (chop:gameloop tp -100)
            if (entity instanceof org.bukkit.entity.Chicken) {
                entity.remove();
            }
        }
    }

    /** Kills survival players in the void / standing on the world-bottom barrier. */
    private void checkVoid() {
        for (Player player : players()) {
            PlayerSession session = this.sessions.get(player.getUniqueId());
            if (session == null) {
                continue;
            }
            double y = player.getLocation().getY();
            boolean ghost = player.getGameMode() == GameMode.SPECTATOR
                    || (session.team != null && !session.alive); // dead players hover in adventure mode
            if (ghost) {
                if (y <= 0) {
                    player.teleport(player.getLocation().add(0, 20, 0));
                }
                continue;
            }
            if (session.team == null || !session.alive) {
                continue;
            }
            if (y <= 0) {
                player.setHealth(0.0);
                continue;
            }
            Block feet = this.world.getBlockAt(player.getLocation());
            if (feet.getY() == 0 && feet.getType() == Material.BARRIER) {
                player.setHealth(0.0);
            } else if (feet.getType() == Material.NETHER_PORTAL) {
                // entering a portal is death, like the original gameloop kill
                player.setHealth(0.0);
            }
        }
    }

    /** Stops Epsilon-style climbing missiles: pistons at y=60 become slime. */
    private void epsilonCeiling() {
        // chop:gameloop: fill 84 60 22 -85 60 124 slime_block replace piston
        for (int x = -85; x <= 84; x++) {
            for (int z = 22; z <= 124; z++) {
                Block block = this.world.getBlockAt(x, 60, z);
                if (block.getType() == Material.PISTON || block.getType() == Material.STICKY_PISTON) {
                    block.setType(Material.SLIME_BLOCK, false);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // breach + loss detection
    // ------------------------------------------------------------------

    private void scanBreaches() {
        for (Team team : Team.values()) {
            int stage = this.breach[team.ordinal()];
            if (stage >= 3) {
                continue;
            }
            int probeX = team.breachProbes()[stage];
            int holes = 0;
            for (int y = Team.WALL_MIN_Y; y <= Team.WALL_MAX_Y; y++) {
                for (int z = Team.WALL_MIN_Z; z <= Team.WALL_MAX_Z; z++) {
                    Block block = this.world.getBlockAt(probeX, y, z);
                    if (block.getType() == Material.AIR) {
                        block.setType(Material.CAVE_AIR, false); // mark hole as counted
                        holes++;
                    }
                }
            }
            if (holes == 0) {
                continue;
            }
            // own-team glass/honey mining delays the breach count (gameloop break flags)
            long window = this.plugin.hmwConfig().breachScanInterval() + 2L;
            if (this.tick - this.lastSelfBreakTick[team.ordinal()] <= window) {
                continue;
            }
            this.breach[team.ordinal()] = stage + 1;
            for (Player member : teamPlayers(team)) {
                member.sendMessage(this.plugin.messages().format("game.breach",
                        "<yellow>Your </yellow><{wall_color}><bold>{wall}</bold></{wall_color}>"
                                + "<yellow> has been breached!</yellow>",
                        "{wall_color}", wallColor(stage + 1, team).toString(),
                        "{wall}", this.plugin.messages().raw("game.wall-" + (stage + 1), team.wallName(stage + 1))));
                member.playSound(member.getLocation(), Sound.ENTITY_GUARDIAN_DEATH, 1.0f, 2.0f);
            }
            for (Player member : teamPlayers(team.enemy())) {
                PlayerSession session = this.sessions.get(member.getUniqueId());
                if (session != null) {
                    session.wallsBreached++;
                }
            }
            this.plugin.rewardService().awardBreach(team.enemy());
            HmwApi.callWallBreach(this, team, stage + 1);
        }
    }

    private NamedTextColor wallColor(int stage, Team team) {
        if (stage == 3) {
            return NamedTextColor.WHITE;
        }
        return team == Team.RED ? (stage == 1 ? NamedTextColor.RED : NamedTextColor.LIGHT_PURPLE)
                : (stage == 1 ? NamedTextColor.DARK_GREEN : NamedTextColor.GREEN);
    }

    /** Any air hole in the giant portal plane means the team lost (loss_detection). */
    private void scanPortals() {
        for (Team team : Team.values()) {
            for (int y = Team.PORTAL_MIN_Y; y <= Team.PORTAL_MAX_Y; y++) {
                for (int z = Team.PORTAL_MIN_Z; z <= Team.PORTAL_MAX_Z; z++) {
                    if (this.world.getBlockAt(team.portalPlaneX(), y, z).getType() == Material.AIR) {
                        // destroy the rest of the portal plane for the end effect
                        Fills.fill(this.world, team.portalPlaneX(), Team.PORTAL_MIN_Y, Team.PORTAL_MIN_Z,
                                team.portalPlaneX(), Team.PORTAL_MAX_Y, Team.PORTAL_MAX_Z, Material.AIR);
                        end(team.enemy(), null);
                        return;
                    }
                }
            }
        }
    }

    public void noteSelfBreak(Team team) {
        if (team != null) {
            this.lastSelfBreakTick[team.ordinal()] = this.tick;
        }
    }

    // ------------------------------------------------------------------
    // death / respawn
    // ------------------------------------------------------------------

    /** Called by DamageListener after PlayerDeathEvent. */
    public void handleDeath(Player victim, PlayerSession session) {
        if (this.phase != GamePhase.RUNNING || session == null) {
            return;
        }
        session.alive = false;
        session.deaths++;

        // kill attribution (last damager within 10 seconds)
        if (session.hasRecentDamager(System.currentTimeMillis(), 10_000)) {
            PlayerSession killerSession = this.sessions.get(session.lastDamager);
            if (killerSession != null && killerSession.team != null && killerSession.team != session.team) {
                killerSession.kills++;
                Player killer = Bukkit.getPlayer(killerSession.playerId);
                if (killer != null) {
                    this.plugin.rewardService().awardKill(killer);
                }
                HmwApi.callKill(this, killerSession.playerId, victim.getUniqueId());
            }
        }

        int delay = this.settings.respawnDelayTicks();
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            Player player = Bukkit.getPlayer(session.playerId);
            if (player != null && player.isOnline() && this.phase == GamePhase.RUNNING) {
                try {
                    player.spigot().respawn(); // skip the "You died!" screen
                } catch (IllegalStateException ignored) {
                    // player already clicked respawn on their own
                }
                // "ghost" wait state: hovering invisibly at the team spawn instead of
                // full spectator, so players cannot noclip-scout while dead
                player.setGameMode(GameMode.ADVENTURE);
                player.setAllowFlight(true);
                player.setFlying(true);
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
                        PotionEffect.INFINITE_DURATION, 0, false, false, false));
            }
        });
        if (delay == 0) {
            Bukkit.getScheduler().runTaskLater(this.plugin,
                    () -> respawn(session, Bukkit.getPlayer(session.playerId)), 2L);
        } else {
            session.respawnTicksLeft = delay;
        }
    }

    private void respawn(PlayerSession session, Player player) {
        if (player == null || !player.isOnline() || this.phase != GamePhase.RUNNING) {
            session.respawnTicksLeft = -1;
            return;
        }
        session.respawnTicksLeft = -1;
        deployPlayer(player, session, false);
        player.sendActionBar(Component.empty());
    }

    // ------------------------------------------------------------------
    // ending
    // ------------------------------------------------------------------

    /** Ends the game; winner may be null for forced ends. */
    public void end(Team winner, String forceReason) {
        if (this.phase == GamePhase.ENDING) {
            return;
        }
        this.phase = GamePhase.ENDING;
        this.winner = winner;
        this.endTicks = this.plugin.hmwConfig().endSeconds() * 20;

        if (this.arenaTask != null) {
            this.arenaTask.cancel();
            this.arenaTask = null;
        }

        broadcastTitle(this.plugin.messages().formatRaw("game.end-title", "<gold>Game Ended</gold>"),
                winner == null ? Component.text(forceReason == null ? "" : forceReason, NamedTextColor.RED)
                        : winnerText(winner, false),
                0, 100, 40);
        broadcast(winner == null
                ? Component.text(forceReason == null ? "Game ended." : forceReason, NamedTextColor.GRAY)
                : winnerText(winner, true));
        broadcastSound(Sound.ENTITY_WITHER_DEATH, 1.0f, 1.5f);

        for (Player player : players()) {
            player.setGameMode(GameMode.SPECTATOR);
        }
        for (org.bukkit.entity.Entity entity : this.world.getEntities()) {
            if (entity instanceof org.bukkit.entity.TNTPrimed || entity instanceof org.bukkit.entity.Fireball
                    || entity instanceof org.bukkit.entity.minecart.ExplosiveMinecart
                    || entity instanceof org.bukkit.entity.Arrow) {
                entity.remove();
            }
        }

        // fireworks over the winner's base (chop:game/fireworks)
        if (winner != null) {
            int color = winner == Team.RED ? 13041664 : 182018;
            Team winTeam = winner;
            this.fireworksTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
                Location base = new Location(this.world,
                        winTeam.spawnX() + (this.random.nextInt(51) - 25),
                        52 + this.random.nextInt(10),
                        winTeam.spawnZ() + (this.random.nextInt(51) - 25));
                Firework firework = this.world.spawn(base, Firework.class);
                var meta = firework.getFireworkMeta();
                meta.addEffect(org.bukkit.FireworkEffect.builder()
                        .with(org.bukkit.FireworkEffect.Type.BALL)
                        .withColor(Color.fromRGB(color))
                        .withFade(Color.WHITE)
                        .build());
                meta.setPower(1);
                firework.setFireworkMeta(meta);
            }, 1L, 10L);
        }

        // rewards + stats
        payout();
        HmwApi.callGameEnd(this, winner);

        updateSidebar();
    }

    /** The translated "<team> wins!" line in the winning team's color (bold for chat). */
    private Component winnerText(Team winner, boolean bold) {
        String text = winner == Team.RED
                ? this.plugin.messages().raw("game.winner-red", "Red Team wins!")
                : this.plugin.messages().raw("game.winner-green", "Green Team wins!");
        return bold
                ? Component.text(text, winner.textColor(), net.kyori.adventure.text.format.TextDecoration.BOLD)
                : Component.text(text, winner.textColor());
    }

    private void payout() {
        var rewardService = this.plugin.rewardService();
        var stats = this.plugin.statsService();
        for (PlayerSession session : this.sessions.values()) {
            Player player = Bukkit.getPlayer(session.playerId);
            if (player == null || !player.isOnline()) {
                // offline players still receive stats; rewards are paid on next join via stats
                stats.recordGame(session.playerId, this.winner != null && session.team == this.winner,
                        session.kills, session.deaths, session.missilesPlaced,
                        session.shieldsDeployed, session.wallsBreached);
                continue;
            }
            boolean won = this.winner != null && session.team == this.winner;
            stats.recordGame(player.getUniqueId(), won, session.kills, session.deaths,
                    session.missilesPlaced, session.shieldsDeployed, session.wallsBreached);
            rewardService.awardEnd(player, won, session.team != null);
        }
    }

    /** Sends everyone back to the hub/proxy and disposes the game. */
    private void finishAndReturn() {
        if (this.fireworksTask != null) {
            this.fireworksTask.cancel();
            this.fireworksTask = null;
        }
        for (Player player : players()) {
            player.sendMessage(this.plugin.messages().format("game.returning",
                    "<gray>Returning you to the lobby...</gray>"));
            this.plugin.gameManager().sendToHub(player);
        }
        this.plugin.gameManager().disposeGame(this);
    }

    /** Immediate shutdown path used on plugin disable. */
    public void shutdownNow() {
        for (Player player : players()) {
            this.plugin.gameManager().sendToHub(player);
        }
    }

    /** Rejoin path: returns a disconnected player straight into the battle. */
    public void joinRunning(Player player, PlayerSession session) {
        this.sessions.put(player.getUniqueId(), session);
        this.scoreboard.assign(player, session.team);
        deployPlayer(player, session, true);
        updateSidebar();
    }

    public void removePlayer(Player player) {
        PlayerSession session = this.sessions.remove(player.getUniqueId());
        if (session != null) {
            this.scoreboard.unassign(player);
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            player.setFlying(false);
            player.setAllowFlight(false);
        }
        updateSidebar();
    }

    // ------------------------------------------------------------------
    // misc helpers
    // ------------------------------------------------------------------

    public void broadcast(Component message) {
        for (Player player : players()) {
            player.sendMessage(message);
        }
    }

    public void broadcastTitle(Component title, Component subtitle, int in, int stay, int out) {
        Title.Times times = Title.Times.times(java.time.Duration.ofMillis(in * 50L),
                java.time.Duration.ofMillis(stay * 50L),
                java.time.Duration.ofMillis(out * 50L));
        Title built = Title.title(title, subtitle, times);
        for (Player player : players()) {
            player.showTitle(built);
        }
    }

    public void broadcastSound(Sound sound, float volume, float pitch) {
        for (Player player : players()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    private void updateSidebar() {
        if (this.phase == GamePhase.WAITING) {
            this.scoreboard.sidebar(
                    "",
                    "Arena: " + this.settings.arena.displayName(),
                    "Set: " + this.settings.missileSet.displayName(),
                    "Players: " + playerCount() + "/" + this.plugin.hmwConfig().maxPlayers(),
                    "Red: " + teamSize(Team.RED) + "  Green: " + teamSize(Team.GREEN),
                    "",
                    "Stand on a pad to join!",
                    "Press the button to start");
        } else if (this.phase == GamePhase.STARTING) {
            this.scoreboard.sidebar(
                    "",
                    "Starting...",
                    "Arena: " + this.settings.arena.displayName());
        } else if (this.phase == GamePhase.RUNNING || this.phase == GamePhase.ENDING) {
            this.scoreboard.sidebar(
                    "",
                    wallsLine(Team.RED),
                    wallsLine(Team.GREEN),
                    "",
                    "Red: " + teamSize(Team.RED) + "  Green: " + teamSize(Team.GREEN),
                    "Next item: " + Math.max(0, (this.itemSpawnTime - this.itemTimer) / 20) + "s",
                    "");
        }
    }

    private String wallsLine(Team team) {
        int stage = this.breach[team.ordinal()];
        StringBuilder bar = new StringBuilder(team == Team.RED ? "Red walls   " : "Green walls ");
        for (int i = 0; i < 3; i++) {
            bar.append(i < 3 - stage ? '\u2593' : '\u2591');
        }
        return bar.toString();
    }

    /** Called by the manager when the game world is unloaded. */
    public void markDisposed() {
        this.disposed = true;
    }
}
