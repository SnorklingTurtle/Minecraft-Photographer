package com.snorklingturtle.photographer;

import com.snorklingturtle.photographer.util.FileUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;


public class CameraConfig {

    private static FileConfiguration config;

    public static void load(Photographer plugin) {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        String filePath = "/config.yml";
        boolean hasConfig = FileUtil.fileExists(plugin, filePath);

        if (!hasConfig)
        {
            try {
                FileUtil.copyResource(plugin, filePath, filePath);
            }
            catch (Exception e)
            {
                plugin.getLogger().warning("Failed to create config.yml");
                e.printStackTrace();
            }
        }

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public static FileConfiguration getConfig(Photographer plugin) {
        if (config == null)
        {
            load(plugin);
        }
        return config;
    }
}
