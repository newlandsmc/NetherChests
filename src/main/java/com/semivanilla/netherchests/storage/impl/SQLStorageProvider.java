package com.semivanilla.netherchests.storage.impl;

import com.semivanilla.netherchests.NetherChests;
import com.semivanilla.netherchests.storage.StorageProvider;
import com.semivanilla.netherchests.utils.BukkitSerialization;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.UUID;

public class SQLStorageProvider implements StorageProvider {
    private static File sqlConfigFile;

    private Connection connection;

    @Override
    public void init(NetherChests plugin) {
        sqlConfigFile = new File(plugin.getDataFolder(), "sql.yml");
        if (!sqlConfigFile.exists()) {
            plugin.getLogger().info("SQL config file not found, creating...");
            plugin.saveResource("sql.yml", false);
            plugin.getLogger().info("Done creating SQL config file");
        }
        plugin.getLogger().info("Initializing SQL storage provider");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(sqlConfigFile);
        String url = config.getString("url");
        String user = config.getString("user");
        String password = config.getString("password");
        try {
            connection = DriverManager.getConnection(url, user, password);
            String sql = "CREATE TABLE IF NOT EXISTS inventories(UUID varchar(255), contents MEDIUMTEXT)";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void disable(NetherChests plugin) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(UUID uuid, ItemStack[] items) {
        String base64 = BukkitSerialization.itemStackArrayToBase64(items);
        String selectSQL = "SELECT * FROM inventories WHERE UUID = ?";
        try {
            PreparedStatement statement1 = connection.prepareStatement(selectSQL);
            statement1.setString(1, uuid.toString());
            ResultSet resultSet = statement1.executeQuery();
            if (resultSet.next()) {
                String updateSQL = "UPDATE inventories SET contents = ? WHERE UUID = ?";
                PreparedStatement statement2 = connection.prepareStatement(updateSQL);
                statement2.setString(1, base64);
                statement2.setString(2, uuid.toString());
                statement2.executeUpdate();
            } else {
                String insertSQL = "INSERT INTO inventories (UUID, contents) VALUES (?, ?)";
                PreparedStatement statement3 = connection.prepareStatement(insertSQL);
                statement3.setString(1, uuid.toString());
                statement3.setString(2, base64);
                statement3.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ItemStack[] load(UUID uuid) {
        String selectSQL = "SELECT * FROM inventories WHERE UUID = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(selectSQL);
            statement.setString(1, uuid.toString());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String base64 = resultSet.getString("contents");
                try {
                    return BukkitSerialization.itemStackArrayFromBase64(base64);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else return new ItemStack[0];
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
