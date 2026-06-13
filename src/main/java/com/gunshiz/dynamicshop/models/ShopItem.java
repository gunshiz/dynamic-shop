package com.gunshiz.dynamicshop.models;

import com.gunshiz.dynamicshop.*;
import com.gunshiz.dynamicshop.managers.*;
import com.gunshiz.dynamicshop.listeners.*;
import com.gunshiz.dynamicshop.utils.*;
import com.gunshiz.dynamicshop.models.*;

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
