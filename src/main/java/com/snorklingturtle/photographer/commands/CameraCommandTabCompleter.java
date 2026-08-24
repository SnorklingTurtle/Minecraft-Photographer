package com.snorklingturtle.photographer.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CameraCommandTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args[0].equalsIgnoreCase("fov"))
        {
            return Arrays.asList("30", "40", "50", "60", "70", "80", "90", "100", "110");
        }

        List<String> commands = new ArrayList<>(Arrays.asList("help", "antialiasing", "shading", "shadows", "dithering", "fov"));

        if (sender instanceof Player player) {
            if (player.hasPermission("camera.admincommand")) {
                commands.add("set_color");
            }
        }
        return commands;
    }
}
