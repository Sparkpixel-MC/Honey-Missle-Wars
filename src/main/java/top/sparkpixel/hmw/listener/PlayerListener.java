package top.sparkpixel.hmw.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import top.sparkpixel.hmw.HoneyMissileWarsPlugin;
import top.sparkpixel.hmw.game.Game;
import top.sparkpixel.hmw.game.GamePhase;
import top.sparkpixel.hmw.game.PlayerSession;

/**
 * Join/quit rejoin handling, portal protection and lobby drop rules.
 */
public final class PlayerListener implements Listener {

    private final HoneyMissileWarsPlugin plugin;

    public PlayerListener(HoneyMissileWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.plugin.gameManager().handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.plugin.gameManager().handleQuit(event.getPlayer());
        this.plugin.statsService().saveNowAsync();
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        Game game = this.plugin.gameManager().gameOf(event.getPlayer());
        if (game != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Game game = this.plugin.gameManager().gameOf(player);
        if (game != null && game.phase() != GamePhase.RUNNING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Game game = this.plugin.gameManager().gameOf(player);
        if (game == null) {
            return;
        }
        PlayerSession session = game.session(player);
        if (session == null || !session.alive) {
            event.setCancelled(true); // dead players must not vacuum up drops while invisible
        }
    }
}
