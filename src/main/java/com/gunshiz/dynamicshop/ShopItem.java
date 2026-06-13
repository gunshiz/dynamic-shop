package com.gunshiz.dynamicshop;

import org.bukkit.Material;

public class ShopItem {
    private final Material material;
    private final double basePrice;
    private int currentStock;

    public ShopItem(Material material, double basePrice, int currentStock) {
        this.material = material;
        this.basePrice = basePrice;
        this.currentStock = currentStock;
    }

    public Material getMaterial() {
        return material;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
    }
}
