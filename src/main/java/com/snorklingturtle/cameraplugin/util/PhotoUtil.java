package com.snorklingturtle.cameraplugin.util;

import com.snorklingturtle.cameraplugin.CameraPlugin;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

public class PhotoUtil {
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isPhoto(ItemStack itemStack)
    {
        if (itemStack.getType() != Material.FILLED_MAP)
            return false;

        Integer mapId = PhotoUtil.getMapId(itemStack);
        if (mapId == null)
            return false;

        return CameraPlugin.cachedMapIDs.contains(mapId);
    }

    public static Integer getMapId(ItemStack item)
    {
        MapMeta mapMeta = (MapMeta) item.getItemMeta();
        if (mapMeta == null || mapMeta.getMapView() == null)
            return null;

        return mapMeta.getMapView().getId();
    }
}
