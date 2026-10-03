package top.sparkpixel.hmw.integration;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import top.sparkpixel.hmw.HoneyMissileWarsPlugin;
import top.sparkpixel.hmw.game.Game;
import top.sparkpixel.hmw.reward.StatsService;

/**
 * PlaceholderAPI expansion (soft dependency - this class is only instantiated
 * when PlaceholderAPI is installed).
 *
 * Placeholders: %hmw_games%, %hmw_wins%, %hmw_kills%, %hmw_deaths%,
 * %hmw_missiles%, %hmw_shields%, %hmw_breaches%, %hmw_game%, %hmw_team%
 */
public final class PlaceholderHook {

    private PlaceholderHook() {
    }

    public static void register(HoneyMissileWarsPlugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            new Expansion(plugin).register();
            plugin.getLogger().info("PlaceholderAPI expansion registered.");
        } catch (Throwable t) {
            plugin.getLogger().warning("Could not register PlaceholderAPI expansion: " + t.getMessage());
        }
    }

    private static final class Expansion extends me.clip.placeholderapi.expansion.PlaceholderExpansion {

        private final HoneyMissileWarsPlugin plugin;

        private Expansion(HoneyMissileWarsPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public String getIdentifier() {
            return "hmw";
        }

        @Override
        public String getAuthor() {
            return "Chopper2112, Supersette, kruthers";
        }

        @Override
        public String getVersion() {
            return this.plugin.getDescription().getVersion();
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public String onRequest(OfflinePlayer offlinePlayer, String params) {
            StatsService.PlayerStats stats = offlinePlayer == null
                    ? new StatsService.PlayerStats()
                    : this.plugin.statsService().get(offlinePlayer.getUniqueId());
            switch (params.toLowerCase()) {
                case "games" -> {
                    return String.valueOf(stats.games);
                }
                case "wins" -> {
                    return String.valueOf(stats.wins);
                }
                case "kills" -> {
                    return String.valueOf(stats.kills);
                }
                case "deaths" -> {
                    return String.valueOf(stats.deaths);
                }
                case "missiles" -> {
                    return String.valueOf(stats.missiles);
                }
                case "shields" -> {
                    return String.valueOf(stats.shields);
                }
                case "breaches" -> {
                    return String.valueOf(stats.breaches);
                }
                default -> {
                }
            }
            if (offlinePlayer == null || !offlinePlayer.isOnline()) {
                return "";
            }
            Player player = (Player) offlinePlayer;
            Game game = this.plugin.gameManager().gameOf(player);
            if (params.equalsIgnoreCase("game")) {
                return game == null ? "none"
                        : game.settings().arena.displayName() + " #" + game.id();
            }
            if (params.equalsIgnoreCase("team")) {
                var team = game == null ? null : game.teamOf(player);
                return team == null ? "none" : team.miniTag();
            }
            return null;
        }
    }
}
