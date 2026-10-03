package top.sparkpixel.hmw.game;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;

import java.util.Set;

/**
 * The two teams with all their geometry constants (arena spans x -85..94 with
 * red on the west, green on the east; all values transcribed from the datapack).
 */
public enum Team {

    RED(
            NamedTextColor.RED, ChatColor.RED, Color.fromRGB(16711680), "red",
            -63, 87, -90f,
            -72,
            new int[]{-55, -60, -65},
            "Red Wall", "Pink Wall", "White Wall",
            Set.of(Material.PINK_STAINED_GLASS, Material.WHITE_STAINED_GLASS, Material.HONEY_BLOCK),
            Material.RED_STAINED_GLASS, Material.RED_TERRACOTTA),
    GREEN(
            NamedTextColor.GREEN, ChatColor.GREEN, Color.fromRGB(5227010), "green",
            62, 87, 90f,
            71,
            new int[]{54, 59, 64},
            "Green Wall", "Lime Wall", "White Wall",
            Set.of(Material.LIME_STAINED_GLASS, Material.WHITE_STAINED_GLASS, Material.HONEY_BLOCK),
            Material.GREEN_STAINED_GLASS, Material.GREEN_TERRACOTTA);

    /** Wall layers are scanned at y 10..49, z 59..115 (chop:game/breach_detection_*). */
    public static final int WALL_MIN_Y = 10, WALL_MAX_Y = 49;
    public static final int WALL_MIN_Z = 59, WALL_MAX_Z = 115;

    /** Portal (nether portal) plane interior: y 12..47, z 59..115 (loss_detection). */
    public static final int PORTAL_MIN_Y = 12, PORTAL_MAX_Y = 47;
    public static final int PORTAL_MIN_Z = 59, PORTAL_MAX_Z = 115;

    public static final int SPAWN_Y = 50;

    private final NamedTextColor textColor;
    private final ChatColor chatColor;
    private final Color leatherColor;
    private final String miniTag;
    private final int spawnX;
    private final int spawnZ;
    private final float spawnYaw;
    private final int portalPlaneX;
    private final int[] breachProbes;
    private final String wallName1;
    private final String wallName2;
    private final String wallName3;
    private final Set<Material> selfBreakExemptions;
    private final Material glass;
    private final Material terracotta;

    Team(NamedTextColor textColor, ChatColor chatColor, Color leatherColor, String miniTag,
         int spawnX, int spawnZ, float spawnYaw, int portalPlaneX, int[] breachProbes,
         String wallName1, String wallName2, String wallName3,
         Set<Material> selfBreakExemptions, Material glass, Material terracotta) {
        this.textColor = textColor;
        this.chatColor = chatColor;
        this.leatherColor = leatherColor;
        this.miniTag = miniTag;
        this.spawnX = spawnX;
        this.spawnZ = spawnZ;
        this.spawnYaw = spawnYaw;
        this.portalPlaneX = portalPlaneX;
        this.breachProbes = breachProbes;
        this.wallName1 = wallName1;
        this.wallName2 = wallName2;
        this.wallName3 = wallName3;
        this.selfBreakExemptions = selfBreakExemptions;
        this.glass = glass;
        this.terracotta = terracotta;
    }

    public NamedTextColor textColor() {
        return this.textColor;
    }

    public ChatColor chatColor() {
        return this.chatColor;
    }

    public Color leatherColor() {
        return this.leatherColor;
    }

    public String miniTag() {
        return this.miniTag;
    }

    public int spawnX() {
        return this.spawnX;
    }

    public int spawnZ() {
        return this.spawnZ;
    }

    public float spawnYaw() {
        return this.spawnYaw;
    }

    /** x of the giant nether-portal plane this team defends. */
    public int portalPlaneX() {
        return this.portalPlaneX;
    }

    /** Breach scan planes, innermost wall first. */
    public int[] breachProbes() {
        return this.breachProbes;
    }

    public String wallName(int breachStage) {
        return switch (breachStage) {
            case 1 -> this.wallName1;
            case 2 -> this.wallName2;
            default -> this.wallName3;
        };
    }

    /** Own-team glass/honey breaks that delay a breach count (see gameloop port notes). */
    public Set<Material> selfBreakExemptions() {
        return this.selfBreakExemptions;
    }

    public Material glass() {
        return this.glass;
    }

    public Material terracotta() {
        return this.terracotta;
    }

    public Team enemy() {
        return this == RED ? GREEN : RED;
    }
}
