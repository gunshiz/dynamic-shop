package com.gunshiz.dynamicshop.listeners;

import com.gunshiz.dynamicshop.*;
import com.gunshiz.dynamicshop.managers.*;
import com.gunshiz.dynamicshop.listeners.*;
import com.gunshiz.dynamicshop.utils.*;
import com.gunshiz.dynamicshop.models.*;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopGUIListener implements Listener {

    private final DynamicShopPlugin plugin;
    public static final Map<UUID, SortType> playerSort = new HashMap<>();
    public static final Map<UUID, String> playerSearch = new HashMap<>();
    public static final Map<UUID, Integer> playerPage = new HashMap<>();

    public ShopGUIListener(DynamicShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTitle().contains("Dynamic Shop")) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();

            ItemStack currentItem = event.getCurrentItem();
            if (currentItem == null || currentItem.getType() == Material.AIR) return;

            if (event.getRawSlot() == 45 && currentItem.getType() == Material.ARROW) {
                int page = playerPage.getOrDefault(player.getUniqueId(), 0);
                if (page > 0) {
                    page--;
                    playerPage.put(player.getUniqueId(), page);
                    String search = playerSearch.get(player.getUniqueId());
                    SortType sort = playerSort.getOrDefault(player.getUniqueId(), SortType.NAME_ASC);
                    GUIUtils.openShop(plugin, player, search, sort, page);
                }
                return;
            }

            if (event.getRawSlot() == 53 && currentItem.getType() == Material.ARROW) {
                int page = playerPage.getOrDefault(player.getUniqueId(), 0);
                page++;
                playerPage.put(player.getUniqueId(), page);
                String search = playerSearch.get(player.getUniqueId());
                SortType sort = playerSort.getOrDefault(player.getUniqueId(), SortType.NAME_ASC);
                GUIUtils.openShop(plugin, player, search, sort, page);
                return;
            }

            if (event.getRawSlot() == 48 && currentItem.getType() == Material.HOPPER) {
                SortType currentSort = playerSort.getOrDefault(player.getUniqueId(), SortType.NAME_ASC);
                SortType nextSort = currentSort.next();
                playerSort.put(player.getUniqueId(), nextSort);
                String currentSearch = playerSearch.get(player.getUniqueId());
                int page = playerPage.getOrDefault(player.getUniqueId(), 0);
                GUIUtils.openShop(plugin, player, currentSearch, nextSort, page);
                return;
            }

            if (event.getRawSlot() == 49 && currentItem.getType() == Material.NAME_TAG) {
                if (event.getClick() == ClickType.RIGHT) {
                    playerSearch.remove(player.getUniqueId());
                    playerPage.put(player.getUniqueId(), 0);
                    SortType sort = playerSort.getOrDefault(player.getUniqueId(), SortType.NAME_ASC);
                    GUIUtils.openShop(plugin, player, null, sort, 0);
                } else {
                    player.closeInventory();
                    
                    if (plugin.getServer().getPluginManager().isPluginEnabled("floodgate") && org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                        org.geysermc.cumulus.form.CustomForm form = org.geysermc.cumulus.form.CustomForm.builder()
                            .title("Search Items")
                            .input("Value", "Enter search here...")
                            .validResultHandler(response -> {
                                String query = response.asInput(0);
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    playerSearch.put(player.getUniqueId(), query);
                                    playerPage.put(player.getUniqueId(), 0);
                                    SortType sort = playerSort.getOrDefault(player.getUniqueId(), SortType.NAME_ASC);
                                    GUIUtils.openShop(plugin, player, query, sort, 0);
                                });
                            })
                            .closedOrInvalidResultHandler(response -> {
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    SortType sort = playerSort.getOrDefault(player.getUniqueId(), SortType.NAME_ASC);
                                    int page = playerPage.getOrDefault(player.getUniqueId(), 0);
                                    GUIUtils.openShop(plugin, player, playerSearch.get(player.getUniqueId()), sort, page);
                                });
                            })
                            .build();
                        
                        org.geysermc.floodgate.api.FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
                    } else {
                        new net.wesjd.anvilgui.AnvilGUI.Builder()
                            .onClose(stateSnapshot -> {
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    String search = playerSearch.get(player.getUniqueId());
                                    SortType sort = playerSort.getOrDefault(player.getUniqueId(), SortType.NAME_ASC);
                                    int page = playerPage.getOrDefault(player.getUniqueId(), 0);
                                    GUIUtils.openShop(plugin, player, search, sort, page);
                                });
                            })
                            .onClick((slot, stateSnapshot) -> {
                                if(slot != net.wesjd.anvilgui.AnvilGUI.Slot.OUTPUT) {
                                    return java.util.Collections.emptyList();
                                }
                                String query = stateSnapshot.getText();
                                return java.util.Collections.singletonList(
                                    net.wesjd.anvilgui.AnvilGUI.ResponseAction.run(() -> {
                                        playerSearch.put(player.getUniqueId(), query);
                                        playerPage.put(player.getUniqueId(), 0);
                                        SortType sort = playerSort.getOrDefault(player.getUniqueId(), SortType.NAME_ASC);
                                        GUIUtils.openShop(plugin, player, query, sort, 0);
                                    })
                                );
                            })
                            .text("Value")
                            .itemLeft(new ItemStack(Material.PAPER))
                            .title("Search Items")
                            .plugin(plugin)
                            .open(player);
                    }
                }
                return;
            }

            ShopItem shopItem = plugin.getShopManager().getItem(currentItem.getType());
            if (shopItem != null) {
                int transactionAmount = 1;
                boolean isBuy = false;
                boolean isSell = false;
                
                if (event.getClick() == ClickType.LEFT) {
                    isBuy = true;
                } else if (event.getClick() == ClickType.SHIFT_LEFT) {
                    isBuy = true;
                    transactionAmount = 64;
                } else if (event.getClick() == ClickType.RIGHT) {
                    isSell = true;
                } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                    isSell = true;
                    transactionAmount = 64;
                }
                
                if (!isBuy && !isSell) return;
                
                if (isBuy) {
                    double price = plugin.getPricingEngine().getExactBuyPrice(shopItem, transactionAmount);
                    if (!plugin.getEconomyManager().getEconomy().has(player, price)) {
                        player.sendMessage(ChatColor.RED + "You don't have enough money!");
                        return;
                    }
                    
                    if (shopItem.getCurrentStock() < transactionAmount) {
                        player.sendMessage(ChatColor.RED + "Not enough stock!");
                        return;
                    }

                    int freeSpace = 0;
                    for (ItemStack is : player.getInventory().getStorageContents()) {
                        if (is == null || is.getType() == Material.AIR) {
                            freeSpace += shopItem.getMaterial().getMaxStackSize();
                        } else if (is.getType() == shopItem.getMaterial() && is.getAmount() < is.getMaxStackSize()) {
                            freeSpace += (is.getMaxStackSize() - is.getAmount());
                        }
                    }
                    
                    if (freeSpace < transactionAmount) {
                        player.sendMessage(ChatColor.RED + "Your inventory is full!");
                        return;
                    }

                    plugin.getEconomyManager().getEconomy().withdrawPlayer(player, price);
                    player.getInventory().addItem(new ItemStack(shopItem.getMaterial(), transactionAmount));
                    plugin.getShopManager().updateStock(shopItem, shopItem.getCurrentStock() - transactionAmount);
                    player.sendMessage(ChatColor.GREEN + "Bought " + transactionAmount + " " + shopItem.getMaterial().name() + " for $" + String.format("%.2f", price));
                } else {
                    int count = 0;
                    for (ItemStack is : player.getInventory().getContents()) {
                        if (is != null && is.getType() == shopItem.getMaterial()) count += is.getAmount();
                    }
                    if (count < transactionAmount) {
                        player.sendMessage(ChatColor.RED + "You don't have enough items to sell!");
                        return;
                    }

                    int maxStock = plugin.getMaxStock();
                    if (shopItem.getCurrentStock() + transactionAmount > maxStock) {
                        player.sendMessage(ChatColor.RED + "The shop cannot buy this many items (max stock reached)!");
                        return;
                    }

                    double price = plugin.getPricingEngine().getExactSellPrice(shopItem, transactionAmount);
                    int remainingToRemove = transactionAmount;
                    ItemStack[] contents = player.getInventory().getContents();
                    for (int i = 0; i < contents.length; i++) {
                        ItemStack is = contents[i];
                        if (is != null && is.getType() == shopItem.getMaterial()) {
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
                    plugin.getEconomyManager().getEconomy().depositPlayer(player, price);
                    plugin.getShopManager().updateStock(shopItem, shopItem.getCurrentStock() + transactionAmount);
                    player.sendMessage(ChatColor.GREEN + "Sold " + transactionAmount + " " + shopItem.getMaterial().name() + " for $" + String.format("%.2f", price));
                    player.sendTitle(ChatColor.GREEN + "+$" + String.format("%.2f", price), "", 10, 40, 10);
                }
                
                // Refresh shop to show updated stock and prices
                String search = playerSearch.get(player.getUniqueId());
                SortType sort = playerSort.getOrDefault(player.getUniqueId(), SortType.NAME_ASC);
                int page = playerPage.getOrDefault(player.getUniqueId(), 0);
                GUIUtils.openShop(plugin, player, search, sort, page);
            }
        }
    }

    @EventHandler
    public void onClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (event.getView().getTitle().equals("Drop items to sell")) {
            if (!(event.getPlayer() instanceof Player)) return;
            Player player = (Player) event.getPlayer();
            
            org.bukkit.inventory.Inventory gui = event.getInventory();
            int maxStock = plugin.getMaxStock();
            double totalEarned = 0.0;
            int totalItemsSold = 0;
            boolean returnedItems = false;
            
            for (int i = 0; i < gui.getSize(); i++) {
                ItemStack is = gui.getItem(i);
                if (is == null || is.getType() == Material.AIR) continue;
                
                ShopItem shopItem = plugin.getShopManager().getItem(is.getType());
                if (shopItem == null || shopItem.getCurrentStock() >= maxStock) {
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(is);
                    if (!leftover.isEmpty()) {
                        for (ItemStack drop : leftover.values()) {
                            player.getWorld().dropItem(player.getLocation(), drop);
                        }
                    }
                    returnedItems = true;
                    gui.setItem(i, null);
                    continue;
                }
                
                int spaceInShop = maxStock - shopItem.getCurrentStock();
                int amountToSell = Math.min(is.getAmount(), spaceInShop);
                
                if (amountToSell > 0) {
                    double price = plugin.getPricingEngine().getExactSellPrice(shopItem, amountToSell);
                    totalEarned += price;
                    totalItemsSold += amountToSell;
                    
                    plugin.getShopManager().updateStock(shopItem, shopItem.getCurrentStock() + amountToSell);
                    
                    if (is.getAmount() <= amountToSell) {
                        gui.setItem(i, null);
                    } else {
                        is.setAmount(is.getAmount() - amountToSell);
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(is);
                        if (!leftover.isEmpty()) {
                            for (ItemStack drop : leftover.values()) {
                                player.getWorld().dropItem(player.getLocation(), drop);
                            }
                        }
                        returnedItems = true;
                        gui.setItem(i, null);
                    }
                }
            }
            
            if (totalEarned > 0) {
                plugin.getEconomyManager().getEconomy().depositPlayer(player, totalEarned);
                player.sendMessage(ChatColor.GREEN + "Sold " + totalItemsSold + " items from the GUI for $" + String.format("%.2f", totalEarned));
                player.sendTitle(ChatColor.GREEN + "+$" + String.format("%.2f", totalEarned), "", 10, 40, 10);
            }
            if (returnedItems) {
                player.sendMessage(ChatColor.YELLOW + "Some items couldn't be sold (max stock or invalid) and were returned to you.");
            }
        }
    }
}
