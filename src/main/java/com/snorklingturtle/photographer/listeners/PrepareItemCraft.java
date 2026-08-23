package com.snorklingturtle.photographer.listeners;

import com.snorklingturtle.photographer.util.CameraUtil;
import com.snorklingturtle.photographer.Photographer;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class PrepareItemCraft implements Listener {

    private final Photographer plugin;
    public PrepareItemCraft(Photographer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void prepareItemCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null) return;

        if (CameraUtil.isCamera(recipe.getResult(), Photographer.cameraItemKey)) {
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
