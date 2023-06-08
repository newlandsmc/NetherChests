package com.semivanilla.netherchests.storage.impl;

import com.semivanilla.netherchests.NetherChests;
import com.semivanilla.netherchests.storage.StorageProvider;
import com.semivanilla.netherchests.utils.BukkitSerialization;
import me.cookie.cookiecore.data.Values;
import me.cookie.cookiecore.data.sql.H2Storage;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class H2StorageProvider implements StorageProvider {
    private H2Storage storage;

    @Override
    public void init(NetherChests plugin) {
        plugin.getLogger().info("Initializing H2 storage provider");
        storage = new H2Storage(plugin, "data");
        storage.connect();
        storage.initTable("inventories", Arrays.asList("UUID varchar(255)", "contents MEDIUMTEXT"));
        plugin.getLogger().info("Done initializing H2 storage provider");
    }

    @Override
    public void disable(NetherChests plugin) {
        storage.disconnect();
    }

    @Override
    public void save(UUID uuid, ItemStack[] items) {
        String base64 = BukkitSerialization.itemStackArrayToBase64(items);
        List<Values> valuesList = storage.getRowsWhere(
                "inventories",
                "UUID",
                "UUID = '" + uuid.toString() + "'",
                1
        );
        if (valuesList.isEmpty()) {
            //System.out.println("Inserting new row");
            storage.insertIntoTable("inventories", Arrays.asList("UUID", "contents"), new Values(uuid.toString(), base64));
        } else {
            //System.out.println("Updating row");
            storage.updateColumnsWhere("inventories", Arrays.asList("contents"), "UUID = '" + uuid + "'", new Values(base64));
        }
    }

    @Override
    public ItemStack[] load(UUID uuid) {
        List<Values> valuesList = storage.getRowsWhere(
                "inventories",
                "contents",
                "UUID = '" + uuid.toString() + "'",
                2
        );
        if (valuesList.isEmpty()) {
            return new ItemStack[0];
        }
        Values val = valuesList.get(0);
        //System.out.println(Arrays.toString(val.getValues()) + " | " + val);
        String base64 = val.getValues()[0].toString();
        try {
            return BukkitSerialization.itemStackArrayFromBase64(base64);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean contains(UUID uuid) {
        List<Values> valuesList = storage.getRowsWhere(
                "inventories",
                "UUID",
                "UUID = '" + uuid.toString() + "'",
                1
        );
        return !valuesList.isEmpty();
    }

    @Override
    public void delete(UUID uuid) {
        save(uuid, new ItemStack[0]);
    }
}
