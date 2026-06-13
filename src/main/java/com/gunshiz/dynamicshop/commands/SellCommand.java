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
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SellCommand implements TabExecutor {

    private final DynamicShopPlugin plugin;

    public SellCommand(DynamicShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        String sub = "hand";
        if (args.length > 0) {
            sub = args[0].toLowerCase();
        }

        if (sub.equals("hand")) {
            ItemStack inHand = player.getInventory().getItemInMainHand();
            if (inHand == null || inHand.getType() == Material.AIR) {
                player.sendMessage(ChatColor.RED + "You are not holding any item.");
                return true;
            }
            sellItems(player, inHand.getType(), inHand.getAmount(), false);
            return true;
        } else if (sub.equals("handall")) {
            ItemStack inHand = player.getInventory().getItemInMainHand();
            if (inHand == null || inHand.getType() == Material.AIR) {
                player.sendMessage(ChatColor.RED + "You are not holding any item.");
                return true;
            }
            sellItems(player, inHand.getType(), -1, true);
            return true;
        } else if (sub.equals("all")) {
            sellAll(player);
            return true;
        } else if (sub.equals("gui")) {
            org.bukkit.inventory.Inventory gui = plugin.getServer().createInventory(player, 54, "Drop items to sell");
            player.openInventory(gui);
            return true;
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /sell <hand|all|handall|gui>");
            return true;
        }
    }

    private void sellItems(Player player, Material material, int amount, boolean sellAllOfType) {
        ShopItem shopItem = plugin.getShopManager().getItem(material);
        if (shopItem == null) {
            player.sendMessage(ChatColor.RED + "This item cannot be sold to the shop.");
            return;
        }

        int maxStock = plugin.getMaxStock();
        if (shopItem.getCurrentStock() >= maxStock) {
            player.sendMessage(ChatColor.RED + "The shop cannot buy any more of this item (max stock reached)!");
            return;
        }

        int availableToSell = 0;
        if (sellAllOfType) {
            for (ItemStack is : player.getInventory().getContents()) {
                if (is != null && is.getType() == material) {
                    availableToSell += is.getAmount();
                }
            }
        } else {
            availableToSell = amount;
        }

        if (availableToSell == 0) {
            player.sendMessage(ChatColor.RED + "You don't have any of this item to sell.");
            return;
        }

        int spaceInShop = maxStock - shopItem.getCurrentStock();
        int amountToSell = Math.min(availableToSell, spaceInShop);

        double price = plugin.getPricingEngine().getExactSellPrice(shopItem, amountToSell);

        int remainingToRemove = amountToSell;
        ItemStack[] contents = player.getInventory().getContents();

        if (!sellAllOfType) {
            ItemStack inHand = player.getInventory().getItemInMainHand();
            if (inHand != null && inHand.getType() == material) {
                if (inHand.getAmount() <= remainingToRemove) {
                    remainingToRemove -= inHand.getAmount();
                    player.getInventory().setItemInMainHand(null);
                } else {
                    inHand.setAmount(inHand.getAmount() - remainingToRemove);
                    remainingToRemove = 0;
                }
            }
        }

        if (remainingToRemove > 0) {
            for (int i = 0; i < contents.length; i++) {
                ItemStack is = contents[i];
                if (is != null && is.getType() == material) {
                    if (is.getAmount() <= remainingToRemove) {
                        remainingToRemove -= is.getAmount();
                        player.getInventory().setItem(i, null);
                    } else {
                        is.setAmount(is.getAmount() - remainingToRemove);
                        remainingToRemove = 0;
                        break;
                    }
                }
            }
        }

        plugin.getEconomyManager().getEconomy().depositPlayer(player, price);
        plugin.getShopManager().updateStock(shopItem, shopItem.getCurrentStock() + amountToSell);
        player.sendMessage(ChatColor.GREEN + "Sold " + amountToSell + " " + material.name() + " for $" + String.format("%.2f", price));
        player.sendTitle(ChatColor.GREEN + "+$" + String.format("%.2f", price), "", 10, 40, 10);

        if (amountToSell < availableToSell) {
            player.sendMessage(ChatColor.YELLOW + "Could not sell " + (availableToSell - amountToSell) + " items because the shop reached max stock.");
        }
    }

    private void sellAll(Player player) {
        int maxStock = plugin.getMaxStock();
        double totalEarned = 0.0;
        int totalItemsSold = 0;

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is == null || is.getType() == Material.AIR) continue;

            ShopItem shopItem = plugin.getShopManager().getItem(is.getType());
            if (shopItem == null) continue;

            if (shopItem.getCurrentStock() >= maxStock) continue;

            int spaceInShop = maxStock - shopItem.getCurrentStock();
            int amountToSell = Math.min(is.getAmount(), spaceInShop);

            if (amountToSell > 0) {
                double price = plugin.getPricingEngine().getExactSellPrice(shopItem, amountToSell);
                totalEarned += price;
                totalItemsSold += amountToSell;

                plugin.getEconomyManager().getEconomy().depositPlayer(player, price);
                plugin.getShopManager().updateStock(shopItem, shopItem.getCurrentStock() + amountToSell);

                if (is.getAmount() <= amountToSell) {
                    player.getInventory().setItem(i, null);
                } else {
                    is.setAmount(is.getAmount() - amountToSell);
                }
            }
        }

        if (totalItemsSold > 0) {
            player.sendMessage(ChatColor.GREEN + "Sold " + totalItemsSold + " items for $" + String.format("%.2f", totalEarned));
            player.sendTitle(ChatColor.GREEN + "+$" + String.format("%.2f", totalEarned), "", 10, 40, 10);
        } else {
            player.sendMessage(ChatColor.RED + "No sellable items found or shop max stock reached.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("hand", "all", "handall", "gui");
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
            Collections.sort(completions);
        }
        return completions;
    }
}
