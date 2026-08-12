package com.snorklingturtle.photographer.util;

import com.snorklingturtle.photographer.Photographer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.logging.Logger;

public class FileUtil {

    public static boolean copyResource(Photographer plugin, String resourceFilePath, String destinationFilePath) {
        File folder = plugin.getDataFolder();
        Logger log = plugin.getLogger();

        try (InputStream sourceStream = Photographer.class.getResourceAsStream(resourceFilePath)) {
            if (sourceStream == null)
            {
                log.severe("Couldn't copy " + resourceFilePath + " from resource directory");
                return false;
            }

            Files.copy(sourceStream, Paths.get(folder + destinationFilePath), StandardCopyOption.REPLACE_EXISTING);

            log.info("Created new " + resourceFilePath);

        } catch (IOException e) {
            log.severe("An error occurred copying the resource: " + resourceFilePath);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean fileExists(Photographer plugin, String destinationFilePath)
    {
        File folder = plugin.getDataFolder();
        return Files.exists(Paths.get(folder + destinationFilePath));
    }

    public static Properties getConfig(Photographer plugin, String sourceFile)
    {
        Properties properties = new Properties();
        String configFilename = plugin.getDataFolder() + sourceFile;

        try (FileInputStream colorMappingStream = new FileInputStream(configFilename)) {
            properties.load(colorMappingStream);
        }
        catch (IOException e)
        {
            plugin.getLogger().severe("Config is missing or could not be opened: " + sourceFile);
        }

        return properties;
    }
}
