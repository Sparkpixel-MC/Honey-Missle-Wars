package top.sparkpixel.hmw.missile;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.Piston;
import org.bukkit.entity.EntityType;
import top.sparkpixel.hmw.HoneyMissileWarsPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads the 41 missile/shield structure files shipped inside the plugin jar
 * (extracted from the map's generated/minecraft/structures and
 * generated/community/structures) and pastes them into any Bukkit world -
 * including slime worlds, which have no "generated" folder of their own.
 *
 * <p>Vanilla structure templates are parsed with the bundled mini NBT reader
 * ({@link NbtReader} + {@link StructureTemplate}) and pasted block-by-block
 * with vanilla rotation semantics - no dependency on the Bukkit structure API
 * (org.bukkit.structure / org.bukkit.block.structure, which current Paper
 * builds no longer expose).
 */
public final class StructureService {

    private final HoneyMissileWarsPlugin plugin;
    private final Map<String, StructureTemplate> cache = new ConcurrentHashMap<>();

    public StructureService(HoneyMissileWarsPlugin plugin) {
        this.plugin = plugin;
        extractStructures();
    }

    private void extractStructures() {
        extractFolder("structures/minecraft");
        extractFolder("structures/community");
    }

    /** Copies every bundled .nbt resource into the data folder (once). */
    private void extractFolder(String jarFolder) {
        File targetDir = new File(this.plugin.getDataFolder(), jarFolder);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            this.plugin.getLogger().warning("Could not create " + targetDir);
            return;
        }
        String namespace = jarFolder.endsWith("community") ? "community" : "minecraft";
        for (String name : structureNames(namespace)) {
            File target = new File(targetDir, name + ".nbt");
            if (target.exists()) {
                continue;
            }
            String resource = jarFolder + "/" + name + ".nbt";
            try (InputStream in = this.plugin.getResource(resource)) {
                if (in == null) {
                    this.plugin.getLogger().warning("Missing bundled structure: " + resource);
                    continue;
                }
                Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                this.plugin.getLogger().warning("Failed to extract " + resource + ": " + ex.getMessage());
            }
        }
    }

    private java.util.List<String> structureNames(String namespace) {
        if ("minecraft".equals(namespace)) {
            return java.util.List.of(
                    "blocker", "bomber", "bridge", "clearer", "dropper", "flat_head",
                    "green_shield", "guardian", "honey_guardian", "honey_juggernaut",
                    "honey_lightning", "honey_shieldbuster", "honey_tomahawk",
                    "juggernaut", "kodachi", "lightning", "minibus", "rainbow_unicorn",
                    "red_shield", "sheild_missile", "shield", "shield_missile",
                    "shieldbuster", "tank", "thunder_strike", "tomahawk", "transport");
        }
        return java.util.List.of(
                "bulldozer", "countdown", "doubledip", "el_derecho", "epsilon",
                "green_annihilator", "hit_and_run", "kodachi", "red_annihilator",
                "scorpion", "torpedo", "trebuchet", "undertaker");
    }

    /** Loads (and caches) a parsed template by "namespace:name". Returns null when unavailable. */
    public StructureTemplate template(String key) {
        return this.cache.computeIfAbsent(key, k -> {
            int split = k.indexOf(':');
            String namespace = split > 0 ? k.substring(0, split) : "minecraft";
            String name = split > 0 ? k.substring(split + 1) : k;
            File file = new File(this.plugin.getDataFolder(), "structures/" + namespace + "/" + name + ".nbt");
            if (!file.exists()) {
                this.plugin.getLogger().warning("Structure file missing: " + file);
                return null;
            }
            try {
                return StructureTemplate.parse(file);
            } catch (IOException ex) {
                this.plugin.getLogger().warning("Failed to parse structure " + key + ": " + ex.getMessage());
                return null;
            }
        });
    }

    /**
     * Pastes a structure with its origin corner (template position 0,0,0) at the
     * given block location, applying vanilla rotation semantics: rotated blocks
     * extend into negative coordinates from the origin, exactly like a
     * structure block with the same rotation - the offsets in
     * {@link MissileType} were calibrated against that behaviour.
     */
    public boolean paste(World world, String structureKey, Location origin,
                         StructureRotation rotation, boolean includeEntities) {
        return paste(world, structureKey, origin, rotation, includeEntities, true);
    }

    /**
     * Pastes a structure. With {@code activate} the paste sends block updates
     * afterwards so piston-bolt missiles start flying; static display models
     * (lobby missile stands) must pass {@code false} or they fly away too.
     */
    public boolean paste(World world, String structureKey, Location origin,
                         StructureRotation rotation, boolean includeEntities, boolean activate) {
        StructureTemplate template = template(structureKey);
        if (template == null || origin.getWorld() == null) {
            return false;
        }
        int originX = origin.getBlockX();
        int originY = origin.getBlockY();
        int originZ = origin.getBlockZ();

        List<Block> placed = new ArrayList<>(template.blocks().size());
        for (StructureTemplate.BlockEntry entry : template.blocks()) {
            Material material = template.material(entry.state());
            if (material == null || material.isAir() || material == Material.STRUCTURE_VOID
                    || material == Material.MOVING_PISTON) {
                continue;
            }
            int x = originX + rotation.transformX(entry.x(), entry.z());
            int y = originY + entry.y();
            int z = originZ + rotation.transformZ(entry.x(), entry.z());
            Block block = world.getBlockAt(x, y, z);
            BlockData data = material.createBlockData();
            applyProperties(data, template.properties(entry.state()), rotation);
            block.setBlockData(data, false);
            placed.add(block);
        }

        // The missiles are piston-bolt flying machines that the datapack starts by
        // loading them through a powered structure block. A plain paste never sends
        // block updates, so the pistons stay dormant forever. Once the whole machine
        // is inert-place, re-set every block (air -> data with physics) so every
        // piston/observer receives a neighbor update and the engine starts. A second
        // pass wakes machines that only react after the first round of updates
        // settled (observers watching other observers, alternating piston bolts).
        if (activate) {
            for (int pass = 0; pass < 2; pass++) {
                for (Block block : placed) {
                    BlockData data = block.getBlockData();
                    block.setType(Material.AIR, false);
                    block.setBlockData(data, true);
                }
            }
        }

        if (includeEntities) {
            for (StructureTemplate.EntityEntry entry : template.entities()) {
                EntityType type = resolveEntityType(entry.id());
                if (type == null || !type.isSpawnable()) {
                    continue;
                }
                double x = originX + rotation.transformX(entry.x(), entry.z());
                double y = originY + entry.y();
                double z = originZ + rotation.transformZ(entry.x(), entry.z());
                world.spawnEntity(new Location(world, x, y, z), type);
            }
        }
        return true;
    }

    private void applyProperties(BlockData data, Map<String, String> properties, StructureRotation rotation) {
        if (properties == null) {
            return;
        }
        for (Map.Entry<String, String> property : properties.entrySet()) {
            String value = property.getValue();
            switch (property.getKey()) {
                case "facing" -> {
                    if (data instanceof Directional directional) {
                        org.bukkit.block.BlockFace face = facingFrom(value);
                        if (face != null) {
                            directional.setFacing(rotation.transformFacing(face));
                        }
                    }
                }
                case "axis" -> {
                    if (data instanceof Orientable orientable) {
                        orientable.setAxis(rotation.transformAxis(
                                "x".equals(value) ? org.bukkit.Axis.X
                                        : "z".equals(value) ? org.bukkit.Axis.Z : org.bukkit.Axis.Y));
                    }
                }
                case "rotation" -> {
                    if (data instanceof Rotatable rotatable) {
                        try {
                            rotatable.setRotation(rotation.transformRotationFace(Integer.parseInt(value)));
                        } catch (NumberFormatException ignored) {
                            // malformed property - keep default
                        }
                    }
                }
                case "extended" -> {
                    if (data instanceof Piston piston) {
                        piston.setExtended(Boolean.parseBoolean(value));
                    }
                }
                default -> {
                    // powered, waterlogged, ... - keep the placed default
                }
            }
        }
    }

    private org.bukkit.block.BlockFace facingFrom(String value) {
        return switch (value) {
            case "north" -> org.bukkit.block.BlockFace.NORTH;
            case "south" -> org.bukkit.block.BlockFace.SOUTH;
            case "east" -> org.bukkit.block.BlockFace.EAST;
            case "west" -> org.bukkit.block.BlockFace.WEST;
            case "up" -> org.bukkit.block.BlockFace.UP;
            case "down" -> org.bukkit.block.BlockFace.DOWN;
            default -> null;
        };
    }

    private EntityType resolveEntityType(String id) {
        String key = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        return EntityType.fromName(key);
    }
}
