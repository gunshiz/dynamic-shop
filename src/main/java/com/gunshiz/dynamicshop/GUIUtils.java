package com.gunshiz.dynamicshop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GUIUtils {

    public static void openShop(DynamicShopPlugin plugin, Player player, String searchQuery, SortType sortType, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "Dynamic Shop" + (searchQuery != null ? " - " + searchQuery : "") + " (Page " + (page + 1) + ")");

        List<ShopItem> allItems = new ArrayList<>(plugin.getShopManager().getItems());
        
        if (searchQuery != null) {
            allItems.removeIf(item -> !item.getMaterial().name().toLowerCase().contains(searchQuery.toLowerCase()));
        }
        
        allItems.sort((a, b) -> {
            switch (sortType) {
                case NAME_ASC: return a.getMaterial().name().compareTo(b.getMaterial().name());
                case NAME_DESC: return b.getMaterial().name().compareTo(a.getMaterial().name());
                case STOCK_DESC: return Integer.compare(b.getCurrentStock(), a.getCurrentStock());
                case STOCK_ASC: return Integer.compare(a.getCurrentStock(), b.getCurrentStock());
                case PRICE_DESC: return Double.compare(plugin.getPricingEngine().getBuyPrice(b), plugin.getPricingEngine().getBuyPrice(a));
                case PRICE_ASC: return Double.compare(plugin.getPricingEngine().getBuyPrice(a), plugin.getPricingEngine().getBuyPrice(b));
                default: return 0;
            }
        });

        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, allItems.size());
        int slot = 0;

        for (int i = startIndex; i < endIndex; i++) {
            ShopItem item = allItems.get(i);
            ItemStack stack = new ItemStack(item.getMaterial());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.AQUA + item.getMaterial().name());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Stock: " + ChatColor.YELLOW + item.getCurrentStock() + " / " + plugin.getConfig().getInt("shop.max_stock", 500));
                
                double buyPrice = plugin.getPricingEngine().getBuyPrice(item);
                double sellPrice = plugin.getPricingEngine().getSellPrice(item);
                
                lore.add(ChatColor.GREEN + "Buy Price: " + ChatColor.GOLD + "$" + String.format("%.2f", buyPrice));
                lore.add(ChatColor.RED + "Sell Price: " + ChatColor.GOLD + "$" + String.format("%.2f", sellPrice));
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click to Buy / Sell");
                
                meta.setLore(lore);
                stack.setItemMeta(meta);
            }
            inv.setItem(slot++, stack);
        }

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName(ChatColor.RED + "Previous Page");
                prev.setItemMeta(prevMeta);
            }
            inv.setItem(45, prev);
        }

        if (endIndex < allItems.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName(ChatColor.GREEN + "Next Page");
                next.setItemMeta(nextMeta);
            }
            inv.setItem(53, next);
        }

        // Add Sort button
        ItemStack sortBtn = new ItemStack(Material.HOPPER);
        ItemMeta sortMeta = sortBtn.getItemMeta();
        if (sortMeta != null) {
            sortMeta.setDisplayName(ChatColor.GOLD + "Sort By");
            List<String> slore = new ArrayList<>();
            slore.add(ChatColor.YELLOW + "Current: " + ChatColor.WHITE + sortType.getDisplayName());
            slore.add(ChatColor.GRAY + "Click to change sorting");
            sortMeta.setLore(slore);
            sortBtn.setItemMeta(sortMeta);
        }
        inv.setItem(48, sortBtn);

        // Add search button
        ItemStack searchBtn = new ItemStack(Material.NAME_TAG);
        ItemMeta searchMeta = searchBtn.getItemMeta();
        if (searchMeta != null) {
            searchMeta.setDisplayName(ChatColor.GOLD + "Search Items");
            List<String> slore = new ArrayList<>();
            slore.add(ChatColor.GRAY + "Click to search for an item.");
            if (searchQuery != null) {
                slore.add(ChatColor.YELLOW + "Current: " + searchQuery);
                slore.add(ChatColor.RED + "Right-Click to clear search.");
            }
            searchMeta.setLore(slore);
            searchBtn.setItemMeta(searchMeta);
        }
        inv.setItem(49, searchBtn);

        player.openInventory(inv);
    }

    public static void openAmountSelector(DynamicShopPlugin plugin, Player player, ShopItem item, int amount) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_BLUE + "Select Amount: " + item.getMaterial().name());

        ItemStack center = new ItemStack(item.getMaterial(), Math.min(64, Math.max(1, amount)));
        ItemMeta meta = center.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + item.getMaterial().name());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Selected Amount: " + ChatColor.YELLOW + amount);
            lore.add(ChatColor.GRAY + "Current Stock: " + ChatColor.YELLOW + item.getCurrentStock());
            
            double exactBuy = plugin.getPricingEngine().getExactBuyPrice(item, amount);
            double exactSell = plugin.getPricingEngine().getExactSellPrice(item, amount);
            
            lore.add(ChatColor.GREEN + "Total Buy Price: " + ChatColor.GOLD + "$" + String.format("%.2f", exactBuy));
            lore.add(ChatColor.RED + "Total Sell Price: " + ChatColor.GOLD + "$" + String.format("%.2f", exactSell));
            meta.setLore(lore);
            center.setItemMeta(meta);
        }
        inv.setItem(13, center);

        // + Buttons top left
        inv.setItem(0, createModifier(Material.LIME_DYE, "+64"));
        inv.setItem(1, createModifier(Material.LIME_DYE, "+10"));
        inv.setItem(2, createModifier(Material.LIME_DYE, "+1"));
        
        // - Buttons bottom left
        inv.setItem(18, createModifier(Material.RED_DYE, "-64"));
        inv.setItem(19, createModifier(Material.RED_DYE, "-10"));
        inv.setItem(20, createModifier(Material.RED_DYE, "-1"));

        ItemStack buyBtn = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta buyMeta = buyBtn.getItemMeta();
        if (buyMeta != null) {
            buyMeta.setDisplayName(ChatColor.GREEN + "CONFIRM BUY");
            List<String> bLore = new ArrayList<>();
            bLore.add(ChatColor.GRAY + "Shift-Click to instantly buy 64");
            buyMeta.setLore(bLore);
            buyBtn.setItemMeta(buyMeta);
        }
        inv.setItem(8, buyBtn); // Top right

        ItemStack sellBtn = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta sellMeta = sellBtn.getItemMeta();
        if (sellMeta != null) {
            sellMeta.setDisplayName(ChatColor.RED + "CONFIRM SELL");
            List<String> sLore = new ArrayList<>();
            sLore.add(ChatColor.GRAY + "Shift-Click to instantly sell 64");
            sellMeta.setLore(sLore);
            sellBtn.setItemMeta(sellMeta);
        }
        inv.setItem(26, sellBtn); // Bottom right

        // Back button
        ItemStack backBtn = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backBtn.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.RED + "Back to Shop");
            backBtn.setItemMeta(backMeta);
        }
        inv.setItem(17, backBtn); // Middle right

        player.openInventory(inv);
    }

    private static ItemStack createModifier(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
