package com.gunshiz.dynamicshop.commands;

import com.gunshiz.dynamicshop.*;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class StockCommand implements CommandExecutor {

    private final DynamicShopPlugin plugin;

    public StockCommand(DynamicShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<ShopItem> items = plugin.getShopManager().getItems();

        if (items.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "The shop is currently empty.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Shop Items Stock ===");
        for (ShopItem item : items) {
            String name = item.getMaterial().name();
            int stock = item.getCurrentStock();
            sender.sendMessage(ChatColor.AQUA + name + ": " + ChatColor.WHITE + stock + " stock");
        }

        return true;
    }
}
