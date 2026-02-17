package me.Chookoo.testPlugin.utils.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import me.Chookoo.testPlugin.Main;

public class RerollToken {

    public static ItemStack create() {

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Crystal Reroll", NamedTextColor.LIGHT_PURPLE));
        meta.lore(java.util.List.of(
                Component.text("Right click to reroll your ability", NamedTextColor.GRAY)
        ));

        // set custom NBT tag instead of custom_model_data
        meta.getPersistentDataContainer().set(
                Main.keyRerollToken,
                PersistentDataType.BYTE,
                (byte) 1
        );

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isToken(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        if (!item.hasItemMeta()) return false;
        if (!item.getItemMeta().getPersistentDataContainer().has(
                Main.keyRerollToken,
                PersistentDataType.BYTE
        )) return false;

        return item.getItemMeta().getPersistentDataContainer().get(
                Main.keyRerollToken,
                PersistentDataType.BYTE
        ) == (byte) 1;
    }
}
