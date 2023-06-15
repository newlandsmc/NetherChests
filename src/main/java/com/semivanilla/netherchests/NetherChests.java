package com.semivanilla.netherchests;

import com.semivanilla.netherchests.listener.BlockListener;
import com.semivanilla.netherchests.listener.ClickListener;
import com.semivanilla.netherchests.storage.StorageProvider;
import com.semivanilla.netherchests.storage.impl.H2StorageProvider;
import com.semivanilla.netherchests.utils.BukkitSerialization;
import dev.triumphteam.gui.guis.BaseGui;
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
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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
        gui.setDefaultClickAction(event -> {
            if (true) {
                if (event.isShiftClick() && event.getClickedInventory() == event.getWhoClicked().getInventory()
                        && !NetherChests.getInstance().getConfig().getBoolean("enable-insert", true)) { // in a NetherChest
                    event.setCancelled(true);
                    Component message = NetherChests.getMiniMessage().deserialize(NetherChests.getInstance().getConfig().getString("insert-disabled-message", "<red>You cannot put items in this chest!"));
                    event.getWhoClicked().sendMessage(message);
                    return;
                }
                return;
            }
            // System.out.println(event.getClick() + " | " + (event.getCursor() != null ? event.getCursor().getType() : "null") + " | " +
            //        (event.getCurrentItem() != null ? event.getCurrentItem().getType() : "null"));
            ItemStack item = event.getCurrentItem();
            if (item == null) item = event.getCursor();
            if (item != null && item.getType().name().endsWith("SHULKER_BOX") && false) {
                // get shulker box contents
                if (item.getItemMeta() instanceof BlockStateMeta im) {
                    if (im.getBlockState() instanceof ShulkerBox box) {
                        for (ItemStack itemStack : box.getInventory().getContents()) {
                            if (itemStack != null && !itemStack.getEnchantments().isEmpty()) {
                                event.setCancelled(true);
                                event.getWhoClicked().sendMessage(ChatColor.RED + "You cannot currently put shulker boxes with enchanted items in a nether chest! This is a bug and will be fixed soon. Sorry for the inconvenience. Please remove the enchanted items from the shulker box and try again. (They can be added to the netherchest separately)");
                                return;
                            }
                        }
                    }
                }
            }
        });
        gui.setDefaultTopClickAction((e) -> {
            //System.out.println(e.getAction() + " - top");
            // if they are putting items in
            if (e.getAction() == InventoryAction.PLACE_ALL || e.getAction() == InventoryAction.PLACE_ONE || e.getAction() == InventoryAction.PLACE_SOME) {
                if (!getConfig().getBoolean("enable-insert", true)) {
                    e.setCancelled(true);
                    Component message = NetherChests.getMiniMessage().deserialize(getConfig().getString("insert-disabled-message", "<red>You cannot put items in this chest!"));
                    e.getWhoClicked().sendMessage(message);
                    return;
                }
            }

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
            sender.sendMessage(ChatColor.RED + " - /netherchests delete <player> | Remove a player's entry from the database.");
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
        } else if (args[0].equalsIgnoreCase("delete")) {
            if (args.length == 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /netherchests delete <player>");
                return true;
            }
            OfflinePlayer player = getServer().getOfflinePlayer(args[1]);
            if (!storageProvider.contains(player.getUniqueId())) {
                sender.sendMessage(ChatColor.RED + "Player " + player.getName() + " is not in the database. Use /netherchests create " + player.getName() + " to create a new entry.");
                return true;
            }
            sender.sendMessage(ChatColor.GREEN + "Deleting " + player.getName() + "'s Nether Chest...");
            storageProvider.delete(player.getUniqueId());
            sender.sendMessage(ChatColor.GREEN + "Done.");
            return true;
        } else if (args[0].equalsIgnoreCase("test_a")) {
            ItemStack holding = ((Player) sender).getInventory().getItemInMainHand();
            if (holding == null || holding.getType() == Material.AIR) {
                sender.sendMessage(ChatColor.RED + "You must be holding an item to test this command.");
                return true;
            }
            String base64 = BukkitSerialization.serializeItem(holding);
            sender.sendMessage(ChatColor.GREEN + "Serialized: " + base64);
            try {
                ItemStack item = BukkitSerialization.deserializeItem(base64);
                ((Player) sender).getInventory().setItemInMainHand(item);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (args[0].equalsIgnoreCase("migrate")) {

        }
        return true;
    }

    public boolean isNetherChestOpen(UUID ownerUUID) {
        return getConfig().getBoolean("lock-chests", true) && openChests.containsKey(ownerUUID);
    }
}
