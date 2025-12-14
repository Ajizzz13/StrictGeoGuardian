package com.zeroends.strictgeoguardian.commands;

import com.google.gson.Gson;
import com.zeroends.strictgeoguardian.StrictGeoGuardian;
import com.zeroends.strictgeoguardian.model.Fingerprint;
import com.zeroends.strictgeoguardian.service.VerificationService;
import com.zeroends.strictgeoguardian.storage.IDataStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class IdentityCommand implements CommandExecutor {

    private final StrictGeoGuardian plugin;
    private final IDataStorage fingerprintStorage;
    private final VerificationService verificationService;
    private final Gson gson;

    public IdentityCommand(StrictGeoGuardian plugin, IDataStorage fingerprintStorage, VerificationService verificationService, Gson gson) {
        this.plugin = plugin;
        this.fingerprintStorage = fingerprintStorage;
        this.verificationService = verificationService;
        this.gson = gson;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("strictgeoguardian.admin")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "view":
                handleView(sender, args);
                break;
            case "override":
                handleOverride(sender, args);
                break;
            case "whitelist":
                handleWhitelist(sender, args);
                break;
            case "audit":
                sender.sendMessage(Component.text("Audit command is not yet implemented.").color(NamedTextColor.YELLOW));
                break;
            case "stats":
                sender.sendMessage(Component.text("Stats command is not yet implemented.").color(NamedTextColor.YELLOW));
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleView(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /identity view <player>").color(NamedTextColor.RED));
            return;
        }
        String playerName = args[1];
        fingerprintStorage.loadFingerprint(playerName).thenAccept(fingerprint -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (fingerprint == null) {
                    sender.sendMessage(Component.text("No fingerprint data found for " + playerName).color(NamedTextColor.RED));
                    return;
                }
                String json = gson.toJson(fingerprint);
                sender.sendMessage(Component.text("Fingerprint for " + playerName + ":").color(NamedTextColor.GOLD));
                sender.sendMessage(Component.text(json).color(NamedTextColor.GRAY));
            });
        });
    }

    private void handleOverride(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /identity override <player>").color(NamedTextColor.RED));
            return;
        }
        String playerName = args[1];
        sender.sendMessage(Component.text("Manual override is not yet implemented. Whitelist the player instead.").color(NamedTextColor.YELLOW));
    }

    private void handleWhitelist(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /identity whitelist <add|remove> <player>").color(NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /identity whitelist <add|remove> <player>").color(NamedTextColor.RED));
            return;
        }

        String action = args[1].toLowerCase();
        String playerName = args[2];
        boolean success;

        if (action.equals("add")) {
            success = plugin.getConfigManager().addWhitelist(playerName);
            if (success) {
                sender.sendMessage(Component.text(playerName + " has been added to the whitelist.").color(NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text(playerName + " is already on the whitelist.").color(NamedTextColor.YELLOW));
            }
        } else if (action.equals("remove")) {
            success = plugin.getConfigManager().removeWhitelist(playerName);
            if (success) {
                sender.sendMessage(Component.text(playerName + " has been removed from the whitelist.").color(NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text(playerName + " is not on the whitelist.").color(NamedTextColor.YELLOW));
            }
        } else {
            sender.sendMessage(Component.text("Usage: /identity whitelist <add|remove> <player>").color(NamedTextColor.RED));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("--- StrictGeoGuardian Help ---").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/identity view <player>").color(NamedTextColor.AQUA).append(Component.text(" - View a player's fingerprint.").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/identity override <player>").color(NamedTextColor.AQUA).append(Component.text(" - Manually allow a player.").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/identity whitelist <add|remove> <player>").color(NamedTextColor.AQUA).append(Component.text(" - Manage player whitelist.").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/identity audit <player>").color(NamedTextColor.AQUA).append(Component.text(" - View verification history.").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/identity stats").color(NamedTextColor.AQUA).append(Component.text(" - View plugin performance statistics.").color(NamedTextColor.GRAY)));
    }
}
