package com.semivanilla.netherchests;

import com.semivanilla.netherchests.listener.ClickListener;
import com.semivanilla.netherchests.listener.BlockListener;
import com.semivanilla.netherchests.storage.StorageProvider;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class NetherChests extends JavaPlugin {
    private static NetherChests instance;
    private static MiniMessage miniMessage = MiniMessage.builder().build();
    private StorageProvider storageProvider;

    private boolean updateOnTransaction = false;

    public static NetherChests getInstance() {
        return instance;
    }

    public static MiniMessage getMiniMessage() {
        return miniMessage;
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveDefaultConfig();
        }
        try {
            String storageProvider = getConfig().getString("storage-provider");
            getLogger().info("Initializing storage provider " + storageProvider);
            Class<?> storageProviderClass = Class.forName("com.semivanilla.netherchests.storage.impl." + storageProvider);
            this.storageProvider = (StorageProvider) storageProviderClass.newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }

        this.storageProvider.init(this);
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new ClickListener(), this);

        updateOnTransaction = getConfig().getBoolean("update-on-transaction", true);
    }

    @Override
    public void onDisable() {
        this.storageProvider.disable(this);
    }

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    public boolean isNetherChest(Block block) {
        if (block.getType() != Material.CHEST) {
            return false;
        }

        boolean hasMetadata = block.hasMetadata("NetherChest");
        boolean isNetherChest = hasMetadata && block.getMetadata("NetherChest").get(0).asBoolean();
        if (!hasMetadata) {
            Block relativeBlock = null;
            for (int i = 0; i < 5; i++) {
                switch (i) {
                    case 0 -> relativeBlock = block.getRelative(BlockFace.NORTH);
                    case 1 -> relativeBlock = block.getRelative(BlockFace.EAST);
                    case 2 -> relativeBlock = block.getRelative(BlockFace.SOUTH);
                    case 3 -> relativeBlock = block.getRelative(BlockFace.WEST);
                    case 4 -> relativeBlock = block.getRelative(BlockFace.UP);
                    default -> {
                        block.setMetadata("NetherChest", new FixedMetadataValue(this, false));
                        return false;
                    }
                }

                if (relativeBlock.getType().name().endsWith("_SIGN")) {
                    Sign sign = (Sign) relativeBlock.getState();
                    if (BlockListener.isNetherChestSign(sign.getLines())) {
                        block.setMetadata("NetherChest", new FixedMetadataValue(this, true));
                        return true;
                    }
                }
            }
            block.setMetadata("NetherChest", new FixedMetadataValue(this, false));
        }
        return isNetherChest;
    }

    public boolean isUpdateOnTransaction() {
        return updateOnTransaction;
    }
}
