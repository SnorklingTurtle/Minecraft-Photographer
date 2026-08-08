package com.snorklingturtle.cameraplugin;

import com.snorklingturtle.cameraplugin.listeners.CameraClick;
import com.snorklingturtle.cameraplugin.listeners.PlayerJoin;
import com.snorklingturtle.cameraplugin.listeners.PrepareItemCraft;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CameraPlugin extends JavaPlugin {

    // Key used to tag the camera item in PersistentDataContainer
    private NamespacedKey cameraItemKey;
    private NamespacedKey recipeItemKey;

    @Override
    public void onEnable() {
        cameraItemKey = new NamespacedKey(this, "camera_item");
        recipeItemKey = new NamespacedKey(this, "camera_recipe");

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerJoin(this), this);
        getServer().getPluginManager().registerEvents(new PrepareItemCraft(this), this);
        getServer().getPluginManager().registerEvents(new CameraClick(this), this);

        getLogger().info("Camera enabled!");

        FileConfiguration config = getConfig();
        //if (config.getBoolean("settings.camera.recipe.enabled"))
        {
            CameraRecipe.addRecipe(this, recipeItemKey, config);
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
