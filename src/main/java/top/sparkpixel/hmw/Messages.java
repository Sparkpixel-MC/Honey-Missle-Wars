package top.sparkpixel.hmw;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Locale;

/**
 * MiniMessage helper with a message table. Lookup order per key:
 * <ol>
 *   <li>the language bundle (messages_&lt;lang&gt;.yml, chosen by "language: " in config.yml)</li>
 *   <li>the "messages." section of config.yml (manual overrides)</li>
 *   <li>the fallback string passed by the caller</li>
 * </ol>
 */
public final class Messages {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static final String PREFIX = "<bold><gold>HMW</gold></bold> <dark_gray>»</dark_gray> ";

    private final HmwConfig config;
    private final FileConfiguration bundle;

    public Messages(HoneyMissileWarsPlugin plugin) {
        this.config = plugin.hmwConfig();
        this.bundle = loadBundle(plugin);
    }

    public Component format(String key, String fallback, String... placeholders) {
        String filled = PREFIX + lookup(key, fallback);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            filled = filled.replace(placeholders[i], placeholders[i + 1]);
        }
        return MM.deserialize(filled);
    }

    /** Actionbar/title/particles have no prefix. */
    public Component formatRaw(String key, String fallback, String... placeholders) {
        String raw = lookup(key, fallback);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            raw = raw.replace(placeholders[i], placeholders[i + 1]);
        }
        return MM.deserialize(raw);
    }

    /** Deserializes a dynamic MiniMessage string that was already translated (e.g. startCountdown errors). */
    public Component mini(String minimessage) {
        return MM.deserialize(minimessage);
    }

    /** Raw string lookup without deserialization, for building messages at the call site. */
    public String raw(String key, String fallback, String... placeholders) {
        String raw = lookup(key, fallback);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            raw = raw.replace(placeholders[i], placeholders[i + 1]);
        }
        return raw;
    }

    private String lookup(String key, String fallback) {
        if (this.bundle != null) {
            String translated = this.bundle.getString(key);
            if (translated != null && !translated.isBlank()) {
                return translated;
            }
        }
        String override = this.config.raw().getString("messages." + key);
        if (override != null && !override.isBlank()) {
            return override;
        }
        return fallback;
    }

    /** Extracts and loads the language bundle for the configured language, or null for English. */
    private static FileConfiguration loadBundle(HoneyMissileWarsPlugin plugin) {
        String language = plugin.hmwConfig().language();
        if (language == null || language.isBlank() || language.equalsIgnoreCase("en")) {
            return null;
        }
        String base = language.toLowerCase(Locale.ROOT).split("[_-]")[0];
        String fileName = "messages_" + base + ".yml";
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            try {
                plugin.saveResource(fileName, false);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("No language bundle '" + fileName + "' found in the plugin jar: "
                        + ex.getMessage());
                return null;
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }
}
