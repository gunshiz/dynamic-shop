package com.gunshiz.dynamicshop;

import org.bukkit.Material;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopManager {
    private final DynamicShopPlugin plugin;
    private final Map<Material, ShopItem> itemsCache = new HashMap<>();

    public ShopManager(DynamicShopPlugin plugin) {
        this.plugin = plugin;
        loadItems();
    }

    public void loadItems() {
        itemsCache.clear();
        List<ShopItem> items = plugin.getDatabaseManager().getAllItems();
        for (ShopItem item : items) {
            itemsCache.put(item.getMaterial(), item);
        }
        
        // Add some default items if empty for testing
        if (itemsCache.isEmpty()) {
            addDefaultItem(Material.DIAMOND, 100.0);
            addDefaultItem(Material.IRON_INGOT, 20.0);
            addDefaultItem(Material.GOLD_INGOT, 50.0);
            addDefaultItem(Material.EMERALD, 80.0);
            addDefaultItem(Material.OAK_LOG, 5.0);
            addDefaultItem(Material.COBBLESTONE, 1.0);
            addDefaultItem(Material.WHEAT, 2.0);
        }
    }

    private void addDefaultItem(Material mat, double price) {
        try {
            java.sql.Connection conn = plugin.getDatabaseManager().getClass().getDeclaredField("dataSource").get(plugin.getDatabaseManager()) != null ? 
                ((com.zaxxer.hikari.HikariDataSource)plugin.getDatabaseManager().getClass().getDeclaredField("dataSource").get(plugin.getDatabaseManager())).getConnection() : null;
            
            if (conn != null) {
                java.sql.PreparedStatement stmt = conn.prepareStatement("INSERT INTO shop_items (material, base_price, current_stock) VALUES (?, ?, 250) ON CONFLICT DO NOTHING");
                stmt.setString(1, mat.name());
                stmt.setDouble(2, price);
                stmt.executeUpdate();
                stmt.close();
                conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        itemsCache.put(mat, new ShopItem(mat, price, 250));
    }

    public List<ShopItem> getItems() {
        return List.copyOf(itemsCache.values());
    }

    public ShopItem getItem(Material material) {
        return itemsCache.get(material);
    }

    public void updateStock(ShopItem item, int newStock) {
        item.setCurrentStock(newStock);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().updateStock(item.getMaterial(), newStock);
        });
    }

    public void migrateShopData(org.bukkit.command.CommandSender sender, String filename) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            java.io.File file = new java.io.File(plugin.getDataFolder(), filename);
            if (!file.exists()) {
                sender.sendMessage(filename + " not found in the plugin folder!");
                return;
            }
            org.bukkit.configuration.file.FileConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            org.bukkit.configuration.ConfigurationSection itemsSection = config.getConfigurationSection("items");
            if (itemsSection == null) {
                sender.sendMessage("No 'items' section found in " + filename + "!");
                return;
            }
            int count = 0;
            try (java.sql.Connection conn = plugin.getDatabaseManager().getConnection()) {
                java.sql.PreparedStatement stmt = conn.prepareStatement("INSERT INTO shop_items (material, base_price, current_stock) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE base_price = VALUES(base_price)");
                for (String key : itemsSection.getKeys(false)) {
                    org.bukkit.Material mat = org.bukkit.Material.matchMaterial(key);
                    if (mat == null) continue;
                    double basePrice = itemsSection.getDouble(key + ".base", itemsSection.getDouble(key + ".price", 10.0));
                    int stock = (int) itemsSection.getDouble(key + ".stock", 250);
                    stmt.setString(1, mat.name());
                    stmt.setDouble(2, basePrice);
                    stmt.setInt(3, stock);
                    stmt.addBatch();
                    count++;
                }
                stmt.executeBatch();
                sender.sendMessage("Successfully migrated " + count + " items from " + filename + "!");
                plugin.getServer().getScheduler().runTask(plugin, this::loadItems);
            } catch (Exception e) {
                sender.sendMessage("Error migrating data: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
