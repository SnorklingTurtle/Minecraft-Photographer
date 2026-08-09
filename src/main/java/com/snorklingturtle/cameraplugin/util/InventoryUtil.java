package com.snorklingturtle.cameraplugin.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class InventoryUtil {

    public static void removePaperFromInventory(Player player, int amount)
    {
        // remove 1 paper from the player's inventory
        Map<Integer, ? extends ItemStack> paperHash = player.getInventory().all(Material.PAPER);
        for (ItemStack item : paperHash.values()) {
            item.setAmount(item.getAmount() - amount);
            break;
        }
    }
}
