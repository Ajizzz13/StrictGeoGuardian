package com.zeroends.strictgeoguardian.commands;

import com.zeroends.strictgeoguardian.StrictGeoGuardian;
import com.zeroends.strictgeoguardian.core.AuthManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PassCommand implements CommandExecutor {

    private final AuthManager authManager;

    public PassCommand(AuthManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by a player.").color(NamedTextColor.RED));
            return true;
        }

        if (!authManager.isPlayerInLimbo(player.getUniqueId())) {
            player.sendMessage(Component.text("You do not need to use this command right now.").color(NamedTextColor.GREEN));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /pass <password>").color(NamedTextColor.RED));
            return true;
        }

        String password = args[0];
        
        authManager.handlePasswordInput(player, password);
        
        return true;
    }
}
