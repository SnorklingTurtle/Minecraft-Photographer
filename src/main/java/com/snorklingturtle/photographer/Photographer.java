package com.snorklingturtle.photographer;

import com.snorklingturtle.photographer.commands.CameraCommand;
import com.snorklingturtle.photographer.commands.CameraCommandTabCompleter;
import com.snorklingturtle.photographer.listeners.*;
import com.snorklingturtle.photographer.util.ByteArrayCompression;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
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
import java.util.zip.DataFormatException;

public class Photographer extends JavaPlugin {

    // Key used to tag the camera item in PersistentDataContainer
    public static NamespacedKey cameraItemKey;
    public static NamespacedKey recipeItemKey;

    public static final int MAP_SIZE = 128;

    public static double fieldOfView = 70;
    public static boolean hasAntiAliasing = true;
    public static boolean hasShading = true;
    public static boolean hasShadows = true;
    public static boolean hasDithering = true;

    public static final String CONFIG_KEY_RECIPE_ENABLED = "settings.camera.recipe.enabled";
    public static final String CONFIG_KEY_RENDER_DISTANCE = "settings.camera.renderDistance";
    public static final String CONFIG_KEY_RECIPE_SHAPE = "settings.camera.recipe.shape";
    public static final String CONFIG_KEY_SKIN_URL = "settings.camera.skinUrl";
    public static final String CONFIG_KEY_RECIPE_INGREDIENTS = "settings.camera.recipe.ingredients";
    public static final String CONFIG_KEY_CAPTURE_COOLDOWN = "settings.camera.cooldown";
    private static final String CONFIG_KEY_DITHERING = "settings.camera.properties.dithering";
    private static final String CONFIG_KEY_ANTIALIASING = "settings.camera.properties.antialiasing";
    private static final String CONFIG_KEY_SHADOWS = "settings.camera.properties.shadows";
    private static final String CONFIG_KEY_SHADING = "settings.camera.properties.shading";
    private static final String CONFIG_KEY_FIELD_OF_VIEW = "settings.camera.properties.fieldOfView";

    public static List<Integer> cachedMapIDs = new ArrayList<>();

    @Override
    public void onEnable() {
        cameraItemKey = new NamespacedKey(this, "camera_item");
        recipeItemKey = new NamespacedKey(this, "camera_recipe");

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerJoin(this), this);
        getServer().getPluginManager().registerEvents(new PrepareItemCraft(this), this);
        getServer().getPluginManager().registerEvents(new CameraClick(this), this);
        getServer().getPluginManager().registerEvents(new PhotoCopy(this), this);
        getServer().getPluginManager().registerEvents(new PhotoDestroy(this), this);

        // Commands
        PluginCommand cameraCommand = getCommand("camera");
        if (cameraCommand != null)
        {
            cameraCommand.setExecutor(new CameraCommand());
            cameraCommand.setTabCompleter(new CameraCommandTabCompleter());
        }

        // Config
        FileConfiguration config = getConfig();
        if (config.getBoolean(CONFIG_KEY_RECIPE_ENABLED))
        {
            CameraRecipe.addRecipe(this, recipeItemKey, config);
        }

        // Load color mapping from file
        ColorMapping.load(this);

        // Default camera properties
        fieldOfView = Math.toRadians(config.getInt(CONFIG_KEY_FIELD_OF_VIEW));
        hasAntiAliasing = config.getBoolean(CONFIG_KEY_ANTIALIASING);
        hasShading = config.getBoolean(CONFIG_KEY_SHADING);
        hasShadows = config.getBoolean(CONFIG_KEY_SHADOWS);
        hasDithering = config.getBoolean(CONFIG_KEY_DITHERING);

        // Prepare database
        Connection dbConnection = Storage.connect(this);
        Storage.createTable(this, dbConnection);
        Storage.createCleanUpTrigger(this, dbConnection);

        // Load renderers
        initializePhotos(dbConnection);

        Storage.disconnect(this, dbConnection);
    }

//    @Override
//    public void onDisable() {
//    }

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
                        if (!cachedMapIDs.contains(mapViewNew.getId())) {
                            cachedMapIDs.add(mapViewNew.getId());

                            byte[] pixels;
                            try {
                                pixels = ByteArrayCompression.decompress(mapDataSerialized);
                            } catch (DataFormatException ex) {
                                throw new RuntimeException(ex);
                            }

                            mapView.setLocked(true);
                            mapView.setTrackingPosition(false);
                            mapView.setUnlimitedTracking(false);

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

    @Override @NonNull
    public FileConfiguration getConfig() {
        return CameraConfig.getConfig(this);
    }
}
