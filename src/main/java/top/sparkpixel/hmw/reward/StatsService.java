package top.sparkpixel.hmw.reward;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import top.sparkpixel.hmw.HoneyMissileWarsPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flat-file player statistics (dataFolder/stats.yml), loaded async and saved
 * async on an interval, on quit and on disable.
 */
public final class StatsService {

    public static final class PlayerStats {
        public int games;
        public int wins;
        public int kills;
        public int deaths;
        public int missiles;
        public int shields;
        public int breaches;

        public void add(PlayerStats other) {
            this.games += other.games;
            this.wins += other.wins;
            this.kills += other.kills;
            this.deaths += other.deaths;
            this.missiles += other.missiles;
            this.shields += other.shields;
            this.breaches += other.breaches;
        }
    }

    private final HoneyMissileWarsPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerStats> stats = new ConcurrentHashMap<>();

    public StatsService(HoneyMissileWarsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
    }

    public void loadAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            Map<UUID, PlayerStats> loaded = new HashMap<>();
            if (this.file.exists()) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(this.file);
                ConfigurationSection root = yaml.getConfigurationSection("players");
                if (root != null) {
                    for (String key : root.getKeys(false)) {
                        try {
                            UUID uuid = UUID.fromString(key);
                            ConfigurationSection section = root.getConfigurationSection(key);
                            if (section == null) {
                                continue;
                            }
                            PlayerStats entry = new PlayerStats();
                            entry.games = section.getInt("games");
                            entry.wins = section.getInt("wins");
                            entry.kills = section.getInt("kills");
                            entry.deaths = section.getInt("deaths");
                            entry.missiles = section.getInt("missiles");
                            entry.shields = section.getInt("shields");
                            entry.breaches = section.getInt("breaches");
                            loaded.put(uuid, entry);
                        } catch (IllegalArgumentException ignored) {
                            // malformed key - skip
                        }
                    }
                }
            }
            Bukkit.getScheduler().runTask(this.plugin, () -> this.stats.putAll(loaded));
        });
        Bukkit.getScheduler().runTaskTimer(this.plugin, this::saveNowAsync,
                this.plugin.hmwConfig().statsSaveIntervalMinutes() * 60L * 20L,
                this.plugin.hmwConfig().statsSaveIntervalMinutes() * 60L * 20L);
    }

    public PlayerStats get(UUID playerId) {
        return this.stats.computeIfAbsent(playerId, id -> new PlayerStats());
    }

    /** Adds a finished game's deltas to the player's lifetime stats. */
    public void recordGame(UUID playerId, boolean won, int kills, int deaths,
                           int missiles, int shields, int breaches) {
        PlayerStats entry = get(playerId);
        synchronized (entry) {
            entry.games++;
            if (won) {
                entry.wins++;
            }
            entry.kills += kills;
            entry.deaths += deaths;
            entry.missiles += missiles;
            entry.shields += shields;
            entry.breaches += breaches;
        }
    }

    /** Live updates (kills/missiles/shields) right when they happen. */
    public void trackKill(UUID playerId) {
        get(playerId).kills++;
    }

    public void trackMissile(UUID playerId) {
        get(playerId).missiles++;
    }

    public void trackShield(UUID playerId) {
        get(playerId).shields++;
    }

    public List<Map.Entry<UUID, PlayerStats>> topByWins(int limit) {
        List<Map.Entry<UUID, PlayerStats>> entries = new ArrayList<>(this.stats.entrySet());
        entries.sort(Comparator.comparingInt((Map.Entry<UUID, PlayerStats> e) -> e.getValue().wins).reversed()
                .thenComparingInt(e -> e.getValue().games));
        return entries.subList(0, Math.min(limit, entries.size()));
    }

    public void saveNowAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, this::saveNow);
    }

    /** Blocking save (onDisable). */
    public void saveNow() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerStats> entry : this.stats.entrySet()) {
            String base = "players." + entry.getKey();
            PlayerStats value = entry.getValue();
            synchronized (value) {
                yaml.set(base + ".games", value.games);
                yaml.set(base + ".wins", value.wins);
                yaml.set(base + ".kills", value.kills);
                yaml.set(base + ".deaths", value.deaths);
                yaml.set(base + ".missiles", value.missiles);
                yaml.set(base + ".shields", value.shields);
                yaml.set(base + ".breaches", value.breaches);
            }
        }
        try {
            yaml.save(this.file);
        } catch (IOException ex) {
            this.plugin.getLogger().severe("Could not save stats.yml: " + ex.getMessage());
        }
    }
}
