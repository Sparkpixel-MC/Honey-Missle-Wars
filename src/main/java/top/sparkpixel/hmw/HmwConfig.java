package top.sparkpixel.hmw;

import org.bukkit.configuration.file.FileConfiguration;
import top.sparkpixel.hmw.arena.ArenaVariant;
import top.sparkpixel.hmw.item.MissileSet;

/**
 * Typed access to config.yml.
 */
public final class HmwConfig {

    private final HoneyMissileWarsPlugin plugin;
    private FileConfiguration raw;

    // world
    private String slimeDir;
    private String baseTemplate;
    private String hubWorld;
    private String returnServer;
    private boolean autoJoinOnConnect;

    // game
    private int maxInstances;
    private int maxPlayers;
    private int minPlayersToStart;
    private int autoStartSeconds;
    private int countdownSeconds;
    private int endSeconds;
    private ArenaVariant defaultArena;
    private MissileSet defaultMissileSet;
    private int rejoinTimeoutSeconds;
    private int breachScanInterval;
    private int epsilonCeilingInterval;
    private boolean soloModeAllowed;
    private int tntFuseTicks;

    // per-match rule defaults (game.options) - mirrors the 12 lobby option signs
    private int optionItemRate;
    private int optionRespawnTime;
    private int optionJumpBoost;
    private int optionSpeedBoost;
    private boolean optionPickaxe;
    private boolean optionFallDamage;
    private boolean optionExplodingArrows;
    private boolean optionElytra;
    private boolean optionItemStacking;
    private boolean optionWallMissiles;

    // queue
    private boolean queueEnabled;
    private int queueCountdownSeconds;
    private String waitingRoomTemplate;
    private String waitZoneWorld;
    private boolean waitZoneUseSpawn;
    private double waitZoneX, waitZoneY, waitZoneZ;
    private float waitZoneYaw, waitZonePitch;

    // language
    private String language;

    // rewards
    private boolean rewardsEnabled;
    private RewardCurrency currency;
    private double rewardKill;
    private double rewardMissile;
    private double rewardShield;
    private double rewardBreach;
    private double rewardWin;
    private double rewardParticipation;
    private double xpPerCoin;

    // stats
    private int statsSaveIntervalMinutes;

    public enum RewardCurrency { VAULT, XP, BOTH }

    public HmwConfig(HoneyMissileWarsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.raw = plugin.getConfig();
        this.slimeDir = this.raw.getString("world.slime-dir", "slime_worlds");
        this.baseTemplate = this.raw.getString("world.base-template", "hmw_base");
        this.hubWorld = this.raw.getString("world.hub-world", "world");
        this.returnServer = this.raw.getString("world.return-server", "");
        this.autoJoinOnConnect = this.raw.getBoolean("world.auto-join-on-connect", false);

        this.maxInstances = clamp(this.raw.getInt("game.max-instances", 8), 1, 64);
        this.maxPlayers = clamp(this.raw.getInt("game.max-players", 16), 2, 100);
        this.minPlayersToStart = clamp(this.raw.getInt("game.min-players-to-start", 2), 1, this.maxPlayers);
        this.autoStartSeconds = Math.max(0, this.raw.getInt("game.auto-start-seconds", 20));
        this.countdownSeconds = clamp(this.raw.getInt("game.countdown-seconds", 4), 3, 30);
        this.endSeconds = clamp(this.raw.getInt("game.end-seconds", 10), 3, 120);
        ArenaVariant arena;
        try {
            arena = ArenaVariant.valueOf(this.raw.getString("game.default-arena", "CLASSIC").toUpperCase());
        } catch (IllegalArgumentException ex) {
            arena = ArenaVariant.CLASSIC;
        }
        this.defaultArena = arena;
        MissileSet set;
        try {
            set = MissileSet.valueOf(this.raw.getString("game.default-missile-set", "CLASSIC").toUpperCase());
        } catch (IllegalArgumentException ex) {
            set = MissileSet.CLASSIC;
        }
        this.defaultMissileSet = set;
        this.rejoinTimeoutSeconds = Math.max(0, this.raw.getInt("game.rejoin-timeout-seconds", 300));
        this.breachScanInterval = clamp(this.raw.getInt("game.breach-scan-interval-ticks", 5), 1, 100);
        this.epsilonCeilingInterval = clamp(this.raw.getInt("game.epsilon-ceiling-interval-ticks", 20), 1, 200);
        this.soloModeAllowed = this.raw.getBoolean("game.solo-mode-allowed", true);
        this.tntFuseTicks = clamp(this.raw.getInt("game.tnt-fuse-ticks", 20), 1, 80);

        this.optionItemRate = clamp(this.raw.getInt("game.options.item-rate", 2), 0, 3);
        this.optionRespawnTime = clamp(this.raw.getInt("game.options.respawn-time", 2), 0, 3);
        this.optionJumpBoost = clamp(this.raw.getInt("game.options.jump-boost", 0), 0, 3);
        this.optionSpeedBoost = clamp(this.raw.getInt("game.options.speed-boost", 0), 0, 3);
        this.optionPickaxe = this.raw.getBoolean("game.options.pickaxe", false);
        this.optionFallDamage = this.raw.getBoolean("game.options.fall-damage", false);
        this.optionExplodingArrows = this.raw.getBoolean("game.options.exploding-arrows", false);
        this.optionElytra = this.raw.getBoolean("game.options.elytra", false);
        this.optionItemStacking = this.raw.getBoolean("game.options.item-stacking", false);
        this.optionWallMissiles = this.raw.getBoolean("game.options.wall-missiles", false);

        this.queueEnabled = this.raw.getBoolean("queue.enabled", true);
        this.queueCountdownSeconds = clamp(this.raw.getInt("queue.countdown-seconds", 10), 0, 300);
        this.waitingRoomTemplate = this.raw.getString("queue.waiting-room.template", "hmw_waiting_room");
        this.waitZoneWorld = this.raw.getString("queue.wait-zone.world", "world");
        this.waitZoneUseSpawn = this.raw.getBoolean("queue.wait-zone.use-spawn", true);
        this.waitZoneX = this.raw.getDouble("queue.wait-zone.x", 0);
        this.waitZoneY = this.raw.getDouble("queue.wait-zone.y", 100);
        this.waitZoneZ = this.raw.getDouble("queue.wait-zone.z", 0);
        this.waitZoneYaw = (float) this.raw.getDouble("queue.wait-zone.yaw", 0);
        this.waitZonePitch = (float) this.raw.getDouble("queue.wait-zone.pitch", 0);

        this.rewardsEnabled = this.raw.getBoolean("rewards.enabled", true);
        String cur = this.raw.getString("rewards.currency", "BOTH").toUpperCase();
        try {
            this.currency = RewardCurrency.valueOf(cur);
        } catch (IllegalArgumentException ex) {
            this.currency = RewardCurrency.BOTH;
        }
        this.rewardKill = this.raw.getDouble("rewards.kill", 10.0);
        this.rewardMissile = this.raw.getDouble("rewards.missile", 5.0);
        this.rewardShield = this.raw.getDouble("rewards.shield", 2.0);
        this.rewardBreach = this.raw.getDouble("rewards.breach", 15.0);
        this.rewardWin = this.raw.getDouble("rewards.win", 100.0);
        this.rewardParticipation = this.raw.getDouble("rewards.participation", 20.0);
        this.xpPerCoin = this.raw.getDouble("rewards.xp-per-coin", 0.2);

        this.statsSaveIntervalMinutes = clamp(this.raw.getInt("stats.save-interval-minutes", 5), 1, 120);

        this.language = this.raw.getString("language", "en");
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Raw config access (used by the message overrides). */
    public FileConfiguration raw() {
        return this.raw;
    }

    public String slimeDir() {
        return this.slimeDir;
    }

    public String baseTemplate() {
        return this.baseTemplate;
    }

    public String hubWorld() {
        return this.hubWorld;
    }

    public String returnServer() {
        return this.returnServer;
    }

    /** BungeeCord dedicated-server mode: everyone connecting to this server joins the queue. */
    public boolean autoJoinOnConnect() {
        return this.autoJoinOnConnect;
    }

    public int maxInstances() {
        return this.maxInstances;
    }

    public int maxPlayers() {
        return this.maxPlayers;
    }

    public int minPlayersToStart() {
        return this.minPlayersToStart;
    }

    public int autoStartSeconds() {
        return this.autoStartSeconds;
    }

    public int countdownSeconds() {
        return this.countdownSeconds;
    }

    public int endSeconds() {
        return this.endSeconds;
    }

    public ArenaVariant defaultArena() {
        return this.defaultArena;
    }

    public MissileSet defaultMissileSet() {
        return this.defaultMissileSet;
    }

    public int rejoinTimeoutSeconds() {
        return this.rejoinTimeoutSeconds;
    }

    public int breachScanInterval() {
        return this.breachScanInterval;
    }

    public int epsilonCeilingInterval() {
        return this.epsilonCeilingInterval;
    }

    public boolean soloModeAllowed() {
        return this.soloModeAllowed;
    }

    /** Upper bound for TNT fuses primed inside game worlds (20 ticks = 1 second). */
    public int tntFuseTicks() {
        return this.tntFuseTicks;
    }

    // game.options defaults
    public int optionItemRate() {
        return this.optionItemRate;
    }

    public int optionRespawnTime() {
        return this.optionRespawnTime;
    }

    public int optionJumpBoost() {
        return this.optionJumpBoost;
    }

    public int optionSpeedBoost() {
        return this.optionSpeedBoost;
    }

    public boolean optionPickaxe() {
        return this.optionPickaxe;
    }

    public boolean optionFallDamage() {
        return this.optionFallDamage;
    }

    public boolean optionExplodingArrows() {
        return this.optionExplodingArrows;
    }

    public boolean optionElytra() {
        return this.optionElytra;
    }

    public boolean optionItemStacking() {
        return this.optionItemStacking;
    }

    public boolean optionWallMissiles() {
        return this.optionWallMissiles;
    }

    // queue
    public boolean queueEnabled() {
        return this.queueEnabled;
    }

    public int queueCountdownSeconds() {
        return this.queueCountdownSeconds;
    }

    /** Slime template of the waiting room world (the queue waiting area). */
    public String waitingRoomTemplate() {
        return this.waitingRoomTemplate;
    }

    /** Message language ("en" = built-in defaults, "zh" = bundled messages_zh.yml). */
    public String language() {
        return this.language;
    }

    public String waitZoneWorld() {
        return this.waitZoneWorld;
    }

    public boolean waitZoneUseSpawn() {
        return this.waitZoneUseSpawn;
    }

    public double waitZoneX() {
        return this.waitZoneX;
    }

    public double waitZoneY() {
        return this.waitZoneY;
    }

    public double waitZoneZ() {
        return this.waitZoneZ;
    }

    public float waitZoneYaw() {
        return this.waitZoneYaw;
    }

    public float waitZonePitch() {
        return this.waitZonePitch;
    }

    // rewards
    public boolean rewardsEnabled() {
        return this.rewardsEnabled;
    }

    public RewardCurrency currency() {
        return this.currency;
    }

    public double rewardKill() {
        return this.rewardKill;
    }

    public double rewardMissile() {
        return this.rewardMissile;
    }

    public double rewardShield() {
        return this.rewardShield;
    }

    public double rewardBreach() {
        return this.rewardBreach;
    }

    public double rewardWin() {
        return this.rewardWin;
    }

    public double rewardParticipation() {
        return this.rewardParticipation;
    }

    public double xpPerCoin() {
        return this.xpPerCoin;
    }

    public int statsSaveIntervalMinutes() {
        return this.statsSaveIntervalMinutes;
    }
}
