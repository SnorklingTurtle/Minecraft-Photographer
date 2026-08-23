package com.snorklingturtle.photographer.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class InventoryUtil {

    public static void removeItemFromInventory(Player player, Material material, int amount)
    {
        // remove 1 paper from the player's inventory
        Map<Integer, ? extends ItemStack> paperHash = player.getInventory().all(material);
        for (ItemStack item : paperHash.values()) {
            item.setAmount(item.getAmount() - amount);
            break;
        }
    }

    public static boolean hasItem(Player player, Material material) {
        return player.getInventory().contains(material);
    }
}
