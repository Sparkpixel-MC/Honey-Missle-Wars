package top.sparkpixel.hmwlobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lobby-side companion of Honey Missile Wars. Provides the command an NPC
 * plugin binds ("hmwlobby join &lt;arena&gt;"); the player is pushed to the
 * game server via BungeeCord and the arena preference is forwarded on the
 * "HMW" Forward subchannel so the main plugin can queue them for that arena.
 */
public final class HmwLobbyPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    /** Half a second between the Forward packet and the Connect packet. */
    private static final long CONNECT_DELAY_TICKS = 10L;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        var command = getCommand("hmwlobby");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }
        getLogger().info("HMWLobby enabled. Bind NPCs to '/hmwlobby join <arena>' "
                + "(target server: " + getConfig().getString("target-server", "hmw") + ").");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "join" -> handleJoin(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "player-only");
            return;
        }
        if (!player.hasPermission("hmwlobby.use")) {
            send(sender, "no-permission");
            return;
        }
        String arena = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        String server = getConfig().getString("target-server", "hmw");
        ConfigurationSection arenas = getConfig().getConfigurationSection("arenas");

        if (arena.isEmpty()) {
            send(player, "sending-default", "{server}", server);
        } else {
            String display = arenas != null ? arenas.getString(arena, null) : null;
            if (display == null) {
                send(player, "invalid-arena", "{arena}", arena);
                return;
            }
            send(player, "sending", "{arena}", display, "{server}", server);
        }
        forwardArena(player, arena);
        connectLater(player);
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("hmwlobby.use")) {
            send(sender, "no-permission");
            return;
        }
        send(sender, "list-header");
        ConfigurationSection arenas = getConfig().getConfigurationSection("arenas");
        if (arenas != null) {
            for (String id : arenas.getKeys(false)) {
                send(sender, "list-entry", "{id}", id, "{name}", arenas.getString(id, id));
            }
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("hmwlobby.admin")) {
            send(sender, "no-permission");
            return;
        }
        reloadConfig();
        send(sender, "reloaded");
    }

    /** BungeeCord "Forward" -> subchannel "HMW": uuid + arena id ("" = default arena). */
    private void forwardArena(Player player, String arena) {
        try {
            ByteArrayOutputStream payload = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(payload);
            data.writeUTF(player.getUniqueId().toString());
            data.writeUTF(arena);

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF("Forward");
            out.writeUTF(getConfig().getString("target-server", "hmw"));
            out.writeUTF("HMW");
            out.writeShort(payload.size());
            out.write(payload.toByteArray());
            player.sendPluginMessage(this, "BungeeCord", bytes.toByteArray());
        } catch (IOException ex) {
            getLogger().warning("Arena forward failed: " + ex.getMessage());
        }
    }

    /** Sent shortly after the Forward packet so the preference lands first. */
    private void connectLater(Player player) {
        getServer().getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline()) {
                return;
            }
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bytes);
                out.writeUTF("Connect");
                out.writeUTF(getConfig().getString("target-server", "hmw"));
                player.sendPluginMessage(this, "BungeeCord", bytes.toByteArray());
            } catch (IOException ex) {
                getLogger().warning("BungeeCord connect failed: " + ex.getMessage());
            }
        }, CONNECT_DELAY_TICKS);
    }

    private void send(CommandSender to, String key, String... replacements) {
        String raw = getConfig().getString("messages." + key, key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        Component parsed = MINI.deserialize(raw);
        if (to instanceof Player player) {
            player.sendMessage(parsed);
        } else {
            to.sendMessage(parsed);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return prefixMatch(args[0], List.of("join", "list", "reload"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            ConfigurationSection arenas = getConfig().getConfigurationSection("arenas");
            if (arenas != null) {
                return prefixMatch(args[1], new ArrayList<>(arenas.getKeys(false)));
            }
        }
        return List.of();
    }

    private static List<String> prefixMatch(String input, List<String> options) {
        String lower = input.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .toList();
    }
}
