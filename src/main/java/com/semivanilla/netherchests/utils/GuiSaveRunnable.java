package com.semivanilla.netherchests.utils;

import com.semivanilla.netherchests.NetherChests;
import dev.triumphteam.gui.components.util.ItemNbt;
import dev.triumphteam.gui.guis.StorageGui;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;

public class GuiSaveRunnable extends BukkitRunnable {
    private boolean onlySaveIfChanged = false;
    private StorageGui gui;
    // private int itemsHash = 0;
    private Player player;
    private ItemStack[] lastItems = null; // TODO: hash isn't working :/
    public GuiSaveRunnable(boolean onlySaveIfChanged, StorageGui gui, Player player) {
        // System.out.println("Instantiated");
        this.onlySaveIfChanged = onlySaveIfChanged;
        this.gui = gui;
        this.player = player;
    }
    @Override
    public void run() {
        // System.out.println("running");
        if (player == null || !player.isOnline() || gui.getInventory().getViewers().isEmpty()) {
            // System.out.println("cancelled");
            cancel();
            return;
        }
        boolean changed = false;
        if (onlySaveIfChanged) {
            ItemStack[] items = Arrays.asList(gui.getInventory().getContents()).stream().map(item -> {
                if (item == null) return null;
                return ItemNbt.removeTag(item, BukkitSerialization.MF_GUI_KEY);
            }).toArray(ItemStack[]::new);
            // int newHash = Arrays.hashCode(items);
            // changed = newHash != itemsHash;
            if (lastItems != null && !Arrays.equals(lastItems, items)) {
                changed = true;
            }
            lastItems = items;
        }
        if (!onlySaveIfChanged || changed) {
            NetherChests.getInstance().getStorageProvider().save(player.getUniqueId(), gui.getInventory().getContents());
        }
    }
}
