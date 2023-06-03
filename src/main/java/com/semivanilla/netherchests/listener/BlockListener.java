package com.semivanilla.netherchests.listener;

import com.semivanilla.netherchests.NetherChests;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class BlockListener implements Listener {
    public static boolean isNetherChestSign(String[] lines) {
        for (String line : lines) {
            line = ChatColor.stripColor(line);
            if (line.equalsIgnoreCase("[netherchest]") || line.equalsIgnoreCase("[nether chest]")) return true;
        }
        return false;
    }

    public static Block getConnectedChest(Sign sign) {
        BlockFace face = null;
        BlockData data = sign.getBlockData();
        if (data instanceof WallSign) {
            face = ((WallSign) data).getFacing().getOppositeFace();
        }
        if (face == null) {
            return null;
        }
        Block chest = sign.getBlock().getRelative(face);
        if (chest.getType() == Material.CHEST)
            return chest;
        return null;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType().name().endsWith("_SIGN")) {
            Sign sign = (Sign) event.getBlock().getState();
            if (isNetherChestSign(sign.getLines())) {
                Block chest = getConnectedChest(sign);
                if (chest != null) {
                    chest.removeMetadata("NetherChest", NetherChests.getInstance());
                }
            }
        }
    }

    @EventHandler
    public void onPlace(SignChangeEvent event) {
        if (event.getBlock().getType().name().endsWith("_SIGN")) {
            Block signBlock = event.getBlock();
            Sign sign = (Sign) signBlock.getState();
            if (isNetherChestSign(event.getLines())) {
                Block chest = getConnectedChest(sign);
                if (chest != null) {
                    System.out.println("NetherChest found!");
                    chest.setMetadata("NetherChest", new FixedMetadataValue(NetherChests.getInstance(), true));
                    String rawOwner = event.getPlayer().getName();
                    chest.setMetadata("NetherChestOwner", new FixedMetadataValue(NetherChests.getInstance(), rawOwner));
                    Bukkit.getServer().getScheduler().runTaskLater(NetherChests.getInstance(), ()-> {
                        Component signName = NetherChests.getMiniMessage().deserialize(NetherChests.getInstance().getConfig().getString("sign-name", "<bold><dark_gray>[<dark_red>Nether Chest<dark_gray>]"));
                        Component nameLine = NetherChests.getMiniMessage().deserialize(NetherChests.getInstance().getConfig().getString("player-name-format", "<white>%name%").replace("%name%", event.getPlayer().getName()));
                        sign.setLine(0, "");
                        sign.line(1, signName);
                        sign.line(2, nameLine);
                        sign.setLine(3, "");
                        sign.update();
                    }, 10);
                    return;
                }
                //System.out.println("Could not find connected chest for NetherChest sign");
            }
        }
    }
}
