package io.spiritsakura;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;

import java.util.Locale;

/**
 * Every piece in the set. {@code model} is the file name under
 * assets/sakura_spirit/items/ in the resource pack. The base material decides the
 * vanilla behaviour (damage, bow/crossbow/shield mechanics, armor value...).
 */
public enum SakuraItem {
    SWORD("sword", "Spirit Blade", Kind.PLAIN, null, "NETHERITE_SWORD"),
    AXE("axe", "Spirit Axe", Kind.PLAIN, null, "NETHERITE_AXE"),
    PICKAXE("pickaxe", "Spirit Pickaxe", Kind.PLAIN, null, "NETHERITE_PICKAXE"),
    SHOVEL("shovel", "Spirit Shovel", Kind.PLAIN, null, "NETHERITE_SHOVEL"),
    HOE("hoe", "Spirit Hoe", Kind.PLAIN, null, "NETHERITE_HOE"),
    HAMMER("hammer", "Spirit Hammer", Kind.PLAIN, null, "MACE"),
    // 1.21.11+ has real spears; older servers fall back to a trident
    SPEAR("spear", "Spirit Spear", Kind.PLAIN, null, "NETHERITE_SPEAR", "TRIDENT"),
    STAFF("staff", "Spirit Staff", Kind.PLAIN, null, "BLAZE_ROD"),
    BOW("bow", "Spirit Bow", Kind.PLAIN, null, "BOW"),
    CROSSBOW("crossbow", "Spirit Crossbow", Kind.PLAIN, null, "CROSSBOW"),
    SHIELD("shield", "Spirit Shield", Kind.PLAIN, null, "SHIELD"),
    ROD("rod", "Spirit Rod", Kind.PLAIN, null, "FISHING_ROD"),
    QUIVER("quiver", "Spirit Quiver", Kind.PLAIN, null, "BUNDLE"),
    GRENADE("grenade", "Spirit Grenade", Kind.PLAIN, null, "SNOWBALL"),
    KEY("key", "Spirit Key", Kind.PLAIN, null, "TRIPWIRE_HOOK"),
    CHEST("chest", "Spirit Chest", Kind.PLAIN, null, "HEART_OF_THE_SEA"),

    // Cosmetic worn in the head slot (rendered with the model's "head" display transform)
    HELMET_3D("helmet", "Spirit Mask", Kind.COSMETIC_HEAD, EquipmentSlot.HEAD, "PAPER"),

    // Real armor using the custom worn texture (equipment asset "sakura")
    ARMOR_HELMET("helmet_icon", "Spirit Helmet", Kind.ARMOR, EquipmentSlot.HEAD, "NETHERITE_HELMET"),
    ARMOR_CHESTPLATE("chestplate_icon", "Spirit Chestplate", Kind.ARMOR, EquipmentSlot.CHEST, "NETHERITE_CHESTPLATE"),
    ARMOR_LEGGINGS("leggings_icon", "Spirit Leggings", Kind.ARMOR, EquipmentSlot.LEGS, "NETHERITE_LEGGINGS"),
    ARMOR_BOOTS("boots_icon", "Spirit Boots", Kind.ARMOR, EquipmentSlot.FEET, "NETHERITE_BOOTS");

    public static final String NAMESPACE = "sakura_spirit";

    private enum Kind { PLAIN, COSMETIC_HEAD, ARMOR }

    private final String model;
    private final String displayName;
    private final Kind kind;
    private final EquipmentSlot slot;
    private final String[] materials;

    SakuraItem(String model, String displayName, Kind kind, EquipmentSlot slot, String... materials) {
        this.model = model;
        this.displayName = displayName;
        this.kind = kind;
        this.slot = slot;
        this.materials = materials;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static SakuraItem byId(String id) {
        for (SakuraItem item : values()) {
            if (item.id().equalsIgnoreCase(id)) return item;
        }
        return null;
    }

    private Material resolveMaterial() {
        for (String candidate : materials) {
            Material m = Material.matchMaterial(candidate);
            if (m != null) return m;
        }
        return Material.PAPER;
    }

    public ItemStack create(String colorHex) {
        ItemStack stack = new ItemStack(resolveMaterial());
        ItemMeta meta = stack.getItemMeta();

        meta.setItemModel(new NamespacedKey(NAMESPACE, model));
        meta.itemName(MiniMessage.miniMessage().deserialize("<" + colorHex + ">" + displayName));

        if (kind != Kind.PLAIN) {
            EquippableComponent equippable = meta.getEquippable();
            equippable.setSlot(slot);
            if (kind == Kind.ARMOR) {
                equippable.setModel(new NamespacedKey(NAMESPACE, "sakura"));
            }
            meta.setEquippable(equippable);
        }

        stack.setItemMeta(meta);
        return stack;
    }
}
