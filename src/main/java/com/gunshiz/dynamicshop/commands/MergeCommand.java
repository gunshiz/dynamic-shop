package com.gunshiz.dynamicshop.commands;

import com.gunshiz.dynamicshop.*;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MergeCommand implements CommandExecutor {

    private final DynamicShopPlugin plugin;

    public MergeCommand(DynamicShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dynamicshop.admin.merge")) {
            sender.sendMessage("You don't have permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("Usage: /merge <filename.yml>");
            return true;
        }

        String filename = args[0];
        sender.sendMessage("Starting merge of " + filename + " in the background...");
        plugin.getShopManager().migrateShopData(sender, filename);
        return true;
    }
}
