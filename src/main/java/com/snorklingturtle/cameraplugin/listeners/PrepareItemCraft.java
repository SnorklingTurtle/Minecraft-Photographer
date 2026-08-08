package com.snorklingturtle.cameraplugin.listeners;

import com.snorklingturtle.cameraplugin.CameraItem;
import com.snorklingturtle.cameraplugin.CameraPlugin;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class PrepareItemCraft implements Listener {

    private final CameraPlugin plugin;
    public PrepareItemCraft(CameraPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void prepareItemCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null) return;

        if (CameraItem.isCamera(recipe.getResult(), plugin.getCameraItemKey())) {
            for (HumanEntity he : event.getViewers()) {
                if (he instanceof Player) {
                    if (!he.hasPermission("camera.craft")) {
                        event.getInventory().setResult(new ItemStack(Material.AIR));
                    }
                }
            }
        }
    }
}
