package com.snorklingturtle.photographer.commands;

import com.snorklingturtle.photographer.util.CameraUtil;
import com.snorklingturtle.photographer.ColorMapping;
import com.snorklingturtle.photographer.Photographer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

public class CameraCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("camera")) return false;

        if (!(sender instanceof Player player)) {
            return true;
        }

        if (!player.hasPermission("camera.command")) {
            return false;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            player.sendMessage("§6=== Camera ===");
            player.sendMessage("§7Right-click while holding a camera in your hand to take a photo.");

            player.sendMessage("§e/camera fov <value> §7- Change field of view (default = 70).");
            player.sendMessage("§e/camera antialiasing §7- Toggle anti-aliasing.");
            player.sendMessage("§e/camera dithering §7- Toggle dithering.");
            player.sendMessage("§e/camera shading §7- Toggles shading of each side of a block.");
            player.sendMessage("§e/camera shadows §7- Toggles shadows depending on light level.");
            return true;
        }

        // Change camera settings
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!CameraUtil.isCamera(heldItem, Photographer.cameraItemKey)) {
            player.sendMessage("§cYou must hold a camera when changing its settings.");
            return true;
        } else {

            if (args.length == 1 && args[0].equalsIgnoreCase("antialiasing")) {
                CameraUtil.toggleAntiAliasing(heldItem);
                player.sendMessage("§6Anti-aliasing ".concat(CameraUtil.hasAntialiasing(heldItem) ? "enabled" : "disabled"));
                return true;
            }

            if (args.length == 2 && args[0].equalsIgnoreCase("fov")) {
                Integer fieldOfView;
                try {
                    fieldOfView = Integer.parseInt(args[1]);
                    if (fieldOfView < 30 || fieldOfView > 110) {
                        throw new NumberFormatException();
                    }
                }
                catch (NumberFormatException e)
                {
                    player.sendMessage("§cField of view must be between 30 and 110.");
                    return true;
                }
                CameraUtil.setFieldOfView(heldItem, fieldOfView);
                player.sendMessage("§6Field of view set to ".concat(String.valueOf(fieldOfView)));
                return true;
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("shading")) {
                CameraUtil.toggleShading(heldItem);
                player.sendMessage("§6Shading ".concat(CameraUtil.hasShading(heldItem) ? "enabled" : "disabled"));
                return true;
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("shadows")) {
                CameraUtil.toggleShadows(heldItem);
                player.sendMessage("§6Shadows ".concat(CameraUtil.hasShadows(heldItem) ? "enabled" : "disabled"));
                return true;
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("dithering")) {
                CameraUtil.toggleDithering(heldItem);
                player.sendMessage("§6Dithering ".concat(CameraUtil.hasDithering(heldItem) ? "enabled" : "disabled"));
                return true;
            }
        }

        // Add/save block color
        if (args.length == 5 && args[0].equalsIgnoreCase("setBlockColor")) {
            String colorR = args[2];
            if (parseIntOrNull(args[2]) == null)
            {
                player.sendMessage(String.format("§cCould not parse '%s' as a number between 0 and 255.", colorR));
                return true;
            }
            String colorG = args[3];
            if (parseIntOrNull(args[3]) == null)
            {
                player.sendMessage(String.format("§cCould not parse '%s' as a number between 0 and 255.", colorG));
                return true;
            }
            String colorB = args[4];
            if (parseIntOrNull(args[4]) == null)
            {
                player.sendMessage(String.format("§cCould not parse '%s' as a number between 0 and 255.", colorB));
                return true;
            }

            String materialName = args[1].toUpperCase();

            ColorMapping.saveColor(materialName, String.format("%s,%s,%s", colorR, colorG, colorB));

            player.sendMessage("§6Color mapping updated.");
            return true;
        }

        player.sendMessage("§cUnknown subcommand. Use §e/camera help§c.");
        return true;
    }

    private Integer parseIntOrNull(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
