package com.semivanilla.netherchests;

import com.semivanilla.netherchests.listener.BlockListener;
import com.semivanilla.netherchests.listener.ClickListener;
import com.semivanilla.netherchests.storage.StorageProvider;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.StorageGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public final class NetherChests extends JavaPlugin implements CommandExecutor {
    private static NetherChests instance;
    private static MiniMessage miniMessage = MiniMessage.builder().build();
    private StorageProvider storageProvider;

    private boolean updateOnTransaction = false;
    private Map<UUID, UUID> openChests = new HashMap<>();

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

        Objects.requireNonNull(getCommand("netherchests")).setExecutor(this);

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
                        Component line2 = sign.line(2);
                        String rawOwner = PlainTextComponentSerializer.plainText().serialize(line2);
                        block.setMetadata("NetherChestOwner", new FixedMetadataValue(this, rawOwner));
                        return true;
                    }
                }
            }
            block.setMetadata("NetherChest", new FixedMetadataValue(this, false));
        }
        return isNetherChest;
    }

    public void openNetherChest(Player player, UUID uuid) {
        openNetherChest(player, uuid, false);
    }
    public void openNetherChest(Player player, UUID uuid, boolean ignoreLock) {
        if (!ignoreLock && NetherChests.getInstance().isNetherChestOpen(uuid)) {
            player.sendMessage(ChatColor.RED + "This chest is already open!");
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1, 1);
        ItemStack[] items = NetherChests.getInstance().getStorageProvider().load(uuid);
        StorageGui gui = Gui.storage()
                .title(NetherChests.getMiniMessage().deserialize(NetherChests.getInstance().getConfig().getString("name", "<dark_red>Nether Chest")))
                .rows(NetherChests.getInstance().getConfig().getInt("rows", 3))
                .create();
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                gui.setItem(i, new GuiItem(items[i]));
            }
        }
        openChests.put(uuid, player.getUniqueId());
        gui.open(player);
        gui.setCloseGuiAction((e) -> {
            NetherChests.getInstance().getStorageProvider().save(uuid,
                    e.getInventory().getContents());
            player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, 1, 1);
            openChests.remove(uuid);
        });
        gui.setDefaultTopClickAction((e) -> {
            if (NetherChests.getInstance().isUpdateOnTransaction()) {
                if (e.getCurrentItem() == null && (e.getAction() != InventoryAction.PLACE_SOME && e.getAction() != InventoryAction.PLACE_ALL && e.getAction() != InventoryAction.PLACE_ONE)) {
                    return;
                }
                NetherChests.getInstance().getStorageProvider().save(uuid,
                        e.getInventory().getContents());
            }
        });
    }

    public boolean isUpdateOnTransaction() {
        return updateOnTransaction;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage:");
            sender.sendMessage(ChatColor.RED + " - /netherchests reload | Reloads the config");
            sender.sendMessage(ChatColor.RED + " - /netherchests open <player> | Open a player's nether chest");
            sender.sendMessage(ChatColor.RED + " - /netherchests create <player> | Add a entry into the database for a player.");
            return false;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            long start = System.currentTimeMillis();
            reloadConfig();
            updateOnTransaction = getConfig().getBoolean("update-on-transaction", true);
            sender.sendMessage(ChatColor.GREEN + "Config reloaded in " + (System.currentTimeMillis() - start) + "ms");
            return true;
        } else if (args[0].equalsIgnoreCase("open")) {
            if (args.length == 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /netherchests open <player>");
                return true;
            }
            OfflinePlayer player = getServer().getOfflinePlayer(args[1]);
            if (!storageProvider.contains(player.getUniqueId())) {
                sender.sendMessage(ChatColor.RED + "Player " + player.getName() + " is not in the database. (they might not have anything inside.) Use /netherchests create " + player.getName() + " to create a new entry.");
                return true;
            }
            sender.sendMessage(ChatColor.GREEN + "Opening " + player.getName() + "'s Nether Chest...");
            openNetherChest((Player) sender, player.getUniqueId(), true);
        } else if (args[0].equalsIgnoreCase("create")) {
            if (args.length == 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /netherchests create <player>");
                return true;
            }
            OfflinePlayer player = getServer().getOfflinePlayer(args[1]);
            if (storageProvider.contains(player.getUniqueId())) {
                sender.sendMessage(ChatColor.RED + "Player " + player.getName() + " is already in the database. Use /netherchests open " + player.getName() + " to open their Nether Chest.");
                return true;
            }
            sender.sendMessage(ChatColor.GREEN + "Creating " + player.getName() + "'s Nether Chest...");
            storageProvider.save(player.getUniqueId(), new ItemStack[0]);
            sender.sendMessage(ChatColor.GREEN + "Done.");
            return true;
        }
        return true;
    }

    public boolean isNetherChestOpen(UUID ownerUUID) {
        return getConfig().getBoolean("lock-chests", true) && openChests.containsKey(ownerUUID);
    }
}
