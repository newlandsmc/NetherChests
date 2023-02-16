package com.semivanilla.netherchests.listener;

import com.semivanilla.netherchests.NetherChests;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class ClickListener implements Listener {
    @EventHandler
    public void onChestClick(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && event.getAction() == Action.RIGHT_CLICK_BLOCK && !event.getPlayer().isSneaking()) {
            if (event.getClickedBlock().getType() == Material.CHEST) {
                if (NetherChests.getInstance().isNetherChest(event.getClickedBlock())) {
                    event.setCancelled(true);
                    if (NetherChests.getInstance().getConfig().getBoolean("persist-chest-to-player", false)) {
                        NetherChests.getInstance().openNetherChest(event.getPlayer(), event.getPlayer().getUniqueId());
                    } else {
                        String owner = event.getClickedBlock().getMetadata("NetherChestOwner").get(0).asString();
                        UUID ownerUUID = UUID.fromString(owner);
                        NetherChests.getInstance().openNetherChest(event.getPlayer(), ownerUUID);
                    }
                }
            }
        }
    }
}
