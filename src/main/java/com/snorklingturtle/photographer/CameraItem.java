package com.snorklingturtle.photographer;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;


public class CameraItem {

    private CameraItem() {}

    public static boolean isCamera(ItemStack item, NamespacedKey key) {
        return item != null &&
            item.hasItemMeta() &&
            item.getItemMeta() != null &&
            item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.INTEGER);
    }

}
