package top.sparkpixel.hmw.missile;

/**
 * Structure rotation, mirroring vanilla structure-block semantics
 * (net.minecraft.world.level.block.Rotation). Replaces the Bukkit structure
 * API enum so the plugin does not depend on org.bukkit.block.structure.
 */
public enum StructureRotation {
    NONE,
    CLOCKWISE_90,
    CLOCKWISE_180,
    COUNTERCLOCKWISE_90;

    /** Template (x, z) -> rotated offset, pivot at the origin corner (vanilla getRotatedBlockPos). */
    public int transformX(int x, int z) {
        return switch (this) {
            case CLOCKWISE_90 -> -z;
            case CLOCKWISE_180 -> -x;
            case COUNTERCLOCKWISE_90 -> z;
            default -> x;
        };
    }

    public int transformZ(int x, int z) {
        return switch (this) {
            case CLOCKWISE_90 -> x;
            case CLOCKWISE_180 -> -z;
            case COUNTERCLOCKWISE_90 -> -x;
            default -> z;
        };
    }

    /** double variants of the transforms above, for entity positions (relative double coords). */
    public double transformX(double x, double z) {
        return switch (this) {
            case CLOCKWISE_90 -> -z;
            case CLOCKWISE_180 -> -x;
            case COUNTERCLOCKWISE_90 -> z;
            default -> x;
        };
    }

    public double transformZ(double x, double z) {
        return switch (this) {
            case CLOCKWISE_90 -> x;
            case CLOCKWISE_180 -> -z;
            case COUNTERCLOCKWISE_90 -> -x;
            default -> z;
        };
    }

    /** Rotates a horizontal facing the way vanilla BlockState#rotate does. */
    public org.bukkit.block.BlockFace transformFacing(org.bukkit.block.BlockFace facing) {
        return switch (this) {
            case CLOCKWISE_90 -> switch (facing) {
                case NORTH -> org.bukkit.block.BlockFace.EAST;
                case EAST -> org.bukkit.block.BlockFace.SOUTH;
                case SOUTH -> org.bukkit.block.BlockFace.WEST;
                case WEST -> org.bukkit.block.BlockFace.NORTH;
                default -> facing;
            };
            case CLOCKWISE_180 -> facing.getOppositeFace();
            case COUNTERCLOCKWISE_90 -> switch (facing) {
                case NORTH -> org.bukkit.block.BlockFace.WEST;
                case WEST -> org.bukkit.block.BlockFace.SOUTH;
                case SOUTH -> org.bukkit.block.BlockFace.EAST;
                case EAST -> org.bukkit.block.BlockFace.NORTH;
                default -> facing;
            };
            default -> facing;
        };
    }

    /** 90-degree rotations swap the X and Z axes (vanilla Axis#rotate). */
    public org.bukkit.Axis transformAxis(org.bukkit.Axis axis) {
        if ((this == CLOCKWISE_90 || this == COUNTERCLOCKWISE_90)) {
            if (axis == org.bukkit.Axis.X) {
                return org.bukkit.Axis.Z;
            }
            if (axis == org.bukkit.Axis.Z) {
                return org.bukkit.Axis.X;
            }
        }
        return axis;
    }

    /** Shift for the 0..15 "rotation" property (signs etc.): clockwise from south. */
    public int transformRotation(int rotation) {
        return switch (this) {
            case CLOCKWISE_90 -> (rotation + 4) % 16;
            case CLOCKWISE_180 -> (rotation + 8) % 16;
            case COUNTERCLOCKWISE_90 -> (rotation + 12) % 16;
            default -> rotation;
        };
    }

    /** Vanilla "rotation" property order: index 0 = south, then clockwise (signs, banners). */
    private static final org.bukkit.block.BlockFace[] ROTATION_FACES = {
            org.bukkit.block.BlockFace.SOUTH,
            org.bukkit.block.BlockFace.SOUTH_SOUTH_WEST,
            org.bukkit.block.BlockFace.SOUTH_WEST,
            org.bukkit.block.BlockFace.WEST_SOUTH_WEST,
            org.bukkit.block.BlockFace.WEST,
            org.bukkit.block.BlockFace.WEST_NORTH_WEST,
            org.bukkit.block.BlockFace.NORTH_WEST,
            org.bukkit.block.BlockFace.NORTH_NORTH_WEST,
            org.bukkit.block.BlockFace.NORTH,
            org.bukkit.block.BlockFace.NORTH_NORTH_EAST,
            org.bukkit.block.BlockFace.NORTH_EAST,
            org.bukkit.block.BlockFace.EAST_NORTH_EAST,
            org.bukkit.block.BlockFace.EAST,
            org.bukkit.block.BlockFace.EAST_SOUTH_EAST,
            org.bukkit.block.BlockFace.SOUTH_EAST,
            org.bukkit.block.BlockFace.SOUTH_SOUTH_EAST
    };

    /** Rotated BlockFace for the 0..15 "rotation" property (signs etc.): index 0 = south, clockwise. */
    public org.bukkit.block.BlockFace transformRotationFace(int rotation) {
        return ROTATION_FACES[transformRotation(rotation)];
    }
}
