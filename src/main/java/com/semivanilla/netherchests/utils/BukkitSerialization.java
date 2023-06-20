package com.semivanilla.netherchests.utils;

import dev.triumphteam.gui.components.util.ItemNbt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class BukkitSerialization {
    public static final String MF_GUI_KEY = "mf-gui"; // TriumphGUI adds this nbt tag to the items, but we have to strip it out to allow them to stack.

    public static byte[] itemStacksToByteArray(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeInt(items.length);

            for (ItemStack item : items) {
                if (item != null) {
                    ItemNbt.removeTag(item, MF_GUI_KEY);
                    dataOutput.writeObject(item.serializeAsBytes());
                } else {
                    dataOutput.writeObject(null);
                }
            }

            dataOutput.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }
    public static ItemStack[] byteArrayToItemStacks(byte[] bytes) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
        ItemStack[] items = new ItemStack[dataInput.readInt()];

        for (int i = 0; i < items.length; i++) {
            byte[] stack = new byte[0];
            try {
                stack = (byte[]) dataInput.readObject();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            if (stack != null) {
                ItemStack item = ItemStack.deserializeBytes(stack);
                ItemNbt.removeTag(item, MF_GUI_KEY);
                items[i] = item;
            } else {
                items[i] = null;
            }
        }

        dataInput.close();
        return items;
    }

    public static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
        return Base64.getEncoder().encodeToString(itemStacksToByteArray(items));
    }
    public static ItemStack[] itemStackArrayFromBase64(String data) throws IOException {
        data = data.replace("\n", "").trim();
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(data);
            return byteArrayToItemStacks(bytes);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to deserialize itemstacks: " + e.getMessage() + " - Attempting legacy deserialization.");
            return LegacySerialization.itemStackArrayFromBase64(data);
        }
    }

    public static String serializeItem(ItemStack item) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] bytes = item.serializeAsBytes();
            baos.write(bytes.length);
            baos.write(bytes);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static ItemStack deserializeItem(String data) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
        int size = inputStream.read();
        if (size > 0) {
            byte[] bytes = inputStream.readNBytes(size);
            ItemStack item = ItemStack.deserializeBytes(bytes);
            ItemNbt.removeTag(item, MF_GUI_KEY);
            return item;
        }
        return null;
    }

    public static class LegacySerialization {
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
                return Base64Coder.encodeLines(baos.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public static ItemStack[] itemStackArrayFromBase64(String data) throws IOException {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            ItemStack[] items = new ItemStack[inputStream.read()];
            try {
                int bytesRead = 0;
                // Read the serialized inventory
                for (int i = 0; i < items.length; i++) {
                    int size = inputStream.read();
                    if (size > 0) {
                        byte[] bytes = inputStream.readNBytes(size);
                        bytesRead += size + 1;
                        System.out.println("bytesRead: " + bytesRead);
                        ItemStack item = ItemStack.deserializeBytes(bytes);
                        System.out.println("item: " + item);
                        ItemNbt.removeTag(item, MF_GUI_KEY);
                        items[i] = item;
                    }
                }

                inputStream.close();
            } catch (Exception e) {
                Bukkit.getLogger().info("Error deserializing itemstacks (legacy): " + e.getMessage());
            }
            return items;
        }
    }
}
