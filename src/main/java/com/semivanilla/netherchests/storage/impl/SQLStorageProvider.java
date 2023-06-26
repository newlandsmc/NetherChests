package com.semivanilla.netherchests.storage.impl;

import com.semivanilla.netherchests.NetherChests;
import com.semivanilla.netherchests.storage.StorageProvider;
import com.semivanilla.netherchests.utils.BukkitSerialization;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Arrays;
import java.util.UUID;

public class SQLStorageProvider implements StorageProvider {
    private String table = "netherchests";
    private static File sqlConfigFile;

    private HikariDataSource dataSource;

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
        String database = config.getString("database");
        int port = config.getInt("port", 3306);
        String driver = config.getString("driver-class", "com.mysql.jdbc.Driver");
        table = config.getString("table", "netherchests");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(driver);
        hikariConfig.setJdbcUrl(
                "jdbc:mysql://" +
                        url +
                        ":" +
                        port +
                        "/" +
                        database
        );
        hikariConfig.addDataSourceProperty("serverName", url);
        hikariConfig.addDataSourceProperty("port", port);
        hikariConfig.addDataSourceProperty("databaseName", database);
        hikariConfig.addDataSourceProperty("user", user);
        hikariConfig.addDataSourceProperty("password", password);

        hikariConfig.setConnectionTimeout(config.getLong("connection-timeout", 30000));
        hikariConfig.setIdleTimeout(config.getLong("idle-timeout", 600000));
        hikariConfig.setMaxLifetime(config.getLong("max-lifetime", 1800000));
        hikariConfig.setMaximumPoolSize(config.getInt("maximum-pool-size", 15));

        dataSource = new HikariDataSource(hikariConfig);

        // contents are bytes
        String sql = "CREATE TABLE IF NOT EXISTS " + table + "(UUID varchar(255), contents MEDIUMBLOB)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
        ) {
            statement.executeQuery();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void disable(NetherChests plugin) {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public void save(UUID uuid, ItemStack[] items) {
        if (items == null || items.length == 0 || Arrays.stream(items).allMatch(item -> item == null || item.getType() == Material.AIR)) {
            delete(uuid);
            return;
        }
        byte[] bytes = BukkitSerialization.itemStacksToByteArray(items);
        String selectSQL = "SELECT * FROM " + table + " WHERE UUID = ?";
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement1 = connection.prepareStatement(selectSQL);
        ) {
            statement1.setString(1, uuid.toString());
            ResultSet resultSet = statement1.executeQuery();
            if (resultSet.next()) {
                String updateSQL = "UPDATE " + table + " SET contents = ? WHERE UUID = ?";
                PreparedStatement statement2 = connection.prepareStatement(updateSQL);
                // statement2.setString(1, base64);
                statement2.setBytes(1, bytes);
                statement2.setString(2, uuid.toString());
                statement2.executeUpdate();
            } else {
                String insertSQL = "INSERT INTO " + table + " (UUID, contents) VALUES (?, ?)";
                PreparedStatement statement3 = connection.prepareStatement(insertSQL);
                statement3.setString(1, uuid.toString());
                // statement3.setString(2, base64);
                statement3.setBytes(2, bytes);
                statement3.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ItemStack[] load(UUID uuid) {
        String selectSQL = "SELECT * FROM " + table + " WHERE UUID = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSQL)) {
            statement.setString(1, uuid.toString());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                /*
                String base64 = resultSet.getString("contents");
                try {
                    return BukkitSerialization.itemStackArrayFromBase64(base64);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                 */
                byte[] bytes = resultSet.getBytes("contents");
                return BukkitSerialization.byteArrayToItemStacks(bytes);
            } else return new ItemStack[0];
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean contains(UUID uuid) {
        String selectSQL = "SELECT * FROM " + table + " WHERE UUID = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSQL)
        ) {
            statement.setString(1, uuid.toString());
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(UUID uuid) {
        String deleteSQL = "DELETE FROM " + table + " WHERE UUID = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(deleteSQL)) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
