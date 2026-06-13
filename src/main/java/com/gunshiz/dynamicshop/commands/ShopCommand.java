package com.gunshiz.dynamicshop.commands;

import com.gunshiz.dynamicshop.*;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    private final DynamicShopPlugin plugin;

    public ShopCommand(DynamicShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        ShopGUIListener.playerSort.put(player.getUniqueId(), SortType.NAME_ASC);
        ShopGUIListener.playerSearch.remove(player.getUniqueId());
        ShopGUIListener.playerPage.put(player.getUniqueId(), 0);
        GUIUtils.openShop(plugin, player, null, SortType.NAME_ASC, 0);
        return true;
    }
}
