package com.snorklingturtle.photographer.util;

import com.snorklingturtle.photographer.Photographer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;


public class CameraUtil {

    private CameraUtil() {}

    public static boolean isCamera(ItemStack item, NamespacedKey key) {
        return item != null &&
            item.hasItemMeta() &&
            item.getItemMeta() != null &&
            item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.INTEGER);
    }

    // --- Antialiasing ---

    public static void toggleAntiAliasing(ItemStack cameraItem) {
        toggleSettingBoolean(cameraItem, Photographer.cameraSettingAntialiasingKey, Photographer.hasAntiAliasing);
    }

    public static boolean hasAntialiasing(ItemStack cameraItem) {
        return getSettingBoolean(cameraItem, Photographer.cameraSettingAntialiasingKey, Photographer.hasAntiAliasing);
    }

    // --- Shading ---

    public static void toggleShading(ItemStack cameraItem) {
        toggleSettingBoolean(cameraItem, Photographer.cameraSettingShadingKey, Photographer.hasShading);
    }

    public static boolean hasShading(ItemStack cameraItem) {
        return getSettingBoolean(cameraItem, Photographer.cameraSettingShadingKey, Photographer.hasShading);
    }

    // --- Shadows ---

    public static void toggleShadows(ItemStack cameraItem) {
        toggleSettingBoolean(cameraItem, Photographer.cameraSettingShadowsKey, Photographer.hasShadows);
    }

    public static boolean hasShadows(ItemStack cameraItem) {
        return getSettingBoolean(cameraItem, Photographer.cameraSettingShadowsKey, Photographer.hasShadows);
    }

    // --- Dithering ---

    public static void toggleDithering(ItemStack cameraItem) {
        toggleSettingBoolean(cameraItem, Photographer.cameraSettingDitheringKey, Photographer.hasDithering);
    }

    public static boolean hasDithering(ItemStack cameraItem) {
        return getSettingBoolean(cameraItem, Photographer.cameraSettingDitheringKey, Photographer.hasDithering);
    }

    // --- Field of View ---

    public static void setFieldOfView(ItemStack cameraItem, int newValue) {
        setSettingInteger(cameraItem, Photographer.cameraSettingFieldOfViewKey, newValue);
    }

    public static int getFieldOfView(ItemStack cameraItem) {
        return getSettingInteger(cameraItem, Photographer.cameraSettingFieldOfViewKey, Photographer.fieldOfView);
    }

    // --- Set/get Integer ---

    private static void setSettingInteger(ItemStack cameraItem, NamespacedKey key, int newValue) {
        SkullMeta cameraMeta = (SkullMeta) cameraItem.getItemMeta();
        if(cameraMeta == null) return;

        PersistentDataContainer container = cameraMeta.getPersistentDataContainer();
        container.set(
                key,
                PersistentDataType.INTEGER,
                newValue
        );

        // SAVE!
        cameraItem.setItemMeta(cameraMeta);
    }

    private static int getSettingInteger(ItemStack cameraItem, NamespacedKey key, int fallbackValue) {
        SkullMeta cameraMeta = (SkullMeta) cameraItem.getItemMeta();
        if(cameraMeta == null) return fallbackValue; // Fallback from config

        PersistentDataContainer container = cameraMeta.getPersistentDataContainer();

        return container.getOrDefault(
                key,
                PersistentDataType.INTEGER,
                fallbackValue
        );
    }

    // --- Set/get Boolean ---

    private static void toggleSettingBoolean(ItemStack cameraItem, NamespacedKey key, boolean fallbackValue) {
        SkullMeta cameraMeta = (SkullMeta) cameraItem.getItemMeta();
        if(cameraMeta == null) return;

        boolean isEnabled = !getSettingBoolean(cameraItem, key, fallbackValue);

        PersistentDataContainer container = cameraMeta.getPersistentDataContainer();
        container.set(
                key,
                PersistentDataType.BOOLEAN,
                isEnabled
        );

        // SAVE!
        cameraItem.setItemMeta(cameraMeta);
    }

    private static boolean getSettingBoolean(ItemStack cameraItem, NamespacedKey key, boolean fallbackValue) {
        SkullMeta cameraMeta = (SkullMeta) cameraItem.getItemMeta();
        if(cameraMeta == null) return fallbackValue; // Fallback from config

        PersistentDataContainer container = cameraMeta.getPersistentDataContainer();

        return container.getOrDefault(
                key,
                PersistentDataType.BOOLEAN,
                fallbackValue
        );
    }
}
