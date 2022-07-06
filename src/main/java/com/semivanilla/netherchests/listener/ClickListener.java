package com.semivanilla.netherchests.listener;

import com.semivanilla.netherchests.NetherChests;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ClickListener implements Listener {
    @EventHandler
    public void onChestClick(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && event.getAction() == Action.RIGHT_CLICK_BLOCK && !event.getPlayer().isSneaking()) {
            if (event.getClickedBlock().getType() == Material.CHEST) {
                if (NetherChests.getInstance().isNetherChest(event.getClickedBlock())) {
                    event.setCancelled(true);
                    NetherChests.getInstance().openNetherChest(event.getPlayer(), event.getPlayer().getUniqueId());
                }
            }
        }
    }
}
