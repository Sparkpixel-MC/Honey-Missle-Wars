package top.sparkpixel.hmw.missile;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import top.sparkpixel.hmw.HoneyMissileWarsPlugin;
import top.sparkpixel.hmw.arena.ArenaVariant;
import top.sparkpixel.hmw.game.Game;
import top.sparkpixel.hmw.game.Team;
import top.sparkpixel.hmw.integration.HmwApi;

/**
 * Missile placement: port of kruthers missile:place_handling / place_control
 * and the team re-colouring fills of missile:place_control/place_missile.
 */
public final class MissileService {

    /** Hard placement bounds (missile:place_handling): x -68..67, y <= 55, z 54..120. */
    private static final int BOUND_MIN_X = -68, BOUND_MAX_X = 67;
    private static final int BOUND_MAX_Y = 55;
    private static final int BOUND_MIN_Z = 54, BOUND_MAX_Z = 120;

    /** Wall-restriction x ranges (red_portal_check / green_portal_check). */
    private static final int RED_WALL_MIN = -65, RED_WALL_MAX = 46;
    private static final int GREEN_WALL_MIN = -47, GREEN_WALL_MAX = 65;
    private static final int RED_WALL_MIN_THICK = -64, RED_WALL_MAX_THICK = 32;
    private static final int GREEN_WALL_MIN_THICK = -33, GREEN_WALL_MAX_THICK = 64;

    private final HoneyMissileWarsPlugin plugin;
    private final StructureService structures;

    public MissileService(HoneyMissileWarsPlugin plugin, StructureService structures) {
        this.plugin = plugin;
        this.structures = structures;
    }

    /**
     * Attempts to place a missile with its reference block at {@code base}
     * (the block the player clicked, offset by the clicked face).
     *
     * @return true when the missile was placed
     */
    public boolean place(Game game, org.bukkit.entity.Player player, Team team, MissileType type, Block base) {
        if (game == null || game.world() == null) {
            return false;
        }
        if (!withinBounds(base)) {
            fail(game, player, team);
            return false;
        }
        if (!game.settings().wallMissiles && !withinWallLimits(base.getX(), team, game.settings().arena)) {
            fail(game, player, team);
            return false;
        }

        World world = game.world();
        MissileType.Placement placement = type.placementFor(team);
        Location origin = new Location(world,
                base.getX() + placement.offsetX() + placement.posX(),
                base.getY() + placement.offsetY() + placement.posY(),
                base.getZ() + placement.offsetZ() + placement.posZ());

        boolean placed = this.structures.paste(world, type.structureFor(team), origin,
                placement.rotation(), true);
        if (!placed) {
            player.sendMessage(this.plugin.messages().format("missile.structure-missing",
                    "<red>Missile structure <name> could not be loaded.</red>",
                    "{name}", type.displayName()));
            return false;
        }

        recolor(world, base, team);

        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);
        game.session(player).missilesPlaced++;
        this.plugin.rewardService().awardMissile(player);
        HmwApi.callMissilePlace(player, game, type, base.getLocation());

        player.sendMessage(this.plugin.messages().format("missile.placed",
                "<gray>Missile deployed: <{team_color}><name></{team_color}></gray>",
                "{name}", type.displayName(),
                "{team_color}", team.miniTag()));
        return true;
    }

    private void fail(Game game, org.bukkit.entity.Player player, Team team) {
        // Port of the place fail actionbar; the item is never consumed because the
        // interact event is cancelled, so no refund is required.
        player.sendActionBar(this.plugin.messages().formatRaw("missile.cannot-place",
                "<red>You can't place a missile here! It may be too close to the enemy wall.</red>"));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_PISTON_CONTRACT, 1.0f, 0.8f);
    }

    private boolean withinBounds(Block base) {
        int x = base.getX();
        int y = base.getY();
        int z = base.getZ();
        return x >= BOUND_MIN_X && x <= BOUND_MAX_X
                && y <= BOUND_MAX_Y
                && z >= BOUND_MIN_Z && z <= BOUND_MAX_Z;
    }

    private boolean withinWallLimits(int x, Team team, ArenaVariant arena) {
        boolean thick = arena.isThick();
        if (team == Team.RED) {
            int min = thick ? RED_WALL_MIN_THICK : RED_WALL_MIN;
            int max = thick ? RED_WALL_MAX_THICK : RED_WALL_MAX;
            return x >= min && x <= max;
        }
        int min = thick ? GREEN_WALL_MIN_THICK : GREEN_WALL_MIN;
        int max = thick ? GREEN_WALL_MAX_THICK : GREEN_WALL_MAX;
        return x >= min && x <= max;
    }

    /**
     * Team re-colouring fill (place_missile tail): relative to the placement point,
     * x +-22, y -15..+3, z +-8.
     */
    private void recolor(World world, Block base, Team team) {
        int minX = base.getX() - 22, maxX = base.getX() + 22;
        int minY = base.getY() - 15, maxY = base.getY() + 3;
        int minZ = base.getZ() - 8, maxZ = base.getZ() + 8;

        Material teamGlass = team == Team.RED ? Material.RED_STAINED_GLASS : Material.GREEN_STAINED_GLASS;
        Material teamTerracotta = team == Team.RED ? Material.RED_TERRACOTTA : Material.GREEN_TERRACOTTA;
        BlockFace arrowFace = team == Team.RED ? BlockFace.EAST : BlockFace.WEST;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();
                    if (type == Material.BROWN_STAINED_GLASS) {
                        block.setType(teamGlass, false);
                    } else if (type == Material.SMOOTH_QUARTZ || type == Material.QUARTZ_BLOCK) {
                        block.setType(teamTerracotta, false);
                    } else if (type == Material.BLUE_ICE) {
                        var data = Material.MAGENTA_GLAZED_TERRACOTTA.createBlockData();
                        if (data instanceof Directional directional) {
                            directional.setFacing(arrowFace);
                        }
                        block.setBlockData(data, false);
                    }
                }
            }
        }
    }
}
