package top.sparkpixel.hmw.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import top.sparkpixel.hmw.HoneyMissileWarsPlugin;
import top.sparkpixel.hmw.game.Team;
import top.sparkpixel.hmw.missile.MissileType;

import java.util.List;

/**
 * Builds every custom item of the game with the same stats as the datapack:
 * unbreakable bow (sharpness 9 / flame 1 / power 2), team-dyed leather armour,
 * the 3-second elytra, missile eggs and the four power items.
 */
public final class ItemFactory {

    public static final String KEY_MISSILE = "missile";
    public static final String KEY_SPECIAL = "special";

    private final HoneyMissileWarsPlugin plugin;
    private final NamespacedKey missileKey;
    private final NamespacedKey specialKey;

    public ItemFactory(HoneyMissileWarsPlugin plugin) {
        this.plugin = plugin;
        this.missileKey = new NamespacedKey(plugin, KEY_MISSILE);
        this.specialKey = new NamespacedKey(plugin, KEY_SPECIAL);
    }

    public NamespacedKey missileKey() {
        return this.missileKey;
    }

    public NamespacedKey specialKey() {
        return this.specialKey;
    }

    /** Reads the missile id stored on an item, or null. */
    public String missileId(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        return stack.getItemMeta().getPersistentDataContainer().get(this.missileKey, PersistentDataType.STRING);
    }

    /** Reads the special item type stored on an item, or null. */
    public String specialId(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        return stack.getItemMeta().getPersistentDataContainer().get(this.specialKey, PersistentDataType.STRING);
    }

    /** Missile spawn egg; the name is coloured for the receiving team. */
    public ItemStack missileEgg(MissileType type, Team team) {
        ItemStack stack = new ItemStack(type.eggMaterial());
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(type.displayName(), team.textColor(), TextDecoration.ITALIC));
        meta.lore(List.of(Component.text("Right-click to place", NamedTextColor.GRAY, TextDecoration.ITALIC)));
        meta.getPersistentDataContainer().set(this.missileKey, PersistentDataType.STRING, type.id());
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack fireballItem() {
        return special(Material.BLAZE_SPAWN_EGG, "Fireball", null);
    }

    public ItemStack grenadeItem() {
        return special(Material.EGG, "Grenade", "Explodes in 1 second!");
    }

    public ItemStack shieldItem() {
        return special(Material.SNOWBALL, "Shield", null);
    }

    private ItemStack special(Material material, String name, String lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW, TextDecoration.ITALIC));
        if (lore != null) {
            meta.lore(List.of(Component.text(lore, NamedTextColor.GRAY, TextDecoration.ITALIC)));
        }
        meta.getPersistentDataContainer().set(this.specialKey, PersistentDataType.STRING, name.toUpperCase());
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack arrowStack(int amount) {
        return new ItemStack(Material.ARROW, amount);
    }

    /** Unbreakable bow: sharpness 9, flame 1, power 2 (chop:game/get_bow). */
    public ItemStack bow() {
        ItemStack stack = new ItemStack(Material.BOW);
        ItemMeta meta = stack.getItemMeta();
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.addEnchant(Enchantment.SHARPNESS, 9, true);
        meta.addEnchant(Enchantment.FLAME, 1, true);
        meta.addEnchant(Enchantment.POWER, 2, true);
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack pickaxe() {
        ItemStack stack = new ItemStack(Material.IRON_PICKAXE);
        ItemMeta meta = stack.getItemMeta();
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        stack.setItemMeta(meta);
        return stack;
    }

    /** Team-dyed unbreakable leather armour piece (chop:game/get_armor). */
    public ItemStack armorPiece(Material material, Team team) {
        ItemStack stack = new ItemStack(material);
        ItemMeta raw = stack.getItemMeta();
        if (raw instanceof LeatherArmorMeta meta) {
            meta.setColor(team.leatherColor());
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            stack.setItemMeta(raw);
        }
        return stack;
    }

    /** 3-second flight elytra (damage 429 of 432 + binding curse). */
    public ItemStack elytra() {
        ItemStack stack = new ItemStack(Material.ELYTRA);
        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(429);
        }
        meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        stack.setItemMeta(meta);
        return stack;
    }
}
