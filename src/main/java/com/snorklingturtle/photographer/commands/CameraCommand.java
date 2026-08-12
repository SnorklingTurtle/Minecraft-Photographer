package com.snorklingturtle.photographer.commands;

import com.snorklingturtle.photographer.Photographer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
            player.sendMessage("§e/camera aa §7- Toggle anti-aliasing.");
            player.sendMessage("§e/camera dithering §7- Toggle dithering.");
            player.sendMessage("§e/camera shading §7- Toggles shading of each side of a block.");
            player.sendMessage("§e/camera shadows §7- Toggles shadows depending on light level.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("aa")) {
            Photographer.hasAntiAliasing = !Photographer.hasAntiAliasing;
            player.sendMessage("§6Anti-aliasing ".concat(Photographer.hasAntiAliasing ? "enabled" : "disabled"));
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("fov")) {
            Photographer.fieldOfView = Math.toRadians(Double.parseDouble(args[1]));
            player.sendMessage("§6Field of view set to ".concat(args[1]));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("shading")) {
            Photographer.hasShading = !Photographer.hasShading;
            player.sendMessage("§6Shading ".concat(Photographer.hasShading ? "enabled" : "disabled"));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("shadows")) {
            Photographer.hasShadows = !Photographer.hasShadows;
            player.sendMessage("§6Shadows ".concat(Photographer.hasShadows ? "enabled" : "disabled"));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("dithering")) {
            Photographer.hasDithering = !Photographer.hasDithering;
            player.sendMessage("§6Dithering ".concat(Photographer.hasDithering ? "enabled" : "disabled"));
            return true;
        }

        player.sendMessage("§cUnknown subcommand. Use §e/camera help§c.");
        return true;
    }
}
