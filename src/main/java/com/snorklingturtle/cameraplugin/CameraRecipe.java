package com.snorklingturtle.cameraplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;

public class CameraRecipe {

    public static void addRecipe(CameraPlugin plugin, NamespacedKey recipeKey, FileConfiguration config) {
        ItemStack camera = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta cameraMeta = (SkullMeta) camera.getItemMeta();

        PlayerProfile customProfile = Bukkit.createPlayerProfile(UUID.randomUUID(), "");
        PlayerTextures customPlayerTextures = customProfile.getTextures();

        // Get texture from URL
        try {
            String skinUrl = config.getString("settings.camera.skinUrl");
            if (skinUrl != null && skinUrl.trim().startsWith("http"))
            {
                customPlayerTextures.setSkin(new URL(skinUrl.trim()));
                customProfile.setTextures(customPlayerTextures);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        if (cameraMeta != null)
        {
            // Apply camera skin to item by setting the custom profile as the owner (https://www.spigotmc.org/threads/help-needed-with-skull-skin-issue.621928/)
            cameraMeta.setOwnerProfile(customProfile);

            // Name displayed in-game
            cameraMeta.setDisplayName(ChatColor.WHITE + "Camera");
            cameraMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

            // Mark item as a camera - this way we can later check for its existence
            cameraMeta.getPersistentDataContainer().set(plugin.getCameraItemKey(), PersistentDataType.INTEGER, 1);
        }

        camera.setItemMeta(cameraMeta);

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, camera);

        ArrayList<String> shapeArr = (ArrayList<String>) config.get("settings.camera.recipe.shape");
        recipe.shape(shapeArr.toArray(new String[shapeArr.size()]));

        for (String ingredientKey : config.getConfigurationSection("settings.camera.recipe.ingredients").getKeys(false)) {
            recipe.setIngredient(ingredientKey.charAt(0), Material.valueOf((String) config.get("settings.camera.recipe.ingredients." + ingredientKey)));
        }

        Bukkit.addRecipe(recipe);
    }

}
