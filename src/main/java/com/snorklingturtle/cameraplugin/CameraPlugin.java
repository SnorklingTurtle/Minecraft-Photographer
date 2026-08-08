package com.snorklingturtle.cameraplugin;

import com.snorklingturtle.cameraplugin.listeners.CameraClick;
import com.snorklingturtle.cameraplugin.listeners.PlayerJoin;
import com.snorklingturtle.cameraplugin.listeners.PrepareItemCraft;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CameraPlugin extends JavaPlugin {

    // Key used to tag the camera item in PersistentDataContainer
    private NamespacedKey cameraItemKey;
    private NamespacedKey recipeItemKey;

    List<Integer> cachedMapIDs = new ArrayList<>();

    @Override
    public void onEnable() {
        cameraItemKey = new NamespacedKey(this, "camera_item");
        recipeItemKey = new NamespacedKey(this, "camera_recipe");

        getLogger().info("Camera enabled!");

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerJoin(this), this);
        getServer().getPluginManager().registerEvents(new PrepareItemCraft(this), this);
        getServer().getPluginManager().registerEvents(new CameraClick(this), this);


        FileConfiguration config = getConfig();
        //if (config.getBoolean("settings.camera.recipe.enabled"))
        {
            CameraRecipe.addRecipe(this, recipeItemKey, config);
        }

        // Prepare database
        Connection dbConnection = Storage.connect(this);
        Storage.createTable(this, dbConnection);
        Storage.createCleanUpTrigger(this, dbConnection);

        // Load renderers
        initializePhotos(dbConnection);

        Storage.disconnect(this, dbConnection);
    }

    private void initializePhotos(Connection dbConnection) {
        ResultSet mapsResultSet = Storage.getAll(this, dbConnection);
        try {
            while (mapsResultSet.next())
            {
                int mapId = mapsResultSet.getInt("map_id");
                byte[] mapDataSerialized = mapsResultSet.getBytes("data");

                MapView mapView = Bukkit.getMap(mapId);
                if (mapView == null)
                    continue;

                mapView.setTrackingPosition(false);
                for (MapRenderer renderer : mapView.getRenderers())
                    mapView.removeRenderer(renderer);

                mapView.addRenderer(new MapRenderer() {
                    @Override
                    public void render(@NonNull MapView mapViewNew, @NonNull MapCanvas mapCanvas, @NonNull Player player) {
                        if (!cachedMapIDs.contains(mapId)) {
                            cachedMapIDs.add(mapId);
                            byte[] pixels = Storage.deserializeByteArray2d(mapDataSerialized);

                            mapView.setLocked(true);
                            mapView.setTrackingPosition(false);
                            mapView.setUnlimitedTracking(false);

                            int MAP_SIZE = 128;

                            for (int py = 0; py < MAP_SIZE; py++) {
                                for (int px = 0; px < MAP_SIZE; px++) {
                                    byte colorByte = pixels[py * MAP_SIZE + px];
                                    mapCanvas.setPixelColor(px, py, ColorPalette.getColor(colorByte));
                                }
                            }
                        }
                    }
                });
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Camera disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("camera")) return false;

        if (!(sender instanceof Player player)) {
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            player.sendMessage("§6=== Camera ===");
            player.sendMessage("§7Right-click while holding a camera in your hand to take a photo.");

            player.sendMessage("§e/camera aa §7- Toggle anti-aliasing.");
            player.sendMessage("§e/camera dither §7- Toggle dithering.");
            player.sendMessage("§e/camera shade §7- Toggles shading of each side of a block.");
            player.sendMessage("§e/camera shadow §7- Toggles shadows depending on light level. ");
            player.sendMessage("§e/camera frame §7- Toggle framing of photo.");
            return true;
        }

        player.sendMessage("§cUnknown subcommand. Use §e/camera help§c.");
        return true;
    }

    public NamespacedKey getCameraItemKey() {
        return cameraItemKey;
    }

    @Override
    public FileConfiguration getConfig() {
        return CameraConfig.getConfig(this);
    }
}
