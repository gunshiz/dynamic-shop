package com.gunshiz.dynamicshop.managers;

import com.gunshiz.dynamicshop.*;
import com.gunshiz.dynamicshop.managers.*;
import com.gunshiz.dynamicshop.listeners.*;
import com.gunshiz.dynamicshop.utils.*;
import com.gunshiz.dynamicshop.models.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import java.util.List;
import java.util.Random;

public class MarketEventManager {

    private final DynamicShopPlugin plugin;
    private final Random random = new Random();
    private long lastDay = -1;

    private final java.util.Map<Material, Integer> todaysFluctuations = new java.util.LinkedHashMap<>();

    public MarketEventManager(DynamicShopPlugin plugin) {
        this.plugin = plugin;
    }

    public java.util.Map<Material, Integer> getTodaysFluctuations() {
        return todaysFluctuations;
    }

    public void startTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (Bukkit.getWorlds().isEmpty()) return;
            World world = Bukkit.getWorlds().get(0); // Use the main world
            long currentDay = world.getFullTime() / 24000;
            
            if (lastDay == -1) {
                lastDay = currentDay;
            } else if (currentDay > lastDay) {
                lastDay = currentDay;
                triggerDailyEvent();
            }
        }, 20L * 10, 20L * 10); // Run every 10 seconds
    }

    private void triggerDailyEvent() {
        List<ShopItem> originalItems = plugin.getShopManager().getItems();
        if (originalItems.isEmpty()) return;

        // Copy list to avoid UnsupportedOperationException on immutable collections
        List<ShopItem> items = new java.util.ArrayList<>(originalItems);

        int maxStock = plugin.getMaxStock();
        boolean changed = false;

        // Shuffle items to pick randomly
        java.util.Collections.shuffle(items, random);
        
        // Pick between 10 and 25 items (or the total shop size if it's smaller)
        int numToPick = random.nextInt(16) + 10; 
        int picked = Math.min(numToPick, items.size());

        todaysFluctuations.clear();

        for (int i = 0; i < picked; i++) {
            ShopItem item = items.get(i);
            double basePrice = item.getBasePrice();
            
            int change = 0;
            // Higher price = lower chance to restock
            double restockChance = Math.max(0.1, Math.min(0.9, 10.0 / basePrice));
            
            if (random.nextDouble() < restockChance) {
                // Positive restock
                // Higher price = lower max restock
                int maxPositive = (int) Math.max(5, 50.0 / basePrice);
                change = random.nextInt(maxPositive) + 1; // 1 to maxPositive
            } else {
                continue; // Do not decrease stock, just skip
            }
            
            int currentStock = item.getCurrentStock();
            int newStock = currentStock + change;
            
            if (newStock > maxStock) newStock = maxStock;
            if (newStock < 0) newStock = 0;

            if (newStock != currentStock) {
                plugin.getShopManager().updateStock(item, newStock);
                int actualChange = newStock - currentStock;
                todaysFluctuations.put(item.getMaterial(), actualChange);
                changed = true;
            }
        }

        if (changed) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "[DynamicShop] " + ChatColor.AQUA + picked + " items have experienced market fluctuations!");
        }
    }
}
