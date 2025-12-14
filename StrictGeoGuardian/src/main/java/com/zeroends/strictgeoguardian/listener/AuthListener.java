package com.zeroends.strictgeoguardian.listener;

import com.zeroends.strictgeoguardian.StrictGeoGuardian;
import com.zeroends.strictgeoguardian.core.AuthManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class AuthListener implements Listener {

    private final StrictGeoGuardian plugin;
    private final AuthManager authManager;

    public AuthListener(StrictGeoGuardian plugin, AuthManager authManager) {
        this.plugin = plugin;
        this.authManager = authManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        authManager.handlePlayerJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        authManager.handlePlayerQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (authManager.isPlayerInLimbo(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("Harap selesaikan verifikasi.").color(NamedTextColor.YELLOW));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncChatEvent event) {
        if (authManager.isPlayerInLimbo(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("Anda tidak bisa chat sebelum menyelesaikan verifikasi.").color(NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (authManager.isPlayerInLimbo(player.getUniqueId())) {
            String[] args = event.getMessage().split(" ");
            String command = args[0];

            // --- PERBAIKAN KEAMANAN (MENCEGAH LOGGING) ---
            if (command.equalsIgnoreCase("/pass")) {
                event.setCancelled(true); // <-- Membatalkan command agar tidak ter-log
                
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /pass <password>").color(NamedTextColor.RED));
                    return;
                }
                
                String password = args[1];
                // Memanggil AuthManager secara manual dan aman
                authManager.handlePasswordInput(player, password);
                
            } else {
                event.setCancelled(true);
                player.sendMessage(Component.text("Anda hanya bisa menggunakan /pass saat ini.").color(NamedTextColor.RED));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (authManager.isPlayerInLimbo(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (authManager.isPlayerInLimbo(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
