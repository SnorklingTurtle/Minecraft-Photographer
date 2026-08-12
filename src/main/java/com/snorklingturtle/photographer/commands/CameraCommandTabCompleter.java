package com.snorklingturtle.photographer.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;

public class CameraCommandTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args[0].equalsIgnoreCase("fov"))
        {
            return Arrays.asList("30", "40", "50", "60", "70", "80", "90", "100", "110");
        }
        return Arrays.asList("help", "aa", "shading", "shadows", "dithering", "fov");
    }
}
