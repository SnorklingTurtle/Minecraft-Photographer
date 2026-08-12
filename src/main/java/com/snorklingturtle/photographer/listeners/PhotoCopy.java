package com.snorklingturtle.photographer.listeners;

import com.snorklingturtle.photographer.Photographer;
import com.snorklingturtle.photographer.Storage;
import com.snorklingturtle.photographer.util.PhotoUtil;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;

/*
* Keep count when players copies photos using the Cartography Table
* */

public class PhotoCopy implements Listener {

    private final Photographer plugin;
    public PhotoCopy(Photographer plugin) {
        this.plugin = plugin;
    }

    final int SLOT_OUT = 2;

    @EventHandler
    public void onPictureCopy(InventoryClickEvent e) {

        if (e.getClickedInventory() == null)
            return;

        if (e.getClickedInventory().getType() != InventoryType.CARTOGRAPHY)
            return;

        // Only check for clicks in the output slot
        if (e.getSlot() != SLOT_OUT)
            return;

        if (e.getCurrentItem() == null)
            return;

        ItemStack outItem = e.getClickedInventory().getItem(SLOT_OUT);

        if (outItem == null)
            return;

        // Is copy?
        if (outItem.getAmount() != 2)
            return;

        if (!PhotoUtil.isPhoto(outItem))
            return;

        Integer mapId = PhotoUtil.getMapId(outItem);
        if (mapId == null)
            return;

        HumanEntity player = e.getWhoClicked();
        if (!(player instanceof Player))
            return;

        long worldSeed = player.getWorld().getSeed();

        Connection connection = Storage.connect(plugin);
        Storage.updateCounter(plugin, connection, mapId, worldSeed, 1);
        Storage.disconnect(plugin, connection);
    }

}
