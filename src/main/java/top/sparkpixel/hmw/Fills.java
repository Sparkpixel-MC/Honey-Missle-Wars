package top.sparkpixel.hmw;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

/**
 * Region block helpers used by the arena generators. All writes use
 * {@code Block#setBlockData(data, false)} (no physics) so that generation does
 * not trigger chain updates, mirroring how the original datapack fills behave.
 */
public final class Fills {

    private Fills() {
    }

    /** set every block in the box to the material (order-normalised, like /fill). */
    public static void fill(World world, int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        fillData(world, x1, y1, z1, x2, y2, z2, material.createBlockData());
    }

    public static void fillData(World world, int x1, int y1, int z1, int x2, int y2, int z2, BlockData data) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != data.getMaterial()) {
                        block.setBlockData(data, false);
                    }
                }
            }
        }
    }

    /** set blocks only where they are currently air (datapack "fill ... keep"). */
    public static void fillKeep(World world, int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        BlockData data = material.createBlockData();
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.isEmpty()) {
                        block.setBlockData(data, false);
                    }
                }
            }
        }
    }

    /** fast region clear - only touches non-air blocks (used for the arena reset sweep). */
    public static void clearRegion(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.isEmpty()) {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
    }
}
