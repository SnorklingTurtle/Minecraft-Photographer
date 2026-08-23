package com.snorklingturtle.photographer;

import com.snorklingturtle.photographer.commands.CameraCommand;
import com.snorklingturtle.photographer.commands.CameraCommandTabCompleter;
import com.snorklingturtle.photographer.listeners.*;
import com.snorklingturtle.photographer.util.ByteArrayCompression;
import com.snorklingturtle.photographer.util.RenderUtil;
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

import javax.naming.Name;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

public class Photographer extends JavaPlugin {

    private static Photographer instance;

    // Key used to tag the camera item in PersistentDataContainer
    public static NamespacedKey cameraItemKey;
    public static NamespacedKey recipeItemKey;
    public static NamespacedKey itemFrameColorKey;

    public static final int MAP_SIZE = 128;

    public static int fieldOfView = 70;
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

    public static NamespacedKey cameraSettingAntialiasingKey;
    public static NamespacedKey cameraSettingFieldOfViewKey;
    public static NamespacedKey cameraSettingShadingKey;
    public static NamespacedKey cameraSettingShadowsKey;
    public static NamespacedKey cameraSettingDitheringKey;

    public static List<Integer> cachedMapIDs = new ArrayList<>();

    @Override
    public void onEnable() {
        instance = this;

        cameraItemKey = new NamespacedKey(this, "camera_item");
        recipeItemKey = new NamespacedKey(this, "camera_recipe");
        itemFrameColorKey = new NamespacedKey(this, "item_frame_color");

        cameraSettingAntialiasingKey = new NamespacedKey(this, "camera_setting_antialiasing_key");
        cameraSettingFieldOfViewKey = new NamespacedKey(this, "camera_setting_fieldofview_key");
        cameraSettingShadingKey = new NamespacedKey(this, "camera_setting_shading_key");
        cameraSettingShadowsKey = new NamespacedKey(this, "camera_setting_shadows_key");
        cameraSettingDitheringKey = new NamespacedKey(this, "camera_setting_dithering_key");

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerJoin(this), this);
        getServer().getPluginManager().registerEvents(new PrepareItemCraft(this), this);
        getServer().getPluginManager().registerEvents(new CameraClick(this), this);
        getServer().getPluginManager().registerEvents(new PhotoCopy(this), this);
        getServer().getPluginManager().registerEvents(new PhotoDestroy(this), this);
        getServer().getPluginManager().registerEvents(new PhotoFrameClick(this), this);

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
        fieldOfView = config.getInt(CONFIG_KEY_FIELD_OF_VIEW);
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
                int frameColorBytes = mapsResultSet.getInt("frame_color");
                Color frameColor = frameColorBytes == 0 ? null : RenderUtil.toColor(frameColorBytes);
                byte[] mapDataSerialized = mapsResultSet.getBytes("data");

                MapView mapView = Bukkit.getMap(mapId);
                if (mapView == null)
                    continue;

                for (MapRenderer renderer : mapView.getRenderers())
                    mapView.removeRenderer(renderer);

                mapView.addRenderer(RenderUtil.photoRender(mapDataSerialized, frameColor, true));
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


    public static Photographer getInstance() {
        return instance;
    }
}
