package top.sparkpixel.hmw.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jspecify.annotations.NonNull;
import top.sparkpixel.hmw.HoneyMissileWarsPlugin;
import top.sparkpixel.hmw.arena.ArenaGenerator;
import top.sparkpixel.hmw.game.Game;
import top.sparkpixel.hmw.game.GamePhase;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** /hmwadmin management commands. */
public final class HmwAdminCommand implements CommandExecutor, TabCompleter {

    private final HoneyMissileWarsPlugin plugin;

    public HmwAdminCommand(HoneyMissileWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "import" -> importWorld(sender, args);
            case "templates" -> sender.sendMessage(Component.text(
                    "Base template '" + this.plugin.hmwConfig().baseTemplate() + "': "
                            + (this.plugin.worldService().templateExists() ? "present" : "MISSING"
                            + " - run /hmwadmin import <world folder>"), NamedTextColor.YELLOW));
            case "list" -> listGames(sender);
            case "forcestart" -> forceStart(sender);
            case "end" -> endGame(sender);
            case "regenerate" -> regenerate(sender);
            case "reload" -> {
                this.plugin.reloadPluginConfig();
                sender.sendMessage(Component.text("Configuration reloaded.", NamedTextColor.GREEN));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("HoneyMissileWars admin:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/hmwadmin import <world folder> [template name] - import an anvil "
                + "map as a slime template (arena base or waiting room)", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hmwadmin templates | list | forcestart | end | regenerate | reload",
                NamedTextColor.YELLOW));
    }

    private void importWorld(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /hmwadmin import <path to the extracted map folder> "
                    + "[template name]", NamedTextColor.RED));
            return;
        }
        if (!this.plugin.worldService().isAspAvailable()) {
            sender.sendMessage(Component.text("Advanced Slime Paper is not installed on this server!",
                    NamedTextColor.RED));
            return;
        }
        String templateName;
        String path;
        if (args.length >= 3) {
            templateName = args[args.length - 1];
            path = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1));
        } else {
            templateName = this.plugin.hmwConfig().baseTemplate();
            path = args[1];
        }
        File dir = new File(path);
        if (!dir.isDirectory() || !new File(dir, "level.dat").isFile()) {
            sender.sendMessage(Component.text("Not a valid world folder: " + path, NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("Import started (async) as template '" + templateName
                + "'. The world is read and converted to the slime format - watch the console.",
                NamedTextColor.YELLOW));
        this.plugin.worldService().importVanillaWorld(dir, templateName)
                .thenAccept(message -> Bukkit.getScheduler().runTask(this.plugin,
                        () -> sender.sendMessage(Component.text(message, NamedTextColor.GREEN))));
    }

    private void listGames(CommandSender sender) {
        var games = this.plugin.gameManager().games();
        sender.sendMessage(Component.text("Instances: " + games.size() + "/"
                + this.plugin.hmwConfig().maxInstances(), NamedTextColor.GOLD));
        for (Game game : games) {
            sender.sendMessage(Component.text(" #" + game.id() + " " + game.worldName()
                    + " - " + game.settings().arena.displayName()
                    + " - " + game.phase()
                    + " - " + game.playerCount() + " players", NamedTextColor.YELLOW));
        }
    }

    private void forceStart(CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player player)) {
            sender.sendMessage(Component.text("Console must use /hmwadmin end instead.", NamedTextColor.RED));
            return;
        }
        Game game = this.plugin.gameManager().gameOf(player);
        if (game == null) {
            sender.sendMessage(Component.text("You are not in a game.", NamedTextColor.RED));
            return;
        }
        if (game.phase() == GamePhase.WAITING) {
            String error = game.startCountdown();
            sender.sendMessage(Component.text(error == null ? "Game started." : error,
                    error == null ? NamedTextColor.GREEN : NamedTextColor.RED));
        } else {
            sender.sendMessage(Component.text("Game is not waiting.", NamedTextColor.RED));
        }
    }

    private void endGame(CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player player)) {
            for (Game game : this.plugin.gameManager().games()) {
                game.end(null, this.plugin.messages().raw("game.reason-admin", "Game ended by an administrator."));
            }
            sender.sendMessage(Component.text("All games ended.", NamedTextColor.GREEN));
            return;
        }
        Game game = this.plugin.gameManager().gameOf(player);
        if (game == null || game.phase() != GamePhase.RUNNING) {
            sender.sendMessage(Component.text("You are not in a running game.", NamedTextColor.RED));
            return;
        }
        game.end(null, this.plugin.messages().raw("game.reason-admin", "Game ended by an administrator."));
    }

    /** Synchronously regenerates the current arena of the game the sender is in (admin tool). */
    private void regenerate(CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player player)) {
            sender.sendMessage(Component.text("Player-only.", NamedTextColor.RED));
            return;
        }
        Game game = this.plugin.gameManager().gameOf(player);
        if (game == null || game.world() == null) {
            sender.sendMessage(Component.text("You are not in a game.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("Regenerating arena (synchronous, brief lag)...",
                NamedTextColor.YELLOW));
        ArenaGenerator.generateSync(game.world(), game.settings().arena);
        sender.sendMessage(Component.text("Done.", NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : List.of("import", "templates", "list", "forcestart", "end", "regenerate", "reload")) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    completions.add(sub);
                }
            }
        }
        return completions;
    }
}
