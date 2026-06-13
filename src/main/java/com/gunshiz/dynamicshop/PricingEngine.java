package com.gunshiz.dynamicshop;

public class PricingEngine {
    private final DynamicShopPlugin plugin;

    public PricingEngine(DynamicShopPlugin plugin) {
        this.plugin = plugin;
    }

    public double getBuyPrice(ShopItem item) {
        int maxStock = plugin.getMaxStock();
        double elasticity = plugin.getElasticity();
        double ratio = (double) (maxStock - item.getCurrentStock()) / maxStock;
        double multiplier = 1.0 + (elasticity * ratio);
        return item.getBasePrice() * multiplier;
    }

    public double getSellPrice(ShopItem item) {
        return getBuyPrice(item) * 0.7;
    }

    public double getExactBuyPrice(ShopItem item, int amount) {
        int maxStock = plugin.getMaxStock();
        double elasticity = plugin.getElasticity();
        double total = 0.0;
        int simStock = item.getCurrentStock();
        for (int i = 0; i < amount; i++) {
            double ratio = (double) (maxStock - simStock) / maxStock;
            double multiplier = 1.0 + (elasticity * ratio);
            total += item.getBasePrice() * multiplier;
            simStock--;
        }
        return total;
    }

    public double getExactSellPrice(ShopItem item, int amount) {
        int maxStock = plugin.getMaxStock();
        double elasticity = plugin.getElasticity();
        double total = 0.0;
        int simStock = item.getCurrentStock();
        for (int i = 0; i < amount; i++) {
            double ratio = (double) (maxStock - simStock) / maxStock;
            double multiplier = 1.0 + (elasticity * ratio);
            double buyPrice = item.getBasePrice() * multiplier;
            total += buyPrice * 0.7;
            simStock++;
        }
        return total;
    }
}
