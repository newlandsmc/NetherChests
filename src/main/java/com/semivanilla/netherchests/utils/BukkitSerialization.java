package com.semivanilla.netherchests.utils;

import dev.triumphteam.gui.components.util.ItemNbt;
import org.bukkit.inventory.ItemStack;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BukkitSerialization {
    private static final String MF_GUI_KEY = "mf-gui"; // TriumphGUI adds this nbt tag to the items, but we have to strip it out

    public static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(items.length);
            for (ItemStack item : items) {
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

        // Read the serialized inventory
        for (int i = 0; i < items.length; i++) {
            int size = inputStream.read();
            if (size > 0) {
                byte[] bytes = inputStream.readNBytes(size);
                ItemStack item = ItemStack.deserializeBytes(bytes);
                ItemNbt.removeTag(item, MF_GUI_KEY);
                items[i] = item;
            }
        }

        inputStream.close();
        return items;
    }
}
