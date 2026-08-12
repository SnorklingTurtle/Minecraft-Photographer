package com.snorklingturtle.photographer;

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
import java.util.*;

public class CameraRecipe {

    public static void addRecipe(Photographer plugin, NamespacedKey recipeKey, FileConfiguration config) {
        ItemStack camera = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta cameraMeta = (SkullMeta) camera.getItemMeta();

        PlayerProfile customProfile = Bukkit.createPlayerProfile(UUID.randomUUID(), "");
        PlayerTextures customPlayerTextures = customProfile.getTextures();

        // Get texture from URL
        try {
            String skinUrl = config.getString(Photographer.CONFIG_KEY_SKIN_URL);
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
            cameraMeta.getPersistentDataContainer().set(Photographer.cameraItemKey, PersistentDataType.INTEGER, 1);
        }

        camera.setItemMeta(cameraMeta);

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, camera);

        ArrayList<String> shapeArr = (ArrayList<String>) config.get(Photographer.CONFIG_KEY_RECIPE_SHAPE);
        recipe.shape(shapeArr.toArray(new String[0]));

        for (String ingredientKey : config.getConfigurationSection(Photographer.CONFIG_KEY_RECIPE_INGREDIENTS).getKeys(false)) {
            recipe.setIngredient(ingredientKey.charAt(0), Material.valueOf((String) config.get(Photographer.CONFIG_KEY_RECIPE_INGREDIENTS.concat(".").concat(ingredientKey))));
        }

        Bukkit.addRecipe(recipe);
    }

}
