package com.snorklingturtle.cameraplugin.util;

import com.snorklingturtle.cameraplugin.CameraPlugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

public class FileUtil {

    public static boolean copyResource(CameraPlugin plugin, String resourceFilePath, String destinationFilePath) {
        File folder = plugin.getDataFolder();
        Logger log = plugin.getLogger();

        try (InputStream sourceStream = CameraPlugin.class.getResourceAsStream(resourceFilePath)) {
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

    public static boolean fileExists(CameraPlugin plugin, String destinationFilePath)
    {
        File folder = plugin.getDataFolder();
        return Files.exists(Paths.get(folder + destinationFilePath));
    }
}
