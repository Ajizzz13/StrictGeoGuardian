package com.zeroends.strictgeoguardian.core;

import com.zeroends.strictgeoguardian.StrictGeoGuardian;
import com.zeroends.strictgeoguardian.model.AuthStatus;
import com.zeroends.strictgeoguardian.model.GeoData;
import com.zeroends.strictgeoguardian.service.FingerprintService;
import com.zeroends.strictgeoguardian.service.GeoService;
import com.zeroends.strictgeoguardian.storage.IAuthStorage;
import com.zeroends.strictgeoguardian.storage.IDataStorage;
import com.zeroends.strictgeoguardian.util.PasswordUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {

    private final StrictGeoGuardian plugin;
    private final IAuthStorage authStorage;
    private final IDataStorage fingerprintStorage;
    private final GeoService geoService;
    private final FingerprintService fingerprintService;

    private final ConcurrentHashMap<UUID, AuthStatus> playerStatus;
    private final ConcurrentHashMap<UUID, GeoData> pendingGeoData;
    private final ConcurrentHashMap<UUID, Integer> loginAttempts;

    public AuthManager(StrictGeoGuardian plugin, IAuthStorage authStorage, IDataStorage fingerprintStorage, GeoService geoService, FingerprintService fingerprintService) {
        this.plugin = plugin;
        this.authStorage = authStorage;
        this.fingerprintStorage = fingerprintStorage;
        this.geoService = geoService;
        this.fingerprintService = fingerprintService;

        this.playerStatus = new ConcurrentHashMap<>();
        this.pendingGeoData = new ConcurrentHashMap<>();
        this.loginAttempts = new ConcurrentHashMap<>();
    }

    public void setPlayerStatus(UUID uuid, AuthStatus status) {
        if (status == AuthStatus.AUTHENTICATED) {
            playerStatus.remove(uuid);
            loginAttempts.remove(uuid);
        } else {
            playerStatus.put(uuid, status);
        }
    }

    public AuthStatus getPlayerStatus(UUID uuid) {
        return playerStatus.getOrDefault(uuid, AuthStatus.AUTHENTICATED);
    }
    
    public void storePendingGeoData(UUID uuid, GeoData geoData) {
        this.pendingGeoData.put(uuid, geoData);
    }

    public void handlePlayerJoin(Player player) {
        UUID uuid = player.getUniqueId();
        AuthStatus status = playerStatus.getOrDefault(uuid, AuthStatus.PENDING_VERIFICATION);

        if (status == AuthStatus.AUTHENTICATED) {
            return;
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || !isPlayerInLimbo(uuid)) return;

            if (status == AuthStatus.NEEDS_REGISTRATION) {
                player.sendTitle("§a§lSelamat Datang!", "§fKetik §e/pass <password_baru> §funtuk mendaftar", 10, 100, 20);
            } else if (status == AuthStatus.NEEDS_LOGIN) {
                player.sendTitle("§c§lVerifikasi Dibutuhkan", "§fKetik §e/pass <password_anda> §funtuk login", 10, 100, 20);
            } else {
                setPlayerStatus(uuid, AuthStatus.AUTHENTICATED);
            }
        }, 40L); 
    }

    public void handlePasswordInput(Player player, String password) {
        UUID uuid = player.getUniqueId();
        AuthStatus status = getPlayerStatus(uuid);

        if (status == AuthStatus.NEEDS_REGISTRATION) {
            attemptRegistration(player, password);
        } else if (status == AuthStatus.NEEDS_LOGIN) {
            attemptLogin(player, password);
        }
    }

    private void attemptRegistration(Player player, String password) {
        UUID uuid = player.getUniqueId();
        String hash = PasswordUtil.hashPassword(password);
        authStorage.savePasswordHash(uuid, player.getName(), hash).thenRun(() -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                GeoData geoData = pendingGeoData.remove(uuid);
                if (geoData != null) {
                    fingerprintService.createFingerprint(player.getName(), uuid, player.getAddress().getHostString(), geoData)
                            .thenAccept(fingerprintStorage::saveFingerprint);
                }
                setPlayerStatus(uuid, AuthStatus.AUTHENTICATED);
                player.sendTitle("§aRegistrasi Berhasil!", "§7Selamat datang di server!", 10, 70, 20);
            });
        });
    }

    private void attemptLogin(Player player, String password) {
        UUID uuid = player.getUniqueId();
        authStorage.getPasswordHash(uuid).thenAccept(hash -> {
            if (PasswordUtil.checkPassword(password, hash)) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    setPlayerStatus(uuid, AuthStatus.AUTHENTICATED);
                    player.sendTitle("§aVerifikasi Berhasil!", "§7Selamat datang kembali!", 10, 70, 20);
                    
                    GeoData latestGeoData = pendingGeoData.remove(uuid);
                    if (latestGeoData != null) {
                        plugin.getLogger().info("Password correct. Updating fingerprint for " + player.getName() + " to new location.");
                        fingerprintService.createFingerprint(player.getName(), uuid, player.getAddress().getHostString(), latestGeoData)
                                .thenAccept(fingerprintStorage::saveFingerprint);
                    }
                });
            } else {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    int attempts = loginAttempts.getOrDefault(uuid, 0) + 1;
                    loginAttempts.put(uuid, attempts);
                    if (attempts >= 3) {
                        player.kick(Component.text("Terlalu banyak percobaan login. Silakan hubungi staff.").color(NamedTextColor.RED));
                    } else {
                        player.sendTitle("§cPassword Salah!", "§7Sisa percobaan: " + (3 - attempts), 10, 40, 10);
                    }
                });
            }
        });
    }

    public void handlePlayerQuit(Player player) {
        playerStatus.remove(player.getUniqueId());
        pendingGeoData.remove(player.getUniqueId());
        loginAttempts.remove(player.getUniqueId());
    }

    public boolean isPlayerInLimbo(UUID uuid) {
        return playerStatus.containsKey(uuid);
    }
}
