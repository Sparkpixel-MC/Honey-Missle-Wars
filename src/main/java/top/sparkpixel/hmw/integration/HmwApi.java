package top.sparkpixel.hmw.integration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import top.sparkpixel.hmw.api.event.GameEndEvent;
import top.sparkpixel.hmw.api.event.GameStartEvent;
import top.sparkpixel.hmw.api.event.MissilePlaceEvent;
import top.sparkpixel.hmw.api.event.PlayerKillPlayerEvent;
import top.sparkpixel.hmw.api.event.WallBreachEvent;
import top.sparkpixel.hmw.arena.ArenaVariant;
import top.sparkpixel.hmw.game.Game;
import top.sparkpixel.hmw.game.GameManager;
import top.sparkpixel.hmw.game.Team;
import top.sparkpixel.hmw.missile.MissileType;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Public facade for other plugins / frontends of a big minigame network.
 * Obtain the game of a player, list games, and listen to the events in
 * {@link org.yeggs.hmw.api.event} for deeper integration.
 */
public final class HmwApi {

    private static GameManager manager;

    private HmwApi() {
    }

    public static void init(GameManager gameManager) {
        manager = gameManager;
    }

    public static Game gameOf(Player player) {
        return manager == null ? null : manager.gameOf(player);
    }

    public static Collection<Game> games() {
        return manager == null ? List.of() : manager.games();
    }

    /** @deprecated use {@link #joinGame(Player, ArenaVariant)}. */
    @Deprecated
    public static void joinGame(Player player) {
        joinGame(player, null);
    }

    /** Allocates the player into a joinable game (arena = null for the default). */
    public static void joinGame(Player player, ArenaVariant arena) {
        if (manager != null) {
            manager.joinGame(player, arena);
        }
    }

    public static boolean isInGame(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        return player != null && gameOf(player) != null;
    }

    // ------------------------------------------------------------------
    // event dispatchers (used internally by the game code, public for cross-package access)
    // ------------------------------------------------------------------

    public static void callGameStart(Game game) {
        Bukkit.getPluginManager().callEvent(new GameStartEvent(game));
    }

    public static void callGameEnd(Game game, Team winner) {
        Bukkit.getPluginManager().callEvent(new GameEndEvent(game, winner));
    }

    public static void callKill(Game game, UUID killer, UUID victim) {
        Bukkit.getPluginManager().callEvent(new PlayerKillPlayerEvent(game, killer, victim));
    }

    public static void callMissilePlace(Player player, Game game, MissileType type, Location location) {
        Bukkit.getPluginManager().callEvent(new MissilePlaceEvent(game, player, type, location));
    }

    public static void callWallBreach(Game game, Team defender, int stage) {
        Bukkit.getPluginManager().callEvent(new WallBreachEvent(game, defender, stage));
    }
}
