package com.snorklingturtle.photographer.listeners;

import com.snorklingturtle.photographer.ColorMapping;
import com.snorklingturtle.photographer.Photographer;


import com.snorklingturtle.photographer.Storage;
import com.snorklingturtle.photographer.util.InventoryUtil;
import com.snorklingturtle.photographer.util.PhotoUtil;
import com.snorklingturtle.photographer.util.RenderUtil;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.sql.Connection;
import java.sql.ResultSet;
import java.awt.Color;


public class PhotoFrameClick implements Listener {

    private final Photographer plugin;
    public PhotoFrameClick(Photographer plugin) { this.plugin = plugin; }

    @EventHandler
    public void frameClicked(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        // Is the player holding a valid dye
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!heldItem.getType().toString().toLowerCase().endsWith("_dye")) return;

        Color dyeColor = ColorMapping.getBaseColor(heldItem.getType());
        if (dyeColor == null) return;

        ItemFrame frame = (ItemFrame) event.getRightClicked();
        ItemStack item = frame.getItem();
        if (!PhotoUtil.isPhoto(item)) return;

        Integer mapId = PhotoUtil.getMapId(item);
        if (mapId == null) return;

        MapView mapView = Bukkit.getMap(mapId);
        if (mapView == null) return;

        for (MapRenderer renderer : mapView.getRenderers())
            mapView.removeRenderer(renderer);

        try {
            // Get photo from database
            Connection connection = Storage.connect(plugin);
            ResultSet mapsResultSet = Storage.getById(plugin, connection, mapId);

            if (mapsResultSet.next()) {
                byte[] mapDataSerialized = mapsResultSet.getBytes("data");

                // Re-render with frame
                MapRenderer renderer = RenderUtil.photoRender(mapDataSerialized, dyeColor, false);
                mapView.addRenderer(renderer);
            }

            Storage.disconnect(plugin, connection);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Save frame color
        try {
            Connection connection = Storage.connect(plugin);
            Storage.updateFrameColor(plugin, connection, mapId, RenderUtil.toBytes(dyeColor));
            Storage.disconnect(plugin, connection);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Spawn particles
        try {
            Particle.DustOptions dustOptions = new Particle.DustOptions(
                org.bukkit.Color.fromRGB(dyeColor.getRed(), dyeColor.getGreen(), dyeColor.getBlue()),
                1.0F
            );

            int amount = 25;
            for (int i = 0; i < amount; i++)
            {
                player.getWorld().spawnParticle(
                    Particle.DUST,
                    frame.getLocation(),
                    1,
                    Math.random() * .5,
                    Math.random() * .5,
                    Math.random() * .5,
                    // Speed
                    10.0F,
                    dustOptions
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Play sound
        player.playSound(frame.getLocation(), Sound.ITEM_DYE_USE, 1.0F, 1.0F);

        // Consume dye
        if (player.hasPermission("camera.consumedye")) {
            InventoryUtil.removeItemFromInventory(player, heldItem.getType(), 1);
        }

        // Cancel default action
        event.setCancelled(true);
    }
}
