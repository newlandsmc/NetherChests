package com.semivanilla.netherchests.listener;

import com.semivanilla.netherchests.NetherChests;
import com.semivanilla.netherchests.utils.Cooldown;
import dev.triumphteam.gui.guis.BaseGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.MetadataValue;

import java.util.List;
import java.util.UUID;

public class ClickListener implements Listener {
    @EventHandler
    public void onChestClick(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && event.getAction() == Action.RIGHT_CLICK_BLOCK && !event.getPlayer().isSneaking()) {
            if (event.getClickedBlock().getType() == Material.CHEST) {
                if (NetherChests.getInstance().isNetherChest(event.getClickedBlock())) {
                    event.setCancelled(true);
                    boolean cooldown = Cooldown.isOnCooldown("open", event.getPlayer().getUniqueId());
                    if (cooldown) {
                        String msg = NetherChests.getInstance().getConfig().getString("rate-limit.open.message", "<red>You are opening netherchests too quickly! Please wait a second.");
                        event.getPlayer().sendRichMessage(msg);
                        return;
                    }
                    Cooldown.addCooldown("open", event.getPlayer().getUniqueId(), NetherChests.getInstance().getConfig().getInt("rate-limit.open.cooldown", 4));
                    if (NetherChests.getInstance().getConfig().getBoolean("persist-chest-to-player", false)) {
                        NetherChests.getInstance().openNetherChest(event.getPlayer(), event.getPlayer().getUniqueId(), true);
                    } else {
                        List<MetadataValue> metadataValues = event.getClickedBlock().getMetadata("NetherChestOwner");
                        if (metadataValues.isEmpty()) {
                            event.getPlayer().sendMessage(ChatColor.RED + "This chest is not owned by anyone.");
                            return;
                        }
                        String owner = metadataValues.get(0).asString();
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(owner);
                        UUID ownerUUID = offlinePlayer.getUniqueId();
                        NetherChests.getInstance().openNetherChest(event.getPlayer(), ownerUUID, true);
                    }
                }
            }
        }
    }

    // on gui hotkey
    @EventHandler
    public void onHotKey(InventoryClickEvent event) {
        if (event.getClick().isKeyboardClick() && event.getInventory().getHolder() instanceof BaseGui) {
            // System.out.println("Hotkey event");
            // System.out.println((event.getCurrentItem() != null ? event.getCurrentItem().getType().name() : "null") + " | " + (event.getCursor() != null ? event.getCursor().getType().name() : "null"));
            event.setCancelled(true);
            event.getWhoClicked().sendMessage(ChatColor.RED + "You cannot use hotkeys in a NetherChest.");
            return;
        }
    }
}
