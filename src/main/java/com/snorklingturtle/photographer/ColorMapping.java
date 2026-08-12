package com.snorklingturtle.photographer;

import org.bukkit.Material;
import com.snorklingturtle.photographer.util.FileUtil;

import java.awt.*;
import java.util.Properties;

public class ColorMapping {

    private static Properties colorMapping = new Properties();
    private static final String colorMappingResourceFile = "/color-mapping.config";
    private static final String colorMappingDestinationFile = "/color-mapping.config";
    private static final Color FALLBACK_COLOR = new Color(128, 128, 128);

    public static void load(Photographer plugin)
    {
        if (!FileUtil.fileExists(plugin, colorMappingDestinationFile))
            FileUtil.copyResource(plugin, colorMappingResourceFile, colorMappingDestinationFile);

        colorMapping = FileUtil.getConfig(plugin, colorMappingDestinationFile);
    }

    // Used when rendering the photo for the first time
    public static Color getBaseColor(Material material) {
        if (material == null) return FALLBACK_COLOR;

        String colorString = colorMapping.getProperty(material.name());
        if (colorString == null) return FALLBACK_COLOR;

        return getColorFromString(colorString);
    }

    private static Color getColorFromString(String colorString)
    {
        int[] colorIntArray = new int[3];
        String[] colorSplit = colorString.split(",");

        for (int i = 0; i < 3; i++)
        {
            String value = colorSplit[i];
            colorIntArray[i] = Integer.parseInt(value);
        }

        return new Color(colorIntArray[0], colorIntArray[1], colorIntArray[2]);
    }
}
