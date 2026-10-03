package top.sparkpixel.hmw.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import top.sparkpixel.hmw.HoneyMissileWarsPlugin;
import top.sparkpixel.hmw.arena.ArenaVariant;
import top.sparkpixel.hmw.game.Game;
import top.sparkpixel.hmw.game.Team;
import top.sparkpixel.hmw.reward.StatsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** /hmw player commands. */
public final class HmwCommand implements CommandExecutor, TabCompleter {

    private final HoneyMissileWarsPlugin plugin;

    public HmwCommand(HoneyMissileWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Honey Missile Wars commands are player-only (except via /hmwadmin list).");
            return true;
        }
        String sub = args.length == 0 ? "join" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "join" -> {
                ArenaVariant preferred = args.length >= 2 ? matchArena(args[1]) : null;
                if (args.length >= 2 && preferred == null) {
                    player.sendMessage(this.plugin.messages().format("command.unknown-arena",
                            "<red>Unknown arena: {arena}</red>", "{arena}", args[1]));
                    return true;
                }
                this.plugin.gameManager().joinGame(player, preferred);
            }
            case "leave" -> this.plugin.gameManager().leaveGame(player);
            case "list" -> listGames(player);
            case "stats" -> {
                UUID target = player.getUniqueId();
                String name = player.getName();
                if (args.length >= 2) {
                    Player online = Bukkit.getPlayerExact(args[1]);
                    if (online != null) {
                        target = online.getUniqueId();
                        name = online.getName();
                    } else {
                        var offline = Bukkit.getOfflinePlayerIfCached(args[1]);
                        if (offline == null) {
                            player.sendMessage(this.plugin.messages().format("command.unknown-player",
                                    "<red>No known player: {player}</red>", "{player}", args[1]));
                            return true;
                        }
                        target = offline.getUniqueId();
                        name = offline.getName() == null ? args[1] : offline.getName();
                    }
                }
                sendStats(player, target, name);
            }
            case "top" -> sendTop(player);
            case "book" -> player.getInventory().addItem(manualBook()).values()
                    .forEach(rest -> player.getWorld().dropItemNaturally(player.getLocation(), rest));
            case "credits" -> sendCredits(player);
            default -> player.sendMessage(this.plugin.messages().format("command.usage",
                    "<red>Usage: /hmw <join|leave|list|stats|top|book|credits></red>"));
        }
        return true;
    }

    private ArenaVariant matchArena(String input) {
        for (ArenaVariant variant : ArenaVariant.values()) {
            if (variant.name().equalsIgnoreCase(input) || variant.displayName().equalsIgnoreCase(input)) {
                return variant;
            }
        }
        return null;
    }

    private void listGames(Player player) {
        var games = this.plugin.gameManager().games();
        if (games.isEmpty()) {
            player.sendMessage(this.plugin.messages().format("command.list-empty",
                    "<gray>No active game instances.</gray>"));
            return;
        }
        player.sendMessage(this.plugin.messages().format("command.list-header", "<gold>Active games:</gold>"));
        for (Game game : games) {
            player.sendMessage(Component.text()
                    .append(Component.text(" #" + game.id() + " ", NamedTextColor.YELLOW))
                    .append(Component.text(game.settings().arena.displayName(), NamedTextColor.WHITE))
                    .append(Component.text(" - " + game.phase(), NamedTextColor.GRAY))
                    .append(Component.text(" - " + game.teamSize(Team.RED) + "v" + game.teamSize(Team.GREEN),
                            NamedTextColor.AQUA))
                    .clickEvent(ClickEvent.runCommand("/hmw join " + game.settings().arena.name()))
                    .build());
        }
    }

    private void sendStats(Player player, UUID target, String name) {
        StatsService.PlayerStats stats = this.plugin.statsService().get(target);
        player.sendMessage(this.plugin.messages().format("command.stats-header",
                "<gold>--- {player} ---</gold>", "{player}", name));
        player.sendMessage(this.plugin.messages().format("command.stats-main",
                "<yellow>Games: {games}  Wins: {wins}  Kills: {kills}  Deaths: {deaths}</yellow>",
                "{games}", String.valueOf(stats.games),
                "{wins}", String.valueOf(stats.wins),
                "{kills}", String.valueOf(stats.kills),
                "{deaths}", String.valueOf(stats.deaths)));
        player.sendMessage(this.plugin.messages().format("command.stats-extra",
                "<yellow>Missiles placed: {missiles}  Shields: {shields}  Breaches: {breaches}</yellow>",
                "{missiles}", String.valueOf(stats.missiles),
                "{shields}", String.valueOf(stats.shields),
                "{breaches}", String.valueOf(stats.breaches)));
    }

    private void sendTop(Player player) {
        var top = this.plugin.statsService().topByWins(10);
        player.sendMessage(this.plugin.messages().format("command.top-header",
                "<gold>--- Top players by wins ---</gold>"));
        int rank = 1;
        for (Map.Entry<UUID, StatsService.PlayerStats> entry : top) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            player.sendMessage(this.plugin.messages().format("command.top-entry",
                    "<yellow>{rank}. {player} - {wins} wins ({games} games)</yellow>",
                    "{rank}", String.valueOf(rank),
                    "{player}", name == null ? "?" : name,
                    "{wins}", String.valueOf(entry.getValue().wins),
                    "{games}", String.valueOf(entry.getValue().games)));
            rank++;
        }
    }

    private void sendCredits(Player player) {
        player.sendMessage(Component.text("Honey Missile Wars", NamedTextColor.GOLD,
                net.kyori.adventure.text.format.TextDecoration.BOLD));
        player.sendMessage(Component.text("Original map: Chopper2112, Supersette and kruthers "
                + "(community missiles by IndigoLaser, Llew Vallis, Wasloigi & Pingu).",
                NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Original Missile Wars by Sethbling and Cubehamster.",
                NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Plugin conversion: HoneyMissileWars for Paper + SlimeWorld.",
                NamedTextColor.GRAY));
    }

    /** Condensed port of the map's Game Manual (chop:lobby/how_to_play). */
    private ItemStack manualBook() {
        ItemStack stack = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) stack.getItemMeta();
        meta.setTitle("Game Manual");
        meta.setAuthor("Supersette");
        meta.addPages(
                Component.text("Honey Missile Wars\n\n", NamedTextColor.GOLD)
                        .append(Component.text("Send missiles to the opposing team to blow up their walls "
                                + "and destroy their nether portal. Defend with honey blocks, shields and your "
                                + "bow. You can also ride missiles!\n", NamedTextColor.BLACK))
                        .append(Component.text("\nRight-click a missile egg on the ground to launch it.",
                                NamedTextColor.DARK_GREEN)),
                Component.text("Missile sets\n\n", NamedTextColor.GOLD)
                        .append(Component.text("Classic: Tomahawk, Juggernaut, Guardian, Shieldbuster, "
                                + "Lightning\nHoney Classic: honey-block versions\nExplosive: Clearer, Bomber, "
                                + "Dropper, Flat Head, Blocker\nRush: Bridge, Thunder Strike, Minibus, Tank, "
                                + "Transport\nSwitch: Trebuchet, Countdown, Undertaker, Bulldozer, Scorpion",
                                NamedTextColor.BLACK)),
                Component.text("Power items\n\n", NamedTextColor.GOLD)
                        .append(Component.text("Bow & Arrow - shoot down enemy missiles.\n\n", NamedTextColor.BLACK)
                                .append(Component.text("Shield - a snowball that becomes a wall.\n\n",
                                        NamedTextColor.BLACK))
                                .append(Component.text("Fireball - punch it to aim, then let it fly.\n\n",
                                        NamedTextColor.BLACK))
                                .append(Component.text("Grenade - explodes one second after the throw.",
                                        NamedTextColor.BLACK))),
                Component.text("Settings room\n\n", NamedTextColor.GOLD)
                        .append(Component.text("Walk through the side corridors of the lobby to reach the "
                                + "settings room. Right-click the signs to change arena, missile set, item rate, "
                                + "respawn time and more.", NamedTextColor.BLACK))
        );
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : List.of("join", "leave", "list", "stats", "top", "book", "credits")) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            for (ArenaVariant variant : ArenaVariant.values()) {
                if (variant.name().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    completions.add(variant.name().toLowerCase(Locale.ROOT));
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("stats")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    completions.add(online.getName());
                }
            }
        }
        return completions;
    }
}
