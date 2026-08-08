package com.snorklingturtle.cameraplugin.listeners;

import com.snorklingturtle.cameraplugin.CameraItem;
import com.snorklingturtle.cameraplugin.CameraPlugin;
import com.snorklingturtle.cameraplugin.CameraRenderer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CameraClick implements Listener {

    private final CameraPlugin plugin;
    public CameraClick(CameraPlugin plugin) {
        this.plugin = plugin;
    }

    // Cooldown set — prevents double-firing (Spigot fires the event twice for some clicks)
    private final Set<UUID> cooldown = new HashSet<>();

    @EventHandler
    public void cameraClicked(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!CameraItem.isCamera(held, CameraPlugin.cameraItemKey)) return;

        event.setCancelled(true);

        Action action = event.getAction();

        // Only care about right clicks (air or block)
        if (action != Action.RIGHT_CLICK_AIR) return;

        boolean usePaper = player.hasPermission("camera.usepaper");
        if (usePaper && !player.getInventory().contains(Material.PAPER)) {
            player.sendMessage("§cYou must have paper in your inventory.");
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage("§cYou cannot take a photo with a full inventory.");
            return;
        }

        // Simple per-player cooldown to avoid duplicate events
        UUID id = player.getUniqueId();
        if (cooldown.contains(id)) return;
        cooldown.add(id);

        int captureCooldown = plugin.getConfig().getInt(CameraPlugin.CONFIG_KEY_CAPTURE_COOLDOWN);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> cooldown.remove(id), captureCooldown);

        if (!player.hasPermission("camera.use")) {
            player.sendMessage("§cYou don't have permission to use the camera.");
            return;
        }

        player.playSound(player.getLocation(), Sound.BLOCK_PISTON_EXTEND, 0.5F, 2.0F);

        // Run the heavy raytrace work asynchronously, then render on main thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            CameraRenderer.capture(player, plugin);
        });
    }
}
