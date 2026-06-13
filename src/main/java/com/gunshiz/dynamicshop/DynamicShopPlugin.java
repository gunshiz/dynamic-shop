package com.gunshiz.dynamicshop;

import org.bukkit.plugin.java.JavaPlugin;
import com.gunshiz.dynamicshop.commands.*;

public class DynamicShopPlugin extends JavaPlugin {

    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private PricingEngine pricingEngine;
    private ShopManager shopManager;
    private MarketEventManager marketEventManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize Managers
        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        economyManager = new EconomyManager(this);
        if (!economyManager.setupEconomy()) {
            getLogger().severe("Vault dependency not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        pricingEngine = new PricingEngine(this);
        shopManager = new ShopManager(this);

        // Register commands
        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("merge").setExecutor(new MergeCommand(this));
        SellCommand sellCmd = new SellCommand(this);
        getCommand("sell").setExecutor(sellCmd);
        getCommand("sell").setTabCompleter(sellCmd);
        ShopAdminCommand adminCmd = new ShopAdminCommand(this);
        getCommand("shopadmin").setExecutor(adminCmd);
        getCommand("shopadmin").setTabCompleter(adminCmd);

        // Register events
        getServer().getPluginManager().registerEvents(new ShopGUIListener(this), this);

        // Start Daily Event Task
        marketEventManager = new MarketEventManager(this);
        marketEventManager.startTask();

        // Register stock command
        getCommand("stock").setExecutor(new StockCommand(this));

        getLogger().info("DynamicShop enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("DynamicShop disabled!");
    }

    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public PricingEngine getPricingEngine() { return pricingEngine; }
    public ShopManager getShopManager() { return shopManager; }
    public MarketEventManager getMarketEventManager() { return marketEventManager; }

    public int getMaxStock() {
        return getConfig().getInt("shop.max_stock", 500);
    }

    public double getElasticity() {
        return getConfig().getDouble("shop.elasticity", 1.5);
    }
}
