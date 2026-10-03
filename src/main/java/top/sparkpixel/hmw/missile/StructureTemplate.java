package top.sparkpixel.hmw.missile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parsed vanilla structure template (.nbt): block palette + block positions +
 * entities. Supported layout is the modern "palette"/"blocks"/"entities"
 * format (1.16+), including the optional "palettes" + "data" variant.
 */
final class StructureTemplate {

    record BlockEntry(int x, int y, int z, int state) {
    }

    record EntityEntry(double x, double y, double z, String id) {
    }

    private final List<Map<String, String>> palette;
    private final org.bukkit.Material[] materials;
    private final List<BlockEntry> blocks;
    private final List<EntityEntry> entities;

    private StructureTemplate(List<Map<String, String>> palette, org.bukkit.Material[] materials,
                              List<BlockEntry> blocks, List<EntityEntry> entities) {
        this.palette = palette;
        this.materials = materials;
        this.blocks = blocks;
        this.entities = entities;
    }

    Map<String, String> properties(int state) {
        return this.palette.get(state);
    }

    org.bukkit.Material material(int state) {
        return this.materials[state];
    }

    List<BlockEntry> blocks() {
        return this.blocks;
    }

    List<EntityEntry> entities() {
        return this.entities;
    }

    @SuppressWarnings("unchecked")
    static StructureTemplate parse(File file) throws IOException {
        Map<String, Object> root = NbtReader.read(file);

        List<Map<String, String>> palette = new ArrayList<>();
        int[] stateMap = null;

        if (root.get("palette") instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> compound) {
                    palette.add(readPaletteEntry((Map<String, Object>) compound));
                }
            }
        } else if (root.get("palettes") instanceof List<?> palettes && !palettes.isEmpty()
                && palettes.get(0) instanceof Map<?, ?> first) {
            if (first.get("palette") instanceof List<?> list) {
                for (Object entry : list) {
                    if (entry instanceof Map<?, ?> compound) {
                        palette.add(readPaletteEntry((Map<String, Object>) compound));
                    }
                }
            }
            if (first.get("data") instanceof int[] data) {
                stateMap = data;
            }
        }
        if (palette.isEmpty()) {
            throw new IOException("Structure has no palette: " + file.getName());
        }

        org.bukkit.Material[] materials = new org.bukkit.Material[palette.size()];
        for (int i = 0; i < palette.size(); i++) {
            String name = palette.get(i).getOrDefault("Name", "minecraft:air");
            String key = name.contains(":") ? name.substring(name.indexOf(':') + 1) : name;
            materials[i] = org.bukkit.Material.matchMaterial(key.toUpperCase());
        }

        List<BlockEntry> blocks = new ArrayList<>();
        if (root.get("blocks") instanceof List<?> list) {
            int index = 0;
            for (Object entry : list) {
                if (!(entry instanceof Map<?, ?> compound)) {
                    continue;
                }
                if (compound.get("pos") instanceof List<?> coords && coords.size() >= 3
                        && compound.get("state") instanceof Number number) {
                    int state = number.intValue();
                    if (stateMap != null && index < stateMap.length) {
                        state = stateMap[index];
                    }
                    blocks.add(new BlockEntry(asInt(coords.get(0)), asInt(coords.get(1)),
                            asInt(coords.get(2)), state));
                }
                index++;
            }
        }

        List<EntityEntry> entities = new ArrayList<>();
        if (root.get("entities") instanceof List<?> list) {
            for (Object entry : list) {
                if (!(entry instanceof Map<?, ?> compound)) {
                    continue;
                }
                if (!(compound.get("pos") instanceof List<?> coords) || coords.size() < 3) {
                    continue;
                }
                if (!(compound.get("nbt") instanceof Map<?, ?> nbt)) {
                    continue;
                }
                if (nbt.get("id") instanceof String id) {
                    entities.add(new EntityEntry(asDouble(coords.get(0)), asDouble(coords.get(1)),
                            asDouble(coords.get(2)), id));
                }
            }
        }

        return new StructureTemplate(palette, materials, blocks, entities);
    }

    private static Map<String, String> readPaletteEntry(Map<String, Object> compound) {
        Map<String, String> entry = new HashMap<>();
        if (compound.get("Name") instanceof String name) {
            entry.put("Name", name);
        }
        if (compound.get("Properties") instanceof Map<?, ?> properties) {
            for (Map.Entry<?, ?> property : ((Map<Object, Object>) properties).entrySet()) {
                if (property.getKey() instanceof String key && property.getValue() != null) {
                    entry.put(key, String.valueOf(property.getValue()));
                }
            }
        }
        return entry;
    }

    private static int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static double asDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }
}
