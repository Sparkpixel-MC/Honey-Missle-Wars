package top.sparkpixel.hmw.missile;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import top.sparkpixel.hmw.HoneyMissileWarsPlugin;
import top.sparkpixel.hmw.game.Game;
import top.sparkpixel.hmw.item.MissileSet;

import java.util.List;

/**
 * Lobby missile-set display: refreshes the floating display items and the five
 * missile models on the lobby stands whenever the missile set changes.
 * Port of chop:lobby/load_missile_set + chop:lobby/missile_sets/*.
 */
public final class LobbyDisplayService {

    /** One stand model: structure key, origin, rotation. */
    private record Stand(String structure, int x, int y, int z, StructureRotation rotation) {
    }

    private record SetDisplay(String setName, List<String> standNames, List<Stand> stands) {
    }

    private static final SetDisplay ALL = new SetDisplay("All Missiles",
            List.of("All Missiles", "All Missiles", "All Missiles", "All Missiles", "All Missiles"),
            List.of());
    private static final SetDisplay CLASSIC = new SetDisplay("Classic Set",
            List.of("Tomahawk", "Guardian", "Juggernaut", "Lightning", "Shieldbuster"),
            List.of(new Stand("minecraft:shieldbuster", -74, 72, -10, StructureRotation.NONE),
                    new Stand("minecraft:lightning", -49, 72, -10, StructureRotation.NONE),
                    new Stand("minecraft:juggernaut", -30, 72, -9, StructureRotation.NONE),
                    new Stand("minecraft:tomahawk", 39, 72, -9, StructureRotation.NONE),
                    new Stand("minecraft:guardian", 63, 72, -9, StructureRotation.COUNTERCLOCKWISE_90)));
    private static final SetDisplay HONEY_CLASSIC = new SetDisplay("Classic Honey Set",
            List.of("Honey Tomahawk", "Honey Guardian", "Honey Juggernaut", "Honey Lightning", "Honey Shieldbuster"),
            List.of(new Stand("minecraft:honey_shieldbuster", -74, 72, -10, StructureRotation.NONE),
                    new Stand("minecraft:honey_lightning", -50, 72, -10, StructureRotation.NONE),
                    new Stand("minecraft:honey_juggernaut", -30, 72, -9, StructureRotation.NONE),
                    new Stand("minecraft:honey_tomahawk", 39, 72, -10, StructureRotation.NONE),
                    new Stand("minecraft:honey_guardian", 63, 72, -9, StructureRotation.COUNTERCLOCKWISE_90)));
    private static final SetDisplay EXPLOSIVE = new SetDisplay("Explosive Set",
            List.of("Clearer", "Bomber", "Dropper", "Flat Head", "Blocker"),
            List.of(new Stand("minecraft:blocker", -72, 72, -10, StructureRotation.NONE),
                    new Stand("minecraft:flat_head", -50, 72, -10, StructureRotation.NONE),
                    new Stand("minecraft:dropper", -31, 72, -7, StructureRotation.NONE),
                    new Stand("minecraft:clearer", 42, 72, -7, StructureRotation.NONE),
                    new Stand("minecraft:bomber", 63, 72, -9, StructureRotation.NONE)));
    private static final SetDisplay RUSH = new SetDisplay("Rush Set",
            List.of("Thunder Strike", "Bridge", "Tank", "Minibus", "Transport"),
            List.of(new Stand("minecraft:transport", -69, 72, -10, StructureRotation.NONE),
                    new Stand("minecraft:minibus", -49, 72, -11, StructureRotation.NONE),
                    new Stand("minecraft:tank", -31, 72, -8, StructureRotation.NONE),
                    new Stand("minecraft:thunder_strike", 40, 72, -9, StructureRotation.NONE),
                    new Stand("minecraft:bridge", 58, 72, -9, StructureRotation.NONE)));
    private static final SetDisplay SWITCH = new SetDisplay("Switch Set",
            List.of("Undertaker", "Scorpion", "Countdown", "Trebuchet", "Bulldozer"),
            List.of(new Stand("community:bulldozer", -71, 72, -10, StructureRotation.NONE),
                    new Stand("community:trebuchet", -50, 72, -9, StructureRotation.NONE),
                    new Stand("community:countdown", -32, 72, -8, StructureRotation.NONE),
                    new Stand("community:undertaker", 43, 72, -9, StructureRotation.NONE),
                    new Stand("community:scorpion", 72, 72, -13, StructureRotation.CLOCKWISE_90)));

    private final HoneyMissileWarsPlugin plugin;

    public LobbyDisplayService(HoneyMissileWarsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Refreshes the whole display for the game's current missile set. */
    public void refresh(Game game, MissileSet set) {
        World world = game.world();
        if (world == null) {
            return;
        }
        SetDisplay display = switch (set) {
            case ALL -> ALL;
            case CLASSIC -> CLASSIC;
            case HONEY_CLASSIC -> HONEY_CLASSIC;
            case EXPLOSIVE -> EXPLOSIVE;
            case RUSH -> RUSH;
            case SWITCH -> SWITCH;
        };

        // rename the floating name entities (tags 1..5) and the set label
        for (Entity entity : world.getEntities()) {
            var tags = entity.getScoreboardTags();
            if (tags.contains("missile_set")) {
                entity.customName(net.kyori.adventure.text.Component.text(display.setName(),
                        net.kyori.adventure.text.format.NamedTextColor.YELLOW,
                        net.kyori.adventure.text.format.TextDecoration.BOLD));
            } else if (tags.contains("1") || tags.contains("2") || tags.contains("3")
                    || tags.contains("4") || tags.contains("5")) {
                String name = "Missile";
                for (String tag : List.of("1", "2", "3", "4", "5")) {
                    if (tags.contains(tag)) {
                        int i = Integer.parseInt(tag) - 1;
                        if (i >= 0 && i < display.standNames().size()) {
                            name = display.standNames().get(i);
                        }
                    }
                }
                entity.customName(net.kyori.adventure.text.Component.text(name,
                        net.kyori.adventure.text.format.NamedTextColor.WHITE));
            }
        }

        // paste the five stand models (static display models - never activated)
        for (Stand stand : display.stands()) {
            this.plugin.structureService().paste(world, stand.structure(),
                    new Location(world, stand.x(), stand.y(), stand.z()), stand.rotation(), false, false);
        }

        refreshDisplayItems(world);
    }

    /** Kills and re-spawns the four floating item displays. */
    private void refreshDisplayItems(World world) {
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Item item && entity.getScoreboardTags().contains("missile_display")) {
                entity.remove();
            }
        }
        spawnDisplayItem(world, 20, 72, -4, "Shield", this.plugin.itemFactory().shieldItem());
        spawnDisplayItem(world, 23, 72, -4, "Fireball", this.plugin.itemFactory().fireballItem());
        spawnDisplayItem(world, 26, 72, -4, "Bow & Arrow", glintedBow());
        spawnDisplayItem(world, 29, 72, -4, "Grenade", this.plugin.itemFactory().grenadeItem());
    }

    private ItemStack glintedBow() {
        ItemStack stack = new ItemStack(Material.BOW);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setEnchantmentGlintOverride(true);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void spawnDisplayItem(World world, int x, int y, int z, String name, ItemStack stack) {
        Location loc = new Location(world, x + 0.5, y, z + 0.5);
        world.spawn(loc, Item.class, spawned -> {
            spawned.setItemStack(stack);
            spawned.setPickupDelay(-32767);
            spawned.setUnlimitedLifetime(true);
            spawned.setCustomNameVisible(true);
            spawned.customName(net.kyori.adventure.text.Component.text(name,
                    net.kyori.adventure.text.format.NamedTextColor.GREEN));
            spawned.addScoreboardTag("missile_display");
        });
    }

    /** Plays the option-change feedback (sound + plank particles). */
    public void playOptionFeedback(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 1.0f, 1.0f);
        player.spawnParticle(org.bukkit.Particle.BLOCK,
                player.getLocation().add(0, 1, 0), 10,
                0.3, 0.3, 0.3, 1.0,
                Material.OAK_PLANKS.createBlockData());
    }
}
