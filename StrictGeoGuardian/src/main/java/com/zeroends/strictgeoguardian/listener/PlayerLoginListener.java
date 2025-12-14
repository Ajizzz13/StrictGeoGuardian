package com.zeroends.strictgeoguardian.listener;

import com.zeroends.strictgeoguardian.StrictGeoGuardian;
import com.zeroends.strictgeoguardian.core.AuthManager;
import com.zeroends.strictgeoguardian.model.AuthStatus;
import com.zeroends.strictgeoguardian.model.VerificationResult;
import com.zeroends.strictgeoguardian.service.VerificationService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.UUID;

public class PlayerLoginListener implements Listener {

    private final StrictGeoGuardian plugin;
    private final VerificationService verificationService;
    private final AuthManager authManager;

    public PlayerLoginListener(StrictGeoGuardian plugin, VerificationService verificationService, AuthManager authManager) {
        this.plugin = plugin;
        this.verificationService = verificationService;
        this.authManager = authManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        final String playerName = event.getName();
        final UUID playerUuid = event.getUniqueId();
        final String ipAddress = event.getAddress().getHostAddress();

        if (playerName == null || playerUuid == null || ipAddress == null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text("Invalid login data. Please try again.").color(NamedTextColor.RED));
            return;
        }

        plugin.getLogger().info("Processing login for " + playerName + " [" + ipAddress + "]");

        VerificationResult result = verificationService.verifyPlayer(playerName, playerUuid, ipAddress);

        if (!result.isAllowed()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, result.getKickMessage());
            plugin.getLogger().warning("DENIED login for " + playerName + ". Reason: " + result.getLogMessage());
        } else {
            if (result.getAuthStatus() != AuthStatus.AUTHENTICATED) {
                authManager.setPlayerStatus(playerUuid, result.getAuthStatus());
            }
            
            if (result.isLoggable()) {
                plugin.getLogger().info("ALLOWED login for " + playerName + ". Status: " + result.getLogMessage());
            } else {
                plugin.getLogger().info("ALLOWED login for " + playerName + ". Status: High confidence match.");
            }
            
            event.allow();
        }
    }
}
