package com.gunshiz.dynamicshop.commands;

import com.gunshiz.dynamicshop.*;
import com.gunshiz.dynamicshop.managers.*;
import com.gunshiz.dynamicshop.listeners.*;
import com.gunshiz.dynamicshop.utils.*;
import com.gunshiz.dynamicshop.models.*;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ShopAdminCommand implements TabExecutor {

    private final DynamicShopPlugin plugin;
    private final Random random = new Random();

    public ShopAdminCommand(DynamicShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dynamicshop.admin.shop")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "=== DynamicShop Admin ===");
            sender.sendMessage(ChatColor.YELLOW + "/sadm setstock <item> <stock>");
            sender.sendMessage(ChatColor.YELLOW + "/sadm setbase <item> <base>");
            sender.sendMessage(ChatColor.YELLOW + "/sadm randomstock [item]");
            sender.sendMessage(ChatColor.YELLOW + "/sadm additem [item] [base]");
            sender.sendMessage(ChatColor.YELLOW + "/sadm removeitem [item]");
            sender.sendMessage(ChatColor.YELLOW + "/sadm populate");
            sender.sendMessage(ChatColor.YELLOW + "/sadm reload");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.YELLOW + "Reloading config and database...");
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    plugin.getDatabaseManager().disconnect();
                    plugin.getDatabaseManager().connect();
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        plugin.getShopManager().loadItems();
                        sender.sendMessage(ChatColor.GREEN + "DynamicShop config and database reloaded!");
                    });
                } catch (Exception e) {
                    plugin.getLogger().severe("Error during reload: " + e.getMessage());
                    sender.sendMessage(ChatColor.RED + "Error during reload. Check console.");
                }
            });
            return true;
        }

        if (sub.equals("populate")) {
            sender.sendMessage(ChatColor.YELLOW + "Populating shop with survival items... This may take a moment.");
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                java.util.Map<Material, Double> itemsToAdd = new java.util.HashMap<>();
                double defaultBase = 50.0;
                
                for (Material mat : Material.values()) {
                    if (!mat.isItem() || mat.isAir()) continue;
                    
                    String name = mat.name();
                    if (name.contains("COMMAND_BLOCK") || name.contains("STRUCTURE_VOID") || 
                        name.contains("BEDROCK") || name.contains("BARRIER") || 
                        name.contains("JIGSAW") || name.contains("SPAWNER") ||
                        name.contains("LIGHT") || name.contains("DEBUG_STICK") ||
                        name.contains("KNOWLEDGE_BOOK")) {
                        continue;
                    }
                    
                    if (plugin.getShopManager().getItem(mat) == null) {
                        itemsToAdd.put(mat, defaultBase);
                    }
                }
                
                if (!itemsToAdd.isEmpty()) {
                    plugin.getDatabaseManager().addShopItemsBatch(itemsToAdd);
                }
                
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getShopManager().loadItems();
                    sender.sendMessage(ChatColor.GREEN + "Populated shop with " + itemsToAdd.size() + " new items! (Default base price: $50.0)");
                });
            });
            return true;
        }

        if (sub.equals("setstock")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /sadm setstock <item> <stock>");
                return true;
            }
            Material mat = Material.matchMaterial(args[1]);
            if (mat == null) {
                sender.sendMessage(ChatColor.RED + "Invalid material.");
                return true;
            }
            int stock;
            try {
                stock = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Stock must be a number.");
                return true;
            }
            ShopItem item = plugin.getShopManager().getItem(mat);
            if (item == null) {
                sender.sendMessage(ChatColor.RED + "Item not found in shop database.");
                return true;
            }
            plugin.getShopManager().updateStock(item, stock);
            sender.sendMessage(ChatColor.GREEN + "Stock for " + mat.name() + " set to " + stock);
            return true;
        }

        if (sub.equals("setbase")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /sadm setbase <item> <base>");
                return true;
            }
            Material mat = Material.matchMaterial(args[1]);
            if (mat == null) {
                sender.sendMessage(ChatColor.RED + "Invalid material.");
                return true;
            }
            double base;
            try {
                base = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Base price must be a number.");
                return true;
            }
            ShopItem item = plugin.getShopManager().getItem(mat);
            if (item == null) {
                sender.sendMessage(ChatColor.RED + "Item not found in shop database.");
                return true;
            }
            
            // Actually ShopManager doesn't expose the map directly, but we can update it in db and reload
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().updateBasePrice(mat, base);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getShopManager().loadItems();
                    sender.sendMessage(ChatColor.GREEN + "Base price for " + mat.name() + " set to " + base);
                });
            });
            return true;
        }

        if (sub.equals("randomstock")) {
            int maxStock = plugin.getMaxStock();
            if (args.length >= 2) {
                Material mat = Material.matchMaterial(args[1]);
                if (mat == null) {
                    sender.sendMessage(ChatColor.RED + "Invalid material.");
                    return true;
                }
                ShopItem item = plugin.getShopManager().getItem(mat);
                if (item == null) {
                    sender.sendMessage(ChatColor.RED + "Item not found in shop database.");
                    return true;
                }
                int rStock = random.nextInt(maxStock + 1);
                plugin.getShopManager().updateStock(item, rStock);
                sender.sendMessage(ChatColor.GREEN + "Random stock for " + mat.name() + " set to " + rStock);
            } else {
                List<ShopItem> items = plugin.getShopManager().getItems();
                if (items.isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "No items in shop.");
                    return true;
                }
                ShopItem item = items.get(random.nextInt(items.size()));
                int rStock = random.nextInt(maxStock + 1);
                plugin.getShopManager().updateStock(item, rStock);
                sender.sendMessage(ChatColor.GREEN + "Randomized stock for random item (" + item.getMaterial().name() + ") to " + rStock);
            }
            return true;
        }

        if (sub.equals("additem")) {
            Material mat = null;
            double basePrice = 10.0;
            
            if (args.length == 2) {
                try {
                    basePrice = Double.parseDouble(args[1]);
                    if (sender instanceof org.bukkit.entity.Player) {
                        mat = ((org.bukkit.entity.Player) sender).getInventory().getItemInMainHand().getType();
                    }
                } catch (NumberFormatException e) {
                    mat = Material.matchMaterial(args[1]);
                }
            } else if (args.length >= 3) {
                mat = Material.matchMaterial(args[1]);
                try {
                    basePrice = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid base price.");
                    return true;
                }
            } else if (sender instanceof org.bukkit.entity.Player) {
                mat = ((org.bukkit.entity.Player) sender).getInventory().getItemInMainHand().getType();
            }

            if (mat == null || mat == Material.AIR) {
                sender.sendMessage(ChatColor.RED + "Invalid material or no item in hand.");
                return true;
            }
            if (plugin.getShopManager().getItem(mat) != null) {
                sender.sendMessage(ChatColor.RED + "Item is already in the shop.");
                return true;
            }
            
            Material finalMat = mat;
            double finalBase = basePrice;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().addShopItem(finalMat, finalBase);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getShopManager().loadItems();
                    sender.sendMessage(ChatColor.GREEN + "Added " + finalMat.name() + " to the shop with base price $" + finalBase + " and 0 stock.");
                });
            });
            return true;
        }

        if (sub.equals("removeitem")) {
            Material mat = null;
            if (args.length >= 2) {
                mat = Material.matchMaterial(args[1]);
            } else if (sender instanceof org.bukkit.entity.Player) {
                mat = ((org.bukkit.entity.Player) sender).getInventory().getItemInMainHand().getType();
            }
            if (mat == null || mat == Material.AIR) {
                sender.sendMessage(ChatColor.RED + "Invalid material or no item in hand.");
                return true;
            }
            if (plugin.getShopManager().getItem(mat) == null) {
                sender.sendMessage(ChatColor.RED + "Item is not in the shop.");
                return true;
            }

            Material finalMat = mat;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().removeShopItem(finalMat);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getShopManager().loadItems();
                    sender.sendMessage(ChatColor.GREEN + "Removed " + finalMat.name() + " from the shop.");
                });
            });
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Unknown sub-command.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("dynamicshop.admin.shop")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("setstock", "setbase", "randomstock", "reload", "additem", "removeitem", "populate");
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("setstock") || sub.equals("setbase") || sub.equals("randomstock") || sub.equals("removeitem")) {
                List<String> shopItems = plugin.getShopManager().getItems().stream()
                        .map(item -> item.getMaterial().name())
                        .collect(Collectors.toList());
                StringUtil.copyPartialMatches(args[1], shopItems, completions);
            } else if (sub.equals("additem")) {
                List<String> allMaterials = Arrays.stream(Material.values())
                        .filter(Material::isItem)
                        .map(Material::name)
                        .collect(Collectors.toList());
                StringUtil.copyPartialMatches(args[1], allMaterials, completions);
            }
        }
        Collections.sort(completions);
        return completions;
    }
}
