package com.semivanilla.netherchests.listener;

import com.semivanilla.netherchests.NetherChests;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.StorageGui;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ClickListener implements Listener {
    @EventHandler
    public void onChestClick(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && event.getAction() == Action.RIGHT_CLICK_BLOCK && !event.getPlayer().isSneaking()) {
            if (event.getClickedBlock().getType() == Material.CHEST) {
                if (NetherChests.getInstance().isNetherChest(event.getClickedBlock())) {
                    event.setCancelled(true);
                    ItemStack[] items = NetherChests.getInstance().getStorageProvider().load(event.getPlayer().getUniqueId());
                    StorageGui gui = Gui.storage()
                            .title(NetherChests.getMiniMessage().deserialize(NetherChests.getInstance().getConfig().getString("name", "<dark_red>Nether Chest")))
                            .rows(NetherChests.getInstance().getConfig().getInt("rows", 3))
                            .create();
                    for (int i = 0; i < items.length; i++) {
                        if (items[i] != null) {
                            gui.setItem(i, new GuiItem(items[i]));
                        }
                    }
                    gui.open(event.getPlayer());
                    gui.setCloseGuiAction((e) -> NetherChests.getInstance().getStorageProvider().save(event.getPlayer().getUniqueId(),
                            e.getInventory().getContents()));
                    gui.setDefaultTopClickAction((e) -> {
                        if (NetherChests.getInstance().isUpdateOnTransaction()) {
                            if (e.getCurrentItem() == null && (e.getAction() != InventoryAction.PLACE_SOME && e.getAction() != InventoryAction.PLACE_ALL && e.getAction() != InventoryAction.PLACE_ONE)) {
                                return;
                            }
                            NetherChests.getInstance().getStorageProvider().save(event.getPlayer().getUniqueId(),
                                    e.getInventory().getContents());
                        }
                    });
                }
            }
        }
    }
}
