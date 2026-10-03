package top.sparkpixel.hmw.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import top.sparkpixel.hmw.HoneyMissileWarsPlugin;
import top.sparkpixel.hmw.game.Game;
import top.sparkpixel.hmw.game.GamePhase;
import top.sparkpixel.hmw.game.PlayerSession;
import top.sparkpixel.hmw.game.Team;
import top.sparkpixel.hmw.item.ItemFactory;
import top.sparkpixel.hmw.missile.StructureRotation;
import top.sparkpixel.hmw.world.SlimeWorldService;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thrown power items: shield snowballs, grenade eggs and exploding arrows.
 * Ports sette:main (shields) and chop:game/spawn_grenade / as_grenade /
 * arrow_explode.
 */
public final class ProjectileListener implements Listener {

    private static final int SHIELD_LIFESPAN_TICKS = 20; // 1 second, like the datapack

    private record ShieldData(UUID owner, Team team, PlayerSession session) {
    }

    private record GrenadeData(TNTPrimed tnt) {
    }

    private final HoneyMissileWarsPlugin plugin;
    private final Map<UUID, ShieldData> shields = new ConcurrentHashMap<>();
    private final Map<UUID, GrenadeData> grenades = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> shieldLifespans = new ConcurrentHashMap<>();

    public ProjectileListener(HoneyMissileWarsPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickTrackers, 1L, 1L);
    }

    // ------------------------------------------------------------------
    // launch
    // ------------------------------------------------------------------

    @EventHandler
    public void onLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player player)) {
            return;
        }
        Game game = this.plugin.gameManager().gameOf(player);
        if (game == null) {
            return;
        }
        if (game.phase() != GamePhase.RUNNING) {
            event.setCancelled(true); // no throwing things around the lobby
            return;
        }
        PlayerSession session = game.session(player);
        if (session == null || session.team == null || !session.alive) {
            event.setCancelled(true);
            return;
        }

        ItemStack used = player.getInventory().getItemInMainHand();
        ItemFactory factory = this.plugin.itemFactory();
        if (projectile instanceof Snowball && "SHIELD".equals(factory.specialId(used))) {
            this.shields.put(projectile.getUniqueId(),
                    new ShieldData(player.getUniqueId(), session.team, session));
            this.shieldLifespans.put(projectile.getUniqueId(), 0);
        } else if (projectile instanceof Egg && "GRENADE".equals(factory.specialId(used))) {
            // TNT follows the egg, fuse 15 (sette/chop grenade behaviour)
            TNTPrimed tnt = game.world().spawn(projectile.getLocation(), TNTPrimed.class);
            tnt.setFuseTicks(15);
            tnt.setSource(player);
            this.grenades.put(projectile.getUniqueId(), new GrenadeData(tnt));
        }
    }

    // ------------------------------------------------------------------
    // impact
    // ------------------------------------------------------------------

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        UUID id = projectile.getUniqueId();

        ShieldData shield = this.shields.remove(id);
        if (shield != null) {
            this.shieldLifespans.remove(id);
            if (projectile.isValid()) {
                deployShield(projectile.getLocation(), shield);
            }
            return;
        }

        GrenadeData grenade = this.grenades.remove(id);
        if (grenade != null) {
            TNTPrimed tnt = grenade.tnt();
            if (tnt.isValid()) {
                tnt.teleport(projectile.getLocation());
                tnt.setFuseTicks(0); // detonate immediately on impact
            }
            return;
        }

        if (projectile instanceof Arrow && projectile.getShooter() instanceof Player player) {
            Game game = this.plugin.gameManager().gameOf(player);
            if (game != null && game.phase() == GamePhase.RUNNING && game.settings().explodingArrows) {
                projectile.remove();
                game.world().createExplosion(projectile.getLocation(), 1.0f, false, false);
            }
        }
    }

    /**
     * Game worlds run with mobGriefing=false, which downgrades vanilla fireball
     * explosions to "no block damage" (the portal never breaks). Replace the
     * vanilla explosion with a source-less one that always destroys blocks.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFireballExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Fireball fireball)) {
            return;
        }
        if (!fireball.getWorld().getName().startsWith(SlimeWorldService.GAME_WORLD_PREFIX)) {
            return;
        }
        event.setCancelled(true);
        // null source = BLOCK interaction, immune to the mobGriefing rule
        fireball.getWorld().createExplosion(fireball.getLocation(), fireball.getYield(), false, true);
    }

    /** Caps the fuse of every TNT primed inside a game world (missiles feel snappier). */
    @EventHandler
    public void onTntSpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt)) {
            return;
        }
        if (!tnt.getWorld().getName().startsWith(SlimeWorldService.GAME_WORLD_PREFIX)) {
            return;
        }
        tnt.setFuseTicks(Math.min(tnt.getFuseTicks(), this.plugin.hmwConfig().tntFuseTicks()));
    }

    private void deployShield(Location location, ShieldData data) {
        Player owner = this.plugin.getServer().getPlayer(data.owner());
        Game game = owner != null ? this.plugin.gameManager().gameOf(owner) : null;
        if (game == null || game.world() == null) {
            return;
        }
        String structure = data.team() == Team.RED ? "minecraft:red_shield" : "minecraft:green_shield";
        Location origin = location.clone().add(0, -3, -3); // structure block posY:-3 posZ:-3
        this.plugin.structureService().paste(game.world(), structure, origin,
                StructureRotation.NONE, false);
        if (data.session() != null) {
            data.session().shieldsDeployed++;
        }
        this.plugin.rewardService().awardShield(owner);
        owner.playSound(owner.getLocation(), Sound.BLOCK_HONEY_BLOCK_SLIDE, 1.0f, 0.0f);
    }

    // ------------------------------------------------------------------
    // per-tick trackers (grenade TNT follows the egg; shield auto-deploy)
    // ------------------------------------------------------------------

    private void tickTrackers() {
        // grenades: the TNT follows its egg while the egg is in flight
        Iterator<Map.Entry<UUID, GrenadeData>> grenadeIt = this.grenades.entrySet().iterator();
        while (grenadeIt.hasNext()) {
            var entry = grenadeIt.next();
            TNTPrimed tnt = entry.getValue().tnt();
            Projectile egg = (Projectile) org.bukkit.Bukkit.getEntity(entry.getKey());
            boolean eggAlive = egg != null && egg.isValid();
            if (!eggAlive || !tnt.isValid()) {
                // impact handled by the hit event; otherwise let the TNT fuse run out on its own
                grenadeIt.remove();
                continue;
            }
            tnt.teleport(egg.getLocation());
        }

        // shields: auto-deploy after 20 ticks in flight
        Iterator<Map.Entry<UUID, Integer>> shieldIt = this.shieldLifespans.entrySet().iterator();
        while (shieldIt.hasNext()) {
            var entry = shieldIt.next();
            int lifespan = entry.getValue() + 1;
            if (lifespan >= SHIELD_LIFESPAN_TICKS) {
                shieldIt.remove();
                ShieldData data = this.shields.remove(entry.getKey());
                Projectile snowball = (Projectile) org.bukkit.Bukkit.getEntity(entry.getKey());
                if (data != null && snowball != null && snowball.isValid()) {
                    deployShield(snowball.getLocation(), data);
                    snowball.remove();
                }
            } else {
                entry.setValue(lifespan);
            }
        }
    }
}
