package com.snorklingturtle.cameraplugin.listeners;

import com.snorklingturtle.cameraplugin.CameraPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoin implements Listener {

    private final CameraPlugin plugin;
    public PlayerJoin(CameraPlugin plugin) {
        this.plugin = plugin;
    }

    /* Add recipe to new players */
    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        if (plugin.getConfig().getBoolean(CameraPlugin.CONFIG_KEY_RECIPE_ENABLED))
            event.getPlayer().discoverRecipe(CameraPlugin.recipeItemKey);
    }

}
