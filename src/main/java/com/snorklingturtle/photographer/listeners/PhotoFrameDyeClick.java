package com.snorklingturtle.photographer.listeners;

import com.snorklingturtle.photographer.ColorMapping;
import com.snorklingturtle.photographer.Photographer;


import com.snorklingturtle.photographer.Storage;
import com.snorklingturtle.photographer.util.InventoryUtil;
import com.snorklingturtle.photographer.util.PhotoUtil;
import com.snorklingturtle.photographer.util.RenderUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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


public class PhotoFrameDyeClick implements Listener {

    private final Photographer plugin;
    public PhotoFrameDyeClick(Photographer plugin) { this.plugin = plugin; }

    private enum Tool {
        SHEAR,
        DYE
    }

    @EventHandler
    public void frameClicked(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        ItemStack heldItem = player.getInventory().getItemInMainHand();

        // Is the player holding either dye or shears?
        if (!heldItem.getType().toString().toLowerCase().endsWith("_dye") && heldItem.getType() != Material.SHEARS) return;

        Tool tool = heldItem.getType() == Material.SHEARS ? Tool.SHEAR : Tool.DYE;

        Color dyeColor = tool == Tool.SHEAR ? null : ColorMapping.getBaseColor(heldItem.getType());

        // Return if tool is a dye, but the dye color is null
        if (tool == Tool.DYE && dyeColor == null) return;

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
            Storage.updateFrameColor(plugin, connection, mapId, tool == Tool.SHEAR ? 0 : RenderUtil.toBytes(dyeColor));
            Storage.disconnect(plugin, connection);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (tool == Tool.DYE) {
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

            // Use dye sound
            player.playSound(frame.getLocation(), Sound.ITEM_DYE_USE, 1.0F, 1.0F);

            // Consume dye
            if (player.hasPermission("camera.consumedye")) {
                InventoryUtil.removeItemFromInventory(player, heldItem.getType(), 1);
            }
        }
        else {
            // Remove frame sound
            player.playSound(frame.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1.0F, 1.0F);
        }

        // Cancel default action
        event.setCancelled(true);
    }
}
