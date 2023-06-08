package com.semivanilla.netherchests.utils;

import dev.triumphteam.gui.components.util.ItemNbt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class BukkitSerialization {
    private static final String MF_GUI_KEY = "mf-gui"; // TriumphGUI adds this nbt tag to the items, but we have to strip it out to allow them to stack.

    public static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // filter out air
            List<ItemStack> filtered = new ArrayList<>();
            for (ItemStack item : items) {
                if (item != null && item.getType() != Material.AIR) filtered.add(item);
            }
            baos.write(filtered.size());
            for (ItemStack item : filtered) {
                Bukkit.getLogger().info("item: " + item);
                if (item != null) {
                    ItemNbt.removeTag(item, MF_GUI_KEY);
                    byte[] bytes = item.serializeAsBytes();
                    baos.write(bytes.length);
                    baos.write(bytes);
                } else baos.write(0);
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ItemStack[] itemStackArrayFromBase64(String data) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
        ItemStack[] items = new ItemStack[inputStream.read()];
        Bukkit.getLogger().info("items.length: " + items.length);

        // Read the serialized inventory
        for (int i = 0; i < items.length; i++) {
            int size = inputStream.read();
            Bukkit.getLogger().info("size: " + size);
            if (size > 0) {
                byte[] bytes = inputStream.readNBytes(size);
                Bukkit.getLogger().info("read bytes: " + bytes);
                ItemStack item = ItemStack.deserializeBytes(bytes);
                Bukkit.getLogger().info("item: " + item);
                ItemNbt.removeTag(item, MF_GUI_KEY);
                items[i] = item;
            }
        }

        inputStream.close();
        return items;
    }
}
