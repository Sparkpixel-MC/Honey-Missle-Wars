package top.sparkpixel.hmw.missile;

import org.bukkit.Material;
import top.sparkpixel.hmw.game.Team;

/**
 * All 33 placeable missiles of the original map, with the exact placement
 * parameters transcribed from the kruthers datapack (missile:place/*).
 *
 * <p>Every missile is a vanilla piston/slime/honey flying machine stored as a
 * structure NBT. The datapack pastes it with a LOAD structure block placed at
 * {@code base + structureBlockOffset}, loading the structure at
 * {@code structureBlock + structurePos} with the given rotation. This plugin
 * parses the .nbt itself ({@link StructureTemplate}) and pastes at
 * {@code base + structureBlockOffset + structurePos} with vanilla rotation
 * semantics - identical result.
 *
 * @param id                datapack tag id (also the PDC value on the egg item)
 * @param displayName       item display name
 * @param eggMaterial       spawn-egg material used for the item
 * @param redStructure      structure namespace:name for the red team
 * @param greenStructure    structure namespace:name for the green team
 * @param red               red team placement
 * @param green             green team placement
 */
public enum MissileType {

    ANNIHILATOR("annihilator", "Annihilator", Material.VINDICATOR_SPAWN_EGG,
            "community:red_annihilator", "community:green_annihilator",
            new Placement(4, -7, 0, StructureRotation.NONE, 0, -1, -1),
            new Placement(-3, -7, 0, StructureRotation.NONE, -5, -1, -1)),
    BLOCKER("blocker", "Blocker", Material.ZOMBIE_HORSE_SPAWN_EGG,
            "minecraft:blocker", "minecraft:blocker",
            new Placement(4, -7, 0, StructureRotation.NONE, 0, -1, -1),
            new Placement(-4, -7, 0, StructureRotation.CLOCKWISE_180, 0, -1, 1)),
    BOMBER("bomber", "Bomber", Material.STRIDER_SPAWN_EGG,
            "minecraft:bomber", "minecraft:bomber",
            new Placement(4, -6, 0, StructureRotation.NONE, 0, -1, 0),
            new Placement(-4, -6, 0, StructureRotation.CLOCKWISE_180, 0, -1, 0)),
    BRIDGE("bridge", "Bridge", Material.EVOKER_SPAWN_EGG,
            "minecraft:bridge", "minecraft:bridge",
            new Placement(4, -6, 1, StructureRotation.NONE, 0, 0, -1),
            new Placement(-4, -6, -1, StructureRotation.CLOCKWISE_180, 0, 0, 1)),
    BULLDOZER("bulldozer", "Bulldozer", Material.VEX_SPAWN_EGG,
            "community:bulldozer", "community:bulldozer",
            new Placement(12, -7, 0, StructureRotation.CLOCKWISE_180, 0, -1, 1),
            new Placement(-12, -7, 0, StructureRotation.NONE, 0, -1, -1)),
    CLEARER("clearer", "Clearer", Material.TROPICAL_FISH_SPAWN_EGG,
            "minecraft:clearer", "minecraft:clearer",
            new Placement(4, -9, 0, StructureRotation.NONE, 0, 0, 0),
            new Placement(-4, -9, 0, StructureRotation.CLOCKWISE_180, 0, 0, 0)),
    COUNTDOWN("countdown", "Countdown", Material.STRAY_SPAWN_EGG,
            "community:countdown", "community:countdown",
            new Placement(4, -10, 0, StructureRotation.NONE, 0, 2, 0),
            new Placement(-4, -10, 0, StructureRotation.CLOCKWISE_180, 0, 2, 0)),
    DOUBLE_DIP("double_dip", "Double Dip", Material.HUSK_SPAWN_EGG,
            "community:doubledip", "community:doubledip",
            new Placement(12, -7, 0, StructureRotation.CLOCKWISE_180, 0, -1, 1),
            new Placement(-12, -7, 0, StructureRotation.NONE, 0, -1, -1)),
    DROPPER("dropper", "Dropper", Material.PUFFERFISH_SPAWN_EGG,
            "minecraft:dropper", "minecraft:dropper",
            new Placement(4, -6, 0, StructureRotation.NONE, 0, -2, 0),
            new Placement(-4, -6, 0, StructureRotation.CLOCKWISE_180, 0, -2, 0)),
    EL_DERECHO("el_derecho", "El Derecho", Material.WITHER_SKELETON_SPAWN_EGG,
            "community:el_derecho", "community:el_derecho",
            new Placement(10, -11, 0, StructureRotation.CLOCKWISE_90, 0, 3, -1),
            new Placement(-10, -11, 0, StructureRotation.COUNTERCLOCKWISE_90, 0, 3, 1)),
    EPSILON("epsilon", "Epsilon", Material.WOLF_SPAWN_EGG,
            "community:epsilon", "community:epsilon",
            new Placement(12, -9, 0, StructureRotation.CLOCKWISE_180, 0, 1, 0),
            new Placement(-12, -9, 0, StructureRotation.NONE, 0, 1, 0)),
    FLAT_HEAD("flat_head", "Flat Head", Material.DROWNED_SPAWN_EGG,
            "minecraft:flat_head", "minecraft:flat_head",
            new Placement(4, -6, 0, StructureRotation.NONE, 0, 0, -2),
            new Placement(-4, -6, 0, StructureRotation.CLOCKWISE_180, 0, 0, 2)),
    GUARDIAN("guardian", "Guardian", Material.GUARDIAN_SPAWN_EGG,
            "minecraft:guardian", "minecraft:guardian",
            new Placement(10, -6, -1, StructureRotation.CLOCKWISE_90, 0, -1, 0),
            new Placement(-10, -6, 1, StructureRotation.COUNTERCLOCKWISE_90, 0, -1, 0)),
    HIT_AND_RUN("hit_and_run", "Hit and Run", Material.PANDA_SPAWN_EGG,
            "community:hit_and_run", "community:hit_and_run",
            new Placement(12, -10, 0, StructureRotation.CLOCKWISE_90, 0, 2, -1),
            new Placement(-12, -10, 0, StructureRotation.COUNTERCLOCKWISE_90, 0, 2, 1)),
    HONEY_GUARDIAN("honey_guardian", "Honey Guardian", Material.GUARDIAN_SPAWN_EGG,
            "minecraft:honey_guardian", "minecraft:honey_guardian",
            new Placement(10, -6, -1, StructureRotation.CLOCKWISE_90, 0, -1, 0),
            new Placement(-10, -6, 1, StructureRotation.COUNTERCLOCKWISE_90, 0, -1, 0)),
    HONEY_JUGGERNAUT("honey_juggernaut", "Honey Juggernaut", Material.POLAR_BEAR_SPAWN_EGG,
            "minecraft:honey_juggernaut", "minecraft:honey_juggernaut",
            new Placement(6, -6, 0, StructureRotation.CLOCKWISE_180, 8, -1, 1),
            new Placement(-6, -6, 0, StructureRotation.NONE, -8, -1, -1)),
    HONEY_LIGHTNING("honey_lightning", "Honey Lightning", Material.OCELOT_SPAWN_EGG,
            "minecraft:honey_lightning", "minecraft:honey_lightning",
            new Placement(4, -6, 0, StructureRotation.CLOCKWISE_180, 8, 0, 1),
            new Placement(-4, -6, 0, StructureRotation.NONE, -8, 0, -1)),
    HONEY_SHIELDBUSTER("honey_shieldbuster", "Honey Shieldbuster", Material.LLAMA_SPAWN_EGG,
            "minecraft:honey_shieldbuster", "minecraft:honey_shieldbuster",
            new Placement(6, -9, 0, StructureRotation.CLOCKWISE_180, 10, 2, 1),
            new Placement(-6, -9, 0, StructureRotation.NONE, -10, 2, -1)),
    HONEY_TOMAHAWK("honey_tomahawk", "Honey Tomahawk", Material.ZOMBIE_SPAWN_EGG,
            "minecraft:honey_tomahawk", "minecraft:honey_tomahawk",
            new Placement(6, -6, 0, StructureRotation.CLOCKWISE_180, 10, 0, 0),
            new Placement(-6, -6, 0, StructureRotation.NONE, -10, 0, 0)),
    JUGGERNAUT("juggernaut", "Juggernaut", Material.POLAR_BEAR_SPAWN_EGG,
            "minecraft:juggernaut", "minecraft:juggernaut",
            new Placement(6, -6, 0, StructureRotation.CLOCKWISE_180, 8, -1, 1),
            new Placement(-6, -6, 0, StructureRotation.NONE, -8, -1, -1)),
    KODACHI("kodachi", "Kodachi", Material.CAT_SPAWN_EGG,
            "community:kodachi", "community:kodachi",
            new Placement(18, -10, 0, StructureRotation.CLOCKWISE_180, 0, 2, 1),
            new Placement(-18, -10, 0, StructureRotation.NONE, 0, 2, -1)),
    LIGHTNING("lightning", "Lightning", Material.OCELOT_SPAWN_EGG,
            "minecraft:lightning", "minecraft:lightning",
            new Placement(4, -6, 0, StructureRotation.CLOCKWISE_180, 8, 0, 1),
            new Placement(-4, -6, 0, StructureRotation.NONE, -8, 0, -1)),
    MINIBUS("minibus", "Minibus", Material.SKELETON_HORSE_SPAWN_EGG,
            "minecraft:minibus", "minecraft:minibus",
            new Placement(4, -7, 0, StructureRotation.NONE, 0, -2, -2),
            new Placement(-4, -7, 0, StructureRotation.CLOCKWISE_180, 0, -2, 2)),
    SCORPION("scorpion", "Scorpion", Material.CAVE_SPIDER_SPAWN_EGG,
            "community:scorpion", "community:scorpion",
            new Placement(12, -10, 0, StructureRotation.CLOCKWISE_90, 0, 2, -1),
            new Placement(-12, -10, 0, StructureRotation.COUNTERCLOCKWISE_90, 0, 2, 1)),
    SHIELD_MISSILE("shield_missile", "Shield Missile", Material.WANDERING_TRADER_SPAWN_EGG,
            "minecraft:shield_missile", "minecraft:shield_missile",
            new Placement(4, -8, 0, StructureRotation.NONE, 0, -3, -3),
            new Placement(-4, -8, 0, StructureRotation.CLOCKWISE_180, 0, -3, 3)),
    SHIELDBUSTER("shieldbuster", "Shieldbuster", Material.LLAMA_SPAWN_EGG,
            "minecraft:shieldbuster", "minecraft:shieldbuster",
            new Placement(6, -9, 0, StructureRotation.CLOCKWISE_180, 10, 2, 1),
            new Placement(-6, -9, 0, StructureRotation.NONE, -10, 2, -1)),
    TANK("tank", "Tank", Material.PHANTOM_SPAWN_EGG,
            "minecraft:tank", "minecraft:tank",
            new Placement(4, -7, 0, StructureRotation.NONE, 0, -2, -1),
            new Placement(-4, -7, 0, StructureRotation.CLOCKWISE_180, 0, -2, 1)),
    THUNDER_STRIKE("thunder_strike", "Thunder Strike", Material.MAGMA_CUBE_SPAWN_EGG,
            "minecraft:thunder_strike", "minecraft:thunder_strike",
            new Placement(4, -6, 0, StructureRotation.NONE, 0, -1, -1),
            new Placement(-4, -6, 0, StructureRotation.CLOCKWISE_180, 0, -1, 1)),
    TOMAHAWK("tomahawk", "Tomahawk", Material.ZOMBIE_SPAWN_EGG,
            "minecraft:tomahawk", "minecraft:tomahawk",
            new Placement(6, -6, 0, StructureRotation.CLOCKWISE_180, 10, 0, 0),
            new Placement(-6, -6, 0, StructureRotation.NONE, -10, 0, 0)),
    TORPEDO("torpedo", "Torpedo", Material.SPIDER_SPAWN_EGG,
            "community:torpedo", "community:torpedo",
            new Placement(3, -7, 0, StructureRotation.NONE, 0, -1, -1),
            new Placement(-3, -7, 0, StructureRotation.CLOCKWISE_180, 0, -1, 1)),
    TRANSPORT("transport", "Transport", Material.HORSE_SPAWN_EGG,
            "minecraft:transport", "minecraft:transport",
            new Placement(4, -9, 0, StructureRotation.NONE, 0, 0, -1),
            new Placement(-4, -9, 0, StructureRotation.CLOCKWISE_180, 0, 0, 1)),
    TREBUCHET("trebuchet", "Trebuchet", Material.BAT_SPAWN_EGG,
            "community:trebuchet", "community:trebuchet",
            new Placement(3, -10, 0, StructureRotation.NONE, 0, 2, -1),
            new Placement(-3, -10, 0, StructureRotation.CLOCKWISE_180, 0, 2, 1)),
    UNDERTAKER("undertaker", "Undertaker", Material.PHANTOM_SPAWN_EGG,
            "community:undertaker", "community:undertaker",
            new Placement(6, -10, 0, StructureRotation.NONE, 0, 2, -1),
            new Placement(-6, -10, 0, StructureRotation.CLOCKWISE_180, 0, 2, 1));

    /** Placement parameters for one team (transcribed from missile:place/<type>). */
    public record Placement(int offsetX, int offsetY, int offsetZ, StructureRotation rotation,
                            int posX, int posY, int posZ) {
    }

    private final String id;
    private final String displayName;
    private final Material eggMaterial;
    private final String redStructure;
    private final String greenStructure;
    private final Placement red;
    private final Placement green;

    MissileType(String id, String displayName, Material eggMaterial,
                String redStructure, String greenStructure,
                Placement red, Placement green) {
        this.id = id;
        this.displayName = displayName;
        this.eggMaterial = eggMaterial;
        this.redStructure = redStructure;
        this.greenStructure = greenStructure;
        this.red = red;
        this.green = green;
    }

    public String id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    public Material eggMaterial() {
        return this.eggMaterial;
    }

    public String structureFor(Team team) {
        return team == Team.RED ? this.redStructure : this.greenStructure;
    }

    public Placement placementFor(Team team) {
        return team == Team.RED ? this.red : this.green;
    }

    public static MissileType byId(String id) {
        for (MissileType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
