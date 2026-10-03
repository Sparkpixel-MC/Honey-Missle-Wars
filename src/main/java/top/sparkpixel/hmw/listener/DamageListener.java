package top.sparkpixel.hmw.listener;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import top.sparkpixel.hmw.HoneyMissileWarsPlugin;
import top.sparkpixel.hmw.game.Game;
import top.sparkpixel.hmw.game.GamePhase;
import top.sparkpixel.hmw.game.PlayerSession;

/**
 * Damage rules, kill attribution and death handling.
 */
public final class DamageListener implements Listener {

    private final HoneyMissileWarsPlugin plugin;

    public DamageListener(HoneyMissileWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Game game = this.plugin.gameManager().gameOf(victim);
        if (game == null) {
            return;
        }
        if (game.phase() != GamePhase.RUNNING) {
            event.setCancelled(true);
            return;
        }
        PlayerSession victimSession = game.session(victim);
        if (victimSession == null || victimSession.team == null || !victimSession.alive) {
            event.setCancelled(true);
            return;
        }

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker != null && attacker != victim) {
            PlayerSession attackerSession = game.session(attacker);
            if (attackerSession == null || attackerSession.team == null || !attackerSession.alive) {
                event.setCancelled(true);
                return;
            }
            if (attackerSession.team == victimSession.team) {
                event.setCancelled(true); // friendly fire is off (vanilla team backup)
                return;
            }
        }

        // record attribution for kill credit (10s window, like typical minigame servers)
        if (attacker != null) {
            PlayerSession source = game.session(attacker);
            if (source != null) {
                victimSession.lastDamager = attacker.getUniqueId();
                victimSession.lastDamageAt = System.currentTimeMillis();
            }
        }
    }

    private Player resolveAttacker(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLobbyDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Game game = this.plugin.gameManager().gameOf(victim);
        if (game == null) {
            return;
        }
        PlayerSession session = game.session(victim);
        if (game.phase() == GamePhase.RUNNING) {
            if (session != null && session.team != null && !session.alive) {
                event.setCancelled(true); // dead players hover invulnerably while waiting to respawn
            }
            return;
        }
        event.setCancelled(true); // lobby/countdown/ending phases are damage-free
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Game game = this.plugin.gameManager().gameOf(player);
        if (game != null) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Game game = this.plugin.gameManager().gameOf(victim);
        if (game == null) {
            return;
        }
        event.deathMessage(null); // showDeathMessages is off in the map
        PlayerSession session = game.session(victim);
        if (game.phase() == GamePhase.RUNNING && session != null) {
            game.handleDeath(victim, session);
        }
    }
}
