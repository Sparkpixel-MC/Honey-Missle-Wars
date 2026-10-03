package top.sparkpixel.hmw.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import top.sparkpixel.hmw.HoneyMissileWarsPlugin;
import top.sparkpixel.hmw.game.Game;
import top.sparkpixel.hmw.game.GamePhase;
import top.sparkpixel.hmw.game.PlayerSession;

/**
 * Build rules: lobby is fully protected; during the battle team members may
 * break and place (honey defence!), with own-wall mining feeding the breach
 * exemption of the original gameloop.
 */
public final class BlockListener implements Listener {

    private final HoneyMissileWarsPlugin plugin;

    public BlockListener(HoneyMissileWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Game game = this.plugin.gameManager().gameOf(player);
        if (game == null) {
            return; // not our world
        }
        PlayerSession session = game.session(player);
        if (game.phase() != GamePhase.RUNNING || session == null
                || session.team == null || !session.alive) {
            event.setCancelled(true);
            return;
        }
        // own-team glass/honey mining delays the breach counter (see Game.scanBreaches)
        if (session.team.selfBreakExemptions().contains(event.getBlock().getType())) {
            game.noteSelfBreak(session.team);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Game game = this.plugin.gameManager().gameOf(player);
        if (game == null) {
            return;
        }
        PlayerSession session = game.session(player);
        if (game.phase() != GamePhase.RUNNING || session == null
                || session.team == null || !session.alive) {
            event.setCancelled(true);
        }
    }
}
