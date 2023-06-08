package com.semivanilla.netherchests.storage;

import com.semivanilla.netherchests.NetherChests;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public interface StorageProvider {
    void init(NetherChests plugin);

    void disable(NetherChests plugin);

    void save(UUID uuid, ItemStack[] items);

    ItemStack[] load(UUID uuid);

    boolean contains(UUID uuid);

    void delete(UUID uuid);
}
