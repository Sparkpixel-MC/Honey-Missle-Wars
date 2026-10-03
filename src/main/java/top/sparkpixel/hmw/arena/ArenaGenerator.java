package top.sparkpixel.hmw.arena;

import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.scheduler.BukkitTask;
import top.sparkpixel.hmw.Fills;
import top.sparkpixel.hmw.HoneyMissileWarsPlugin;

/**
 * Java port of the datapack arena generators (chop:lobby/arena/*).
 *
 * <p>The original walks a marker from x=-65 eastwards, one column per tick
 * (step N is at x = -65 + N - 1). This port keeps the exact step semantics so
 * every variant is reproduced block-for-block, but can run several steps per
 * tick (used during the game-start countdown, where the original also rebuilds
 * the arena while players are still in the lobby).
 */
public final class ArenaGenerator {

    /** Marker origin (chop:lobby/load_arena: positioned -65 54 87). */
    public static final int ORIGIN_X = -65;
    public static final int CENTER_Z = 87;
    public static final int TOTAL_STEPS = 140;

    /** Region wiped by chop:lobby/arena/reset (marker -85..+94, y 0..99, z 37..147). */
    private static final int RESET_MIN_X = -85, RESET_MAX_X = 94;
    private static final int RESET_MIN_Y = 0, RESET_MAX_Y = 99;
    private static final int RESET_MIN_Z = 37, RESET_MAX_Z = 147;

    private ArenaGenerator() {
    }

    /** Clears the whole arena volume (players are in the lobby at this point). */
    public static void resetRegion(World world) {
        Fills.clearRegion(world, RESET_MIN_X, RESET_MIN_Y, RESET_MIN_Z, RESET_MAX_X, RESET_MAX_Y, RESET_MAX_Z);
    }

    /** Barrier wall placed by chop:lobby/start/start2 (fill -86 0 125 95 128 125 barrier). */
    public static void placeBoundaryWall(World world) {
        Fills.fill(world, -86, 0, 125, 95, 128, 125, Material.BARRIER);
    }

    /** Full synchronous generation (used by admin tooling). */
    public static void generateSync(World world, ArenaVariant variant) {
        resetRegion(world);
        for (int step = 1; step <= TOTAL_STEPS; step++) {
            buildStep(world, variant, step);
        }
        placeBoundaryWall(world);
    }

    /**
     * Stepped generation matching the original tick pacing. Runs {@code stepsPerTick}
     * generator steps per server tick; {@code onDone} fires on the main thread.
     */
    public static BukkitTask generate(World world, ArenaVariant variant, int stepsPerTick, Runnable onDone) {
        resetRegion(world);
        final int[] next = {1};
        final BukkitTask[] handle = new BukkitTask[1];
        handle[0] = Bukkit.getScheduler().runTaskTimer(HoneyMissileWarsPlugin.get(), new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < stepsPerTick && next[0] <= TOTAL_STEPS; i++) {
                    buildStep(world, variant, next[0]);
                    next[0]++;
                }
                if (next[0] > TOTAL_STEPS) {
                    handle[0].cancel();
                    placeBoundaryWall(world);
                    onDone.run();
                }
            }
        }, 1L, 1L);
        return handle[0];
    }

    /** Executes one marker step (block writes only; spawn points are plugin constants). */
    public static void buildStep(World world, ArenaVariant variant, int step) {
        int x = ORIGIN_X + step - 1;

        switch (variant) {
            case CLASSIC -> classic(world, x, step);
            case CLIFF -> cliff(world, x, step);
            case SLOPE -> slope(world, x, step);
            case HILL -> hill(world, x, step);
            case VALLEY -> valley(world, x, step);
            case BRIDGE -> bridge(world, x, step);
            case ISLAND -> island(world, x, step);
            case WALL -> wall(world, x, step);
            case HARDCORE -> hardcore(world, x, step);
            case HONEY -> honey(world, x, step);
            case THICK -> thick(world, x, step);
            case TOWERS -> towers(world, x, step);
        }

        // obsidian platforms under both spawn points (all variants)
        if (step >= 2 && step <= 4) {
            col(world, x, 1, 49, -1, 1, Material.OBSIDIAN);
        } else if (step >= 127 && step <= 129) {
            col(world, x, 1, 49, -1, 1, Material.OBSIDIAN);
        }

        // portals (all variants)
        if (step == 1) {
            buildPortal(world, x - 7, x - 8); // red: portal plane -72, barrier -73
        } else if (step == 137) {
            buildPortal(world, x, x + 1); // green: portal plane +71, barrier +72
        }
    }

    private static void classic(World w, int x, int s) {
        if (s <= 5) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
        else if (s <= 10) col(w, x, 10, 49, -30, 30, Material.PINK_STAINED_GLASS);
        else if (s <= 15) col(w, x, 10, 49, -30, 30, Material.RED_STAINED_GLASS);
        else if (s <= 115) col(w, x, 0, 100, -40, 40, Material.AIR);
        else if (s <= 120) col(w, x, 10, 49, -30, 30, Material.GREEN_STAINED_GLASS);
        else if (s <= 125) col(w, x, 10, 49, -30, 30, Material.LIME_STAINED_GLASS);
        else if (s <= 130) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
    }

    private static void cliff(World w, int x, int s) {
        if (s <= 5) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
        else if (s == 6) col(w, x, 13, 49, -30, 30, Material.PINK_STAINED_GLASS);
        else if (s == 7) col(w, x, 16, 49, -30, 30, Material.PINK_STAINED_GLASS);
        else if (s == 8) col(w, x, 19, 49, -30, 30, Material.PINK_STAINED_GLASS);
        else if (s == 9) col(w, x, 22, 49, -30, 30, Material.PINK_STAINED_GLASS);
        else if (s == 10) col(w, x, 25, 49, -30, 30, Material.PINK_STAINED_GLASS);
        else if (s == 11) col(w, x, 28, 49, -30, 30, Material.RED_STAINED_GLASS);
        else if (s == 12) col(w, x, 31, 49, -30, 30, Material.RED_STAINED_GLASS);
        else if (s == 13) col(w, x, 34, 49, -30, 30, Material.RED_STAINED_GLASS);
        else if (s == 14) col(w, x, 37, 49, -30, 30, Material.RED_STAINED_GLASS);
        else if (s == 15) col(w, x, 40, 49, -30, 30, Material.RED_STAINED_GLASS);
        else if (s == 116) col(w, x, 40, 49, -30, 30, Material.GREEN_STAINED_GLASS);
        else if (s == 117) col(w, x, 37, 49, -30, 30, Material.GREEN_STAINED_GLASS);
        else if (s == 118) col(w, x, 34, 49, -30, 30, Material.GREEN_STAINED_GLASS);
        else if (s == 119) col(w, x, 31, 49, -30, 30, Material.GREEN_STAINED_GLASS);
        else if (s == 120) col(w, x, 28, 49, -30, 30, Material.GREEN_STAINED_GLASS);
        else if (s == 121) col(w, x, 25, 49, -30, 30, Material.LIME_STAINED_GLASS);
        else if (s == 122) col(w, x, 22, 49, -30, 30, Material.LIME_STAINED_GLASS);
        else if (s == 123) col(w, x, 19, 49, -30, 30, Material.LIME_STAINED_GLASS);
        else if (s == 124) col(w, x, 16, 49, -30, 30, Material.LIME_STAINED_GLASS);
        else if (s == 125) col(w, x, 13, 49, -30, 30, Material.LIME_STAINED_GLASS);
        else if (s <= 130) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
    }

    private static void slope(World w, int x, int s) {
        if (s <= 5) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
        else if (s == 6) col(w, x, 10, 47, -30, 30, Material.PINK_STAINED_GLASS);
        else if (s == 7) col(w, x, 10, 45, -30, 30, Material.PINK_STAINED_GLASS);
        else if (s == 8) col(w, x, 10, 43, -30, 30, Material.PINK_STAINED_GLASS);
        else if (s == 9) col(w, x, 10, 41, -30, 30, Material.PINK_STAINED_GLASS);
        else if (s == 10) col(w, x, 10, 39, -30, 30, Material.PINK_STAINED_GLASS);
        else if (s == 11) col(w, x, 10, 35, -30, 30, Material.RED_STAINED_GLASS);
        else if (s == 12) col(w, x, 10, 31, -30, 30, Material.RED_STAINED_GLASS);
        else if (s == 13) col(w, x, 10, 27, -30, 30, Material.RED_STAINED_GLASS);
        else if (s == 14) col(w, x, 10, 23, -30, 30, Material.RED_STAINED_GLASS);
        else if (s == 15) col(w, x, 10, 19, -30, 30, Material.RED_STAINED_GLASS);
        else if (s == 116) col(w, x, 10, 19, -30, 30, Material.GREEN_STAINED_GLASS);
        else if (s == 117) col(w, x, 10, 23, -30, 30, Material.GREEN_STAINED_GLASS);
        else if (s == 118) col(w, x, 10, 27, -30, 30, Material.GREEN_STAINED_GLASS);
        else if (s == 119) col(w, x, 10, 31, -30, 30, Material.GREEN_STAINED_GLASS);
        else if (s == 120) col(w, x, 10, 35, -30, 30, Material.GREEN_STAINED_GLASS);
        else if (s == 121) col(w, x, 10, 39, -30, 30, Material.LIME_STAINED_GLASS);
        else if (s == 122) col(w, x, 10, 41, -30, 30, Material.LIME_STAINED_GLASS);
        else if (s == 123) col(w, x, 10, 43, -30, 30, Material.LIME_STAINED_GLASS);
        else if (s == 124) col(w, x, 10, 45, -30, 30, Material.LIME_STAINED_GLASS);
        else if (s == 125) col(w, x, 10, 47, -30, 30, Material.LIME_STAINED_GLASS);
        else if (s <= 130) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
    }

    private static void hill(World w, int x, int s) {
        if (s <= 5) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
        else if (s == 6) col(w, x, 10, 49, -28, 28, Material.PINK_STAINED_GLASS);
        else if (s == 7) col(w, x, 10, 49, -26, 26, Material.PINK_STAINED_GLASS);
        else if (s == 8) col(w, x, 10, 49, -24, 24, Material.PINK_STAINED_GLASS);
        else if (s == 9) col(w, x, 10, 49, -22, 22, Material.PINK_STAINED_GLASS);
        else if (s == 10) col(w, x, 10, 49, -20, 20, Material.PINK_STAINED_GLASS);
        else if (s == 11) col(w, x, 10, 49, -17, 17, Material.RED_STAINED_GLASS);
        else if (s == 12) col(w, x, 10, 49, -14, 14, Material.RED_STAINED_GLASS);
        else if (s == 13) col(w, x, 10, 49, -11, 11, Material.RED_STAINED_GLASS);
        else if (s == 14) col(w, x, 10, 49, -8, 8, Material.RED_STAINED_GLASS);
        else if (s == 15) col(w, x, 10, 49, -5, 5, Material.RED_STAINED_GLASS);
        else if (s == 116) col(w, x, 10, 49, -5, 5, Material.GREEN_STAINED_GLASS);
        else if (s == 117) col(w, x, 10, 49, -8, 8, Material.GREEN_STAINED_GLASS);
        else if (s == 118) col(w, x, 10, 49, -11, 11, Material.GREEN_STAINED_GLASS);
        else if (s == 119) col(w, x, 10, 49, -14, 14, Material.GREEN_STAINED_GLASS);
        else if (s == 120) col(w, x, 10, 49, -17, 17, Material.GREEN_STAINED_GLASS);
        else if (s == 121) col(w, x, 10, 49, -20, 20, Material.LIME_STAINED_GLASS);
        else if (s == 122) col(w, x, 10, 49, -22, 22, Material.LIME_STAINED_GLASS);
        else if (s == 123) col(w, x, 10, 49, -24, 24, Material.LIME_STAINED_GLASS);
        else if (s == 124) col(w, x, 10, 49, -26, 26, Material.LIME_STAINED_GLASS);
        else if (s <= 130) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
    }

    private static void valley(World w, int x, int s) {
        if (s <= 5) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
        else if (s <= 10) col(w, x, 10, 49, -30, 30, Material.PINK_STAINED_GLASS);
        else if (s <= 15) col(w, x, 10, 49, -30, 30, Material.RED_STAINED_GLASS);
        else if (s <= 115) col(w, x, 0, 100, -40, 40, Material.AIR);
        else if (s <= 120) col(w, x, 10, 49, -30, 30, Material.GREEN_STAINED_GLASS);
        else if (s <= 125) col(w, x, 10, 49, -30, 30, Material.LIME_STAINED_GLASS);
        else if (s <= 130) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);

        // valley wedge carved into the walls towards the centre
        if (s >= 6 && s <= 15) {
            int half = switch (s) {
                case 6 -> 3;
                case 7 -> 5;
                case 8 -> 7;
                case 9 -> 9;
                case 10 -> 11;
                case 11 -> 14;
                case 12 -> 17;
                case 13 -> 20;
                case 14 -> 23;
                default -> 26;
            };
            col(w, x, 10, 49, -half, half, Material.AIR);
        } else if (s >= 116 && s <= 125) {
            int half = switch (s) {
                case 116 -> 26;
                case 117 -> 23;
                case 118 -> 20;
                case 119 -> 17;
                case 120 -> 14;
                case 121 -> 11;
                case 122 -> 9;
                case 123 -> 7;
                case 124 -> 5;
                default -> 3;
            };
            col(w, x, 10, 49, -half, half, Material.AIR);
        }

        // notch cleared in front of both portals
        if (s >= 131 && s <= 136) {
            col(w, x, 10, 49, -30, 30, Material.AIR);
        } else if (s >= 1 && s <= 6) {
            Fills.fill(w, x - 7, 10, CENTER_Z - 30, x - 7, 49, CENTER_Z + 30, Material.AIR);
        }
    }

    private static void bridge(World w, int x, int s) {
        if (s <= 5) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
        else if (s <= 10) col(w, x, 10, 49, -30, 30, Material.PINK_STAINED_GLASS);
        else if (s <= 15) col(w, x, 10, 49, -30, 30, Material.RED_STAINED_GLASS);
        else if (s <= 50) col(w, x, 43, 45, -3, 3, Material.RED_STAINED_GLASS);
        else if (s <= 115) col(w, x, 43, 45, -3, 3, Material.GREEN_STAINED_GLASS);
        else if (s <= 120) col(w, x, 10, 49, -30, 30, Material.GREEN_STAINED_GLASS);
        else if (s <= 125) col(w, x, 10, 49, -30, 30, Material.LIME_STAINED_GLASS);
        else if (s <= 130) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
    }

    private static void island(World w, int x, int s) {
        if (s <= 5) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
        else if (s <= 10) col(w, x, 10, 49, -30, 30, Material.PINK_STAINED_GLASS);
        else if (s <= 15) col(w, x, 10, 49, -30, 30, Material.RED_STAINED_GLASS);
        else if (s <= 65) col(w, x, 35, 40, -13, 13, Material.RED_STAINED_GLASS);
        else if (s <= 80) col(w, x, 35, 40, -13, 13, Material.GREEN_STAINED_GLASS);
        else if (s <= 120) col(w, x, 10, 49, -30, 30, Material.GREEN_STAINED_GLASS);
        else if (s <= 125) col(w, x, 10, 49, -30, 30, Material.LIME_STAINED_GLASS);
        else if (s <= 130) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
    }

    private static void wall(World w, int x, int s) {
        if (s <= 5) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
        else if (s <= 10) col(w, x, 10, 49, -30, 30, Material.PINK_STAINED_GLASS);
        else if (s <= 15) col(w, x, 10, 49, -30, 30, Material.RED_STAINED_GLASS);
        else if (s <= 65) col(w, x, 10, 60, -30, 30, Material.RED_STAINED_GLASS);
        else if (s <= 74) col(w, x, 10, 60, -30, 30, Material.GREEN_STAINED_GLASS);
        else if (s <= 120) col(w, x, 10, 49, -30, 30, Material.GREEN_STAINED_GLASS);
        else if (s <= 125) col(w, x, 10, 49, -30, 30, Material.LIME_STAINED_GLASS);
        else if (s <= 130) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
    }

    private static void hardcore(World w, int x, int s) {
        if (s <= 3) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
        else if (s <= 5) col(w, x, 45, 49, -30, 30, Material.WHITE_STAINED_GLASS);
        else if (s <= 10) col(w, x, 45, 49, -30, 30, Material.PINK_STAINED_GLASS);
        else if (s <= 15) col(w, x, 45, 49, -30, 30, Material.RED_STAINED_GLASS);
        else if (s <= 120) col(w, x, 45, 49, -30, 30, Material.GREEN_STAINED_GLASS);
        else if (s <= 125) col(w, x, 45, 49, -30, 30, Material.LIME_STAINED_GLASS);
        else if (s <= 127) col(w, x, 45, 49, -30, 30, Material.WHITE_STAINED_GLASS);
        else if (s <= 130) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
    }

    private static void thick(World w, int x, int s) {
        if (s <= 10) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
        else if (s <= 20) col(w, x, 10, 49, -30, 30, Material.PINK_STAINED_GLASS);
        else if (s <= 30) col(w, x, 10, 49, -30, 30, Material.RED_STAINED_GLASS);
        else if (s <= 100) col(w, x, 0, 100, -40, 40, Material.AIR);
        else if (s <= 110) col(w, x, 10, 49, -30, 30, Material.GREEN_STAINED_GLASS);
        else if (s <= 120) col(w, x, 10, 49, -30, 30, Material.LIME_STAINED_GLASS);
        else if (s <= 130) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
    }

    private static void towers(World w, int x, int s) {
        if (s <= 5) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
        else if (s <= 10) col(w, x, 10, 49, -30, 30, Material.PINK_STAINED_GLASS);
        else if (s <= 15) col(w, x, 10, 49, -30, 30, Material.RED_STAINED_GLASS);
        else if (s <= 115) col(w, x, 0, 100, -40, 40, Material.AIR);
        else if (s <= 120) col(w, x, 10, 49, -30, 30, Material.GREEN_STAINED_GLASS);
        else if (s <= 125) col(w, x, 10, 49, -30, 30, Material.LIME_STAINED_GLASS);
        else if (s <= 130) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);

        if (s >= 61 && s <= 65) {
            col(w, x, 10, 53, -30, -21, Material.RED_STAINED_GLASS);   // north tower (red)
            col(w, x, 10, 53, 21, 30, Material.RED_STAINED_GLASS);     // south tower (red)
        } else if (s >= 66 && s <= 70) {
            col(w, x, 10, 53, -30, -21, Material.GREEN_STAINED_GLASS); // north tower (green)
            col(w, x, 10, 53, 21, 30, Material.GREEN_STAINED_GLASS);   // south tower (green)
        }
        if (s >= 61 && s <= 70) {
            // tower windows (both towers, both sides)
            col(w, x, 41, 45, -28, -23, Material.AIR);
            col(w, x, 16, 20, -28, -23, Material.AIR);
            col(w, x, 41, 45, 23, 28, Material.AIR);
            col(w, x, 16, 20, 23, 28, Material.AIR);
        }
    }

    private static void honey(World w, int x, int s) {
        // red side
        if (s <= 5) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
        else if (s <= 10) col(w, x, 10, 49, -30, 30, Material.PINK_STAINED_GLASS);
        else if (s <= 15) col(w, x, 10, 49, -30, 30, Material.RED_STAINED_GLASS);

        if (s == 11) {
            col(w, x, 10, 49, -5, 5, Material.HONEY_BLOCK);           // honey gate band
            col(w, x, 30, 49, 21, 30, Material.HONEY_BLOCK);          // honey on the sides
            col(w, x, 30, 49, -30, -21, Material.HONEY_BLOCK);
        } else if (s <= 13) {
            col(w, x, 10, 49, -5, 5, Material.RED_STAINED_GLASS);
            col(w, x, 10, 49, -4, 4, Material.AIR);
            col(w, x, 47, 49, -5, 5, Material.AIR);                   // climbing gaps
            col(w, x, 38, 41, -5, 5, Material.AIR);
            col(w, x, 30, 33, -5, 5, Material.AIR);
            col(w, x, 22, 25, -5, 5, Material.AIR);
            col(w, x, 14, 17, -5, 5, Material.AIR);
            col(w, x, 30, 49, 21, 30, Material.AIR);
            col(w, x, 30, 49, -30, -21, Material.AIR);
        } else if (s <= 15) {
            col(w, x, 10, 49, -5, 5, Material.AIR);                   // full honey-gate slit
            col(w, x, 30, 49, 21, 30, Material.AIR);
            col(w, x, 30, 49, -30, -21, Material.AIR);
        }

        // green side
        if (s >= 116 && s <= 120) col(w, x, 10, 49, -30, 30, Material.GREEN_STAINED_GLASS);
        if (s == 120) {
            col(w, x, 10, 49, -5, 5, Material.HONEY_BLOCK);
            col(w, x, 30, 49, 21, 30, Material.HONEY_BLOCK);
            col(w, x, 30, 49, -30, -21, Material.HONEY_BLOCK);
        } else if (s >= 116 && s <= 119) {
            if (s <= 117) col(w, x, 10, 49, -5, 5, Material.AIR);
            col(w, x, 30, 49, 21, 30, Material.AIR);
            col(w, x, 30, 49, -30, -21, Material.AIR);
        }
        if (s == 118 || s == 119) {
            col(w, x, 10, 49, -5, 5, Material.LIME_STAINED_GLASS);
            col(w, x, 10, 49, -4, 4, Material.AIR);
            col(w, x, 47, 49, -5, 5, Material.AIR);
            col(w, x, 38, 41, -5, 5, Material.AIR);
            col(w, x, 30, 33, -5, 5, Material.AIR);
            col(w, x, 22, 25, -5, 5, Material.AIR);
            col(w, x, 14, 17, -5, 5, Material.AIR);
        }
        if (s >= 121 && s <= 125) col(w, x, 10, 49, -30, 30, Material.LIME_STAINED_GLASS);
        else if (s >= 126 && s <= 130) col(w, x, 10, 49, -30, 30, Material.WHITE_STAINED_GLASS);
    }

    /** Single-column helper; z offsets are relative to CENTER_Z (87). */
    private static void col(World w, int x, int y1, int y2, int zr1, int zr2, Material material) {
        Fills.fill(w, x, y1, CENTER_Z + zr1, x, y2, CENTER_Z + zr2, material);
    }

    /**
     * Giant nether-portal plane behind each base (the destructible "core").
     * Obsidian border, barrier back plane, and a plane of nether_portal[axis=z]
     * filling y 12..47 / z 59..115 - exactly as generated by every chop:lobby/arena/*.
     */
    private static void buildPortal(World world, int portalX, int barrierX) {
        Fills.fill(world, portalX, 11, 58, portalX, 48, 116, Material.OBSIDIAN);
        Fills.fill(world, barrierX, 11, 58, barrierX, 48, 116, Material.BARRIER);
        BlockData portalData = Material.NETHER_PORTAL.createBlockData();
        if (portalData instanceof Orientable orientable) {
            orientable.setAxis(Axis.Z);
        }
        Fills.fillData(world, portalX, 12, 59, portalX, 47, 115, portalData);
    }
}
