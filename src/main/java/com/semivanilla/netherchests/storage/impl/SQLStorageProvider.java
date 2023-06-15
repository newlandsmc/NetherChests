package com.semivanilla.netherchests.storage.impl;

import com.semivanilla.netherchests.NetherChests;
import com.semivanilla.netherchests.storage.StorageProvider;
import com.semivanilla.netherchests.utils.BukkitSerialization;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.sql.*;
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
        try {
            //connection = DriverManager.getConnection(url, user, password);
            dataSource = new HikariDataSource();
            dataSource.setDriverClassName(driver);
            dataSource.setJdbcUrl(
                    "jdbc:mysql://" +
                            url +
                            ":" +
                            port +
                            "/" +
                            database
            );
            dataSource.addDataSourceProperty("serverName", url);
            dataSource.addDataSourceProperty("port", port);
            dataSource.addDataSourceProperty("databaseName", database);
            dataSource.addDataSourceProperty("user", user);
            dataSource.addDataSourceProperty("password", password);

            // contents are bytes
            String sql = "CREATE TABLE IF NOT EXISTS " + table + "(UUID varchar(255), contents MEDIUMBLOB)";
            PreparedStatement statement = dataSource.getConnection().prepareStatement(sql);
            statement.execute();
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
        byte[] bytes = BukkitSerialization.itemStacksToByteArray(items);
        String selectSQL = "SELECT * FROM " + table + " WHERE UUID = ?";
        try {
            Connection connection = dataSource.getConnection();
            PreparedStatement statement1 = connection.prepareStatement(selectSQL);
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
        try {
            PreparedStatement statement = dataSource.getConnection().prepareStatement(selectSQL);
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
        try {
            PreparedStatement statement = dataSource.getConnection().prepareStatement(selectSQL);
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
        try {
            PreparedStatement statement = dataSource.getConnection().prepareStatement(deleteSQL);
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
