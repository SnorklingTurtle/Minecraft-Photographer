package com.snorklingturtle.cameraplugin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;


public class CameraItem {

    private CameraItem() {}

    public static boolean isCamera(ItemStack item, NamespacedKey key) {
        return item != null &&
            item.hasItemMeta() &&
            item.getItemMeta() != null &&
            item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.INTEGER);
    }

}
