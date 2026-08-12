package com.snorklingturtle.photographer.listeners;

import com.snorklingturtle.photographer.Photographer;
import com.snorklingturtle.photographer.Storage;
import com.snorklingturtle.photographer.util.PhotoUtil;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import java.sql.Connection;

import java.util.HashSet;

public class PhotoDestroy implements Listener {

    private final Photographer plugin;
    public PhotoDestroy(Photographer plugin) {
        this.plugin = plugin;
    }

    private static final HashSet<Item> damagedItems = new HashSet<>();

    @EventHandler
    public void onItemDespawnEvent(ItemDespawnEvent event) {
        ItemStack itemStack = event.getEntity().getItemStack();

        if (!PhotoUtil.isPhoto(itemStack))
            return;

        Integer mapId = PhotoUtil.getMapId(itemStack);
        if (mapId == null)
            return;

        long worldSeed = event.getEntity().getWorld().getSeed();

        Connection connection = Storage.connect(plugin);
        Storage.updateCounter(plugin, connection, mapId, worldSeed, -itemStack.getAmount());
        Storage.disconnect(plugin, connection);
    }

    @EventHandler
    public void onEntityDamageEvent(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof Item))
            return;

        Item itemEntity = (Item) event.getEntity();
        ItemStack itemStack = itemEntity.getItemStack();

        if (!PhotoUtil.isPhoto(itemStack))
            return;

        damagedItems.add(itemEntity);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!itemEntity.isDead() || !damagedItems.contains(itemEntity))
                    return;

                damagedItems.remove(itemEntity);

                Integer mapId = PhotoUtil.getMapId(itemStack);
                if (mapId == null)
                    return;

                long worldSeed = itemEntity.getWorld().getSeed();

                Connection connection = Storage.connect(plugin);
                Storage.updateCounter(plugin, connection, mapId, worldSeed, -itemStack.getAmount());
                Storage.disconnect(plugin, connection);
            }
        }.runTaskLater(plugin, 1);
    }
}
