package com.snorklingturtle.photographer.listeners;

import com.snorklingturtle.photographer.Photographer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoin implements Listener {

    private final Photographer plugin;
    public PlayerJoin(Photographer plugin) {
        this.plugin = plugin;
    }

    /* Add recipe to new players */
    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        if (plugin.getConfig().getBoolean(Photographer.CONFIG_KEY_RECIPE_ENABLED))
            event.getPlayer().discoverRecipe(Photographer.recipeItemKey);
    }

}
