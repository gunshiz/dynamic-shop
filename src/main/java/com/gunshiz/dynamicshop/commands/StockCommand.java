package com.gunshiz.dynamicshop.commands;

import com.gunshiz.dynamicshop.*;
import com.gunshiz.dynamicshop.managers.*;
import com.gunshiz.dynamicshop.listeners.*;
import com.gunshiz.dynamicshop.utils.*;
import com.gunshiz.dynamicshop.models.*;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class StockCommand implements CommandExecutor {

    private final DynamicShopPlugin plugin;

    public StockCommand(DynamicShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Map<Material, Integer> fluctuations = plugin.getMarketEventManager().getTodaysFluctuations();

        if (fluctuations.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No market fluctuations have happened yet today.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Today's Market Fluctuations ===");
        for (Map.Entry<Material, Integer> entry : fluctuations.entrySet()) {
            int change = entry.getValue();
            String name = entry.getKey().name();
            
            if (change > 0) {
                sender.sendMessage(ChatColor.AQUA + name + ": " + ChatColor.GREEN + "+" + change + " stock");
            } else {
                sender.sendMessage(ChatColor.AQUA + name + ": " + ChatColor.RED + change + " stock");
            }
        }

        return true;
    }
}
