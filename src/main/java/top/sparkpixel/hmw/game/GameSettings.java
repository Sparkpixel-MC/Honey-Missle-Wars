package top.sparkpixel.hmw.game;

import top.sparkpixel.hmw.HoneyMissileWarsPlugin;
import top.sparkpixel.hmw.arena.ArenaVariant;
import top.sparkpixel.hmw.item.MissileSet;

/**
 * Lobby-adjustable match settings, mirroring the 12 option signs of the map.
 */
public final class GameSettings {

    public ArenaVariant arena;
    public MissileSet missileSet;

    /** 3 = fast, 2 = normal, 1 = slow, 0 = ultra slow (item_rate). */
    public int itemRate = 2;
    /** 0 instant, 1 = 3s, 2 = 5s, 3 = 10s. */
    public int respawnTime = 2;
    /** 0 off, 1..3 = jump boost 2/4/6. */
    public int jumpBoost = 0;
    /** 0 off, 1..3 = speed 1/2/4. */
    public int speedBoost = 0;
    public boolean pickaxe;
    public boolean fallDamage;
    public boolean explodingArrows;
    public boolean elytra;
    /** missile_stacking: false = limited, true = unlimited items. */
    public boolean missileStacking;
    /** wall missiles (portalPlace). */
    public boolean wallMissiles;

    public GameSettings(ArenaVariant arena, MissileSet missileSet) {
        this.arena = arena;
        this.missileSet = missileSet;
    }

    /** Per-match settings seeded from config.yml (game.options section). */
    public static GameSettings fromConfig(HoneyMissileWarsPlugin plugin,
                                          ArenaVariant arena, MissileSet missileSet) {
        var config = plugin.hmwConfig();
        GameSettings settings = new GameSettings(arena, missileSet);
        settings.itemRate = config.optionItemRate();
        settings.respawnTime = config.optionRespawnTime();
        settings.jumpBoost = config.optionJumpBoost();
        settings.speedBoost = config.optionSpeedBoost();
        settings.pickaxe = config.optionPickaxe();
        settings.fallDamage = config.optionFallDamage();
        settings.explodingArrows = config.optionExplodingArrows();
        settings.elytra = config.optionElytra();
        settings.missileStacking = config.optionItemStacking();
        settings.wallMissiles = config.optionWallMissiles();
        return settings;
    }

    /** Respawn delay in ticks (start2). */
    public int respawnDelayTicks() {
        return switch (this.respawnTime) {
            case 1 -> 60;
            case 2 -> 100;
            case 3 -> 200;
            default -> 0;
        };
    }

    /** Item cycle length in ticks for the given player count (start2). */
    public int itemSpawnTimeTicks(int players) {
        int base;
        if (players <= 3) {
            base = 90;
        } else if (players <= 5) {
            base = 130;
        } else if (players <= 7) {
            base = 180;
        } else {
            base = 250;
        }
        return switch (this.itemRate) {
            case 3 -> Math.max(20, base - 60);
            case 1 -> base + 100;
            case 0 -> base + 200;
            default -> base;
        };
    }
}
