package com.gunshiz.dynamicshop;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Material;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private HikariDataSource dataSource;
    private final DynamicShopPlugin plugin;

    public DatabaseManager(DynamicShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String dbName = plugin.getConfig().getString("database.name", "minecraft");
        String username = plugin.getConfig().getString("database.username", "root");
        String password = plugin.getConfig().getString("database.password", "password");

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + dbName);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        
        dataSource = new HikariDataSource(config);
        setupTables();
    }

    private void setupTables() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS shop_items (" +
                             "material VARCHAR(64) PRIMARY KEY, " +
                             "base_price DOUBLE PRECISION NOT NULL, " +
                             "current_stock INTEGER NOT NULL DEFAULT 250" +
                             ");"
             )) {
            stmt.execute();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not setup database tables: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public List<ShopItem> getAllItems() {
        List<ShopItem> items = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM shop_items");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String matStr = rs.getString("material");
                Material mat = Material.matchMaterial(matStr);
                if (mat == null) continue;

                double basePrice = rs.getDouble("base_price");
                int currentStock = rs.getInt("current_stock");

                items.add(new ShopItem(mat, basePrice, currentStock));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error fetching shop items: " + e.getMessage());
        }
        return items;
    }

    public void updateStock(Material material, int newStock) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE shop_items SET current_stock = ? WHERE material = ?")) {
            stmt.setInt(1, newStock);
            stmt.setString(2, material.name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error updating stock: " + e.getMessage());
        }
    }

    public void updateBasePrice(Material material, double basePrice) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE shop_items SET base_price = ? WHERE material = ?")) {
            stmt.setDouble(1, basePrice);
            stmt.setString(2, material.name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error updating base price: " + e.getMessage());
        }
    }

    public void addShopItem(Material material, double basePrice) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO shop_items (material, base_price, current_stock) VALUES (?, ?, 0)")) {
            stmt.setString(1, material.name());
            stmt.setDouble(2, basePrice);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error adding item: " + e.getMessage());
        }
    }

    public void removeShopItem(Material material) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM shop_items WHERE material = ?")) {
            stmt.setString(1, material.name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error removing item: " + e.getMessage());
        }
    }

    public void addShopItemsBatch(java.util.Map<Material, Double> items) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO shop_items (material, base_price, current_stock) VALUES (?, ?, 0) ON DUPLICATE KEY UPDATE material=material")) {
            
            // Note: ON DUPLICATE KEY UPDATE is MySQL specific, used to safely ignore duplicates
            for (java.util.Map.Entry<Material, Double> entry : items.entrySet()) {
                stmt.setString(1, entry.getKey().name());
                stmt.setDouble(2, entry.getValue());
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error adding batch items: " + e.getMessage());
        }
    }
}
