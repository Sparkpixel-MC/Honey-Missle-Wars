package top.sparkpixel.hmw.listener;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import top.sparkpixel.hmw.HoneyMissileWarsPlugin;
import top.sparkpixel.hmw.game.Game;
import top.sparkpixel.hmw.game.GamePhase;
import top.sparkpixel.hmw.game.PlayerSession;
import top.sparkpixel.hmw.item.ItemFactory;
import top.sparkpixel.hmw.missile.MissileType;

/**
 * Right-click handling for missile eggs and fireballs.
 * Missile placement replaces the datapack's spawn-egg -> AEC -> structure block
 * chain with a direct structure paste (kruthers missile:place_handling).
 */
public final class MissileListener implements Listener {

    private final HoneyMissileWarsPlugin plugin;

    public MissileListener(HoneyMissileWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        ItemFactory factory = this.plugin.itemFactory();

        String missileId = factory.missileId(item);
        if (missileId != null) {
            event.setCancelled(true);
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return; // datapack spawn eggs only trigger on a block hit
            }
            MissileType type = MissileType.byId(missileId);
            Game game = this.plugin.gameManager().gameOf(player);
            if (type == null || game == null || game.phase() != GamePhase.RUNNING) {
                return;
            }
            PlayerSession session = game.session(player);
            if (session == null || session.team == null || !session.alive
                    || player.getGameMode() != GameMode.SURVIVAL) {
                return;
            }
            Block clicked = event.getClickedBlock();
            BlockFace face = event.getBlockFace();
            if (clicked == null || face == null) {
                return;
            }
            Block base = clicked.getRelative(face);
            boolean placed = this.plugin.missileService().place(game, player, session.team, type, base);
            if (placed && player.getGameMode() != GameMode.CREATIVE) {
                consumeHeld(event);
            }
            return;
        }

        String special = factory.specialId(item);
        if ("FIREBALL".equals(special)) {
            event.setCancelled(true);
            Game game = this.plugin.gameManager().gameOf(player);
            if (game == null || game.phase() != GamePhase.RUNNING) {
                return;
            }
            PlayerSession session = game.session(player);
            if (session == null || session.team == null || !session.alive) {
                return;
            }
            Location spawn;
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                Block base = event.getClickedBlock().getRelative(event.getBlockFace());
                spawn = base.getLocation().add(0.5, 1.5, 0.5);
            } else {
                Vector dir = player.getLocation().getDirection().normalize();
                spawn = player.getEyeLocation().add(dir.multiply(2));
            }
            Fireball fireball = game.world().spawn(spawn, Fireball.class);
            fireball.setShooter(player);
            fireball.setDirection(new Vector(0, 0, 0)); // hovers until punched (map behaviour)
            fireball.setYield(1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.5f, 1.0f);
            if (player.getGameMode() != GameMode.CREATIVE) {
                consumeHeld(event);
            }
        }
    }

    private void consumeHeld(PlayerInteractEvent event) {
        ItemStack held = event.getItem();
        if (held != null) {
            held.setAmount(held.getAmount() - 1);
        }
    }
}
