package com.semivanilla.netherchests.utils;

import com.semivanilla.netherchests.NetherChests;
import com.semivanilla.netherchests.storage.StorageProvider;
import com.semivanilla.netherchests.storage.impl.H2StorageProvider;
import com.semivanilla.netherchests.storage.impl.SQLStorageProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MigrationManager {
    public static final MigrationManager INSTANCE = new MigrationManager();

    private boolean enabled;
    private StorageProvider targetStorageProvider, sourceStorageProvider;

    private MigrationManager() {
    }

    public void init() {
        // check if we are on SQLStorageProvider
        enabled = NetherChests.getInstance().getConfig().getBoolean("migrate.enabled", false);
        if (enabled) {
            if (NetherChests.getInstance().getStorageProvider() instanceof SQLStorageProvider) {
                targetStorageProvider = NetherChests.getInstance().getStorageProvider();
                sourceStorageProvider = new H2StorageProvider();
                sourceStorageProvider.init(NetherChests.getInstance());
            } else {
                throw new RuntimeException("StorageProvider must be SQLStorageProvider to migrate data from H2!");
            }
        }
    }

    public void reloadConfig() {
        boolean before = enabled;
        enabled = NetherChests.getInstance().getConfig().getBoolean("migrate.enabled", false);
        if (!before && enabled) {
            init(); // init if we are enabling
        } else {
            targetStorageProvider = null;
            sourceStorageProvider = null;
        }
    }

    public void tryMigration(UUID uuid) {
        if (enabled) {
            if (targetStorageProvider == null || sourceStorageProvider == null) {
                throw new RuntimeException("MigrationManager not initialized!");
            }
            boolean source = sourceStorageProvider.contains(uuid);
            boolean target = targetStorageProvider.contains(uuid);
            int randId = (int) (Math.random() * 1000000);
            NetherChests.getInstance().getLogger().info("(" + randId + ") [NC-MIGRATE] Checking migration for " + uuid + " (source: " + source + ", target: " + target + ")");
            if (source && !target) {
                try {
                    NetherChests.getInstance().getLogger().info("(" + randId + ") [NC-MIGRATE] Migrating data for " + uuid);
                    targetStorageProvider.save(uuid, sourceStorageProvider.load(uuid));
                    NetherChests.getInstance().getLogger().info("(" + randId + ") [NC-MIGRATE] Done migrating data for " + uuid);
                    sourceStorageProvider.delete(uuid);
                    NetherChests.getInstance().getLogger().info("(" + randId + ") [NC-MIGRATE] Done deleting data for " + uuid);
                } catch (Exception e) {
                    NetherChests.getInstance().getLogger().severe("(" + randId + ") [NC-MIGRATE] Failed to migrate data for " + uuid);
                    e.printStackTrace();
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        player.sendRichMessage("<red>Unfortunately we had an issue migrating your NetherChest. Please open a ticket on the discord server and provide this ID: " + randId);
                    }
                }
            } else if (source) {
                NetherChests.getInstance().getLogger().info("(" + randId + ") [NC-MIGRATE] Skipping migration for " + uuid + " as they already have data");
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.sendRichMessage("<red>We found items in your old NetherChest, but you already have items in your new NetherChest. Please open a ticket on the discord server and provide this ID: " + randId);
                }
            }
        }
    }
}
