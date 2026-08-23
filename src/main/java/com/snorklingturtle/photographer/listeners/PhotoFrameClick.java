package com.snorklingturtle.photographer.listeners;

import com.snorklingturtle.photographer.ColorPalette;
import com.snorklingturtle.photographer.Photographer;


import com.snorklingturtle.photographer.Storage;
import com.snorklingturtle.photographer.util.ByteArrayCompression;
import com.snorklingturtle.photographer.util.PhotoUtil;
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
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.awt.Color;
import java.util.zip.DataFormatException;

import static java.util.Map.entry;

public class PhotoFrameClick implements Listener {

    private final Photographer plugin;
    public PhotoFrameClick(Photographer plugin) {
        this.plugin = plugin;
    }

    // TODO: Take color for mapping instead
    public static final Map<Material, Color> DYES = Map.ofEntries(
            entry(Material.WHITE_DYE, new Color(249, 255, 254)),
            entry(Material.LIGHT_GRAY_DYE, new Color(157, 157, 151)),
            entry(Material.GRAY_DYE, new Color(71, 79, 82)),
            entry(Material.BLACK_DYE, new Color(29, 29, 33)),
            entry(Material.BROWN_DYE, new Color(131, 84, 50)),
            entry(Material.RED_DYE, new Color(176, 46, 38)),
            entry(Material.ORANGE_DYE, new Color(249, 128, 29)),
            entry(Material.YELLOW_DYE, new Color(254, 216, 61)),
            entry(Material.LIME_DYE, new Color(128, 199, 31)),
            entry(Material.GREEN_DYE, new Color(94, 124, 22)),
            entry(Material.CYAN_DYE, new Color(22, 156, 156)),
            entry(Material.LIGHT_BLUE_DYE, new Color(58, 179, 218)),
            entry(Material.BLUE_DYE, new Color(60, 68, 170)),
            entry(Material.PURPLE_DYE, new Color(137, 50, 184)),
            entry(Material.MAGENTA_DYE, new Color(199, 78, 189)),
            entry(Material.PINK_DYE, new Color(243, 139, 170))
    );

//    private byte[] mapDataSerialized;

    @EventHandler
    public void frameClicked(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        // Is player sneaking
        if (!player.isSneaking()) return;

        // Is the player holding a valid dye
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!DYES.containsKey(heldItem.getType())) return;

        ItemFrame frame = (ItemFrame) event.getRightClicked();
        ItemStack item = frame.getItem();
        if (!PhotoUtil.isPhoto(item)) return;

        Integer mapId = PhotoUtil.getMapId(item);
        if (mapId == null) return;

        MapView mapView = Bukkit.getMap(mapId);
        if (mapView == null) return;

//        mapView.setTrackingPosition(false);
//        for (MapRenderer renderer : mapView.getRenderers())
//            mapView.removeRenderer(renderer);

        // Get photo from database
        Connection dbConnection = Storage.connect(plugin);
        ResultSet mapsResultSet = Storage.getById(plugin, dbConnection, mapId);
        try {
            if (mapsResultSet.next()) {
                byte[] mapDataSerialized = mapsResultSet.getBytes("data");

                // Re-render with frame
                mapView.addRenderer(new MapRenderer() {
                    @Override
                    public void render(@NonNull MapView mapViewNew, @NonNull MapCanvas mapCanvas, @NonNull Player player) {
                        //if (!Photographer.cachedMapIDs.contains(mapViewNew.getId())) {
                        //Photographer.cachedMapIDs.add(mapViewNew.getId());

                        byte[] pixels;
                        try {
                            pixels = ByteArrayCompression.decompress(mapDataSerialized);
                        } catch (DataFormatException ex) {
                            throw new RuntimeException(ex);
                        }

                        mapView.setLocked(true);
                        mapView.setTrackingPosition(false);
                        mapView.setUnlimitedTracking(false);

                        for (int py = 0; py < Photographer.MAP_SIZE; py++) {
                            for (int px = 0; px < Photographer.MAP_SIZE; px++) {
                                byte colorByte = pixels[py * Photographer.MAP_SIZE + px];
                                mapCanvas.setPixelColor(px, py, ColorPalette.getColor(colorByte));
                            }
                        }

                        // Render frame
                        boolean isBlackDye = heldItem.getType() == Material.BLACK_DYE;
                        Color colorOuter = isBlackDye ? DYES.get(heldItem.getType()) : DYES.get(heldItem.getType()).darker();
                        Color colorInner = isBlackDye ? DYES.get(heldItem.getType()).brighter() : DYES.get(heldItem.getType());
                        drawFrame(mapCanvas,
                                colorOuter,
                                colorInner
                        );
                        //}
                    }
                });
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        Storage.disconnect(plugin, dbConnection);

        try {
            Color particleColor = DYES.get(heldItem.getType());
            Particle.DustOptions dustOptions = new Particle.DustOptions(org.bukkit.Color.fromRGB(particleColor.getRed(), particleColor.getGreen(), particleColor.getBlue()), 1.0F);

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

        player.playSound(frame.getLocation(), Sound.ITEM_DYE_USE, 1.0F, 1.0F);

        // Cancel default action
        event.setCancelled(true);
    }

    private static void drawFrame(MapCanvas canvas, Color colorOuter, Color colorInner) {
        byte frameColor = MapPalette.matchColor(colorOuter);
        byte innerColor = MapPalette.matchColor(colorInner);
        int thickness = 6;

        for (int i = 0; i < Photographer.MAP_SIZE; i++) {
            for (int t = 0; t < thickness; t++) {
                // Outer edge
                canvas.setPixel(i, t, frameColor);                  // top
                canvas.setPixel(i, Photographer.MAP_SIZE - 1 - t, frameColor);  // bottom
                canvas.setPixel(t, i, frameColor);                  // left
                canvas.setPixel(Photographer.MAP_SIZE - 1 - t, i, frameColor);  // right

                // Inner edge highlight
                if (i < Photographer.MAP_SIZE - thickness)
                {
                    canvas.setPixel(i, thickness + t, innerColor);                 // top inner
                    canvas.setPixel(i, Photographer.MAP_SIZE - 1 - thickness - t, innerColor); // bottom inner
                    canvas.setPixel(thickness + t, i, innerColor);                 // left inner
                    canvas.setPixel(Photographer.MAP_SIZE - 1 - thickness - t, i, innerColor); // right inner
                }
            }
        }
    }
}
