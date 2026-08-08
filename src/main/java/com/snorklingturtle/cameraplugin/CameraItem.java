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

//    public static ItemStack build(NamespacedKey key) {
//        ItemStack item = new ItemStack(Material.SPYGLASS);
//        ItemMeta meta = item.getItemMeta();
//
//        meta.setDisplayName("§6§lCamera");
//        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
//
//        // Tag it so we can identify it later
//        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
//
//        item.setItemMeta(meta);
//        return item;
//    }

    public static boolean isCamera(ItemStack item, NamespacedKey key) {
        return item != null &&
            item.hasItemMeta() &&
            item.getItemMeta() != null &&
            item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.INTEGER);
    }

}
