package com.zeroends.strictgeoguardian.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zeroends.strictgeoguardian.StrictGeoGuardian;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class JsonAuthStorage implements IAuthStorage {

    private final StrictGeoGuardian plugin;
    private final File authFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private ConcurrentHashMap<UUID, AuthData> authCache = new ConcurrentHashMap<>();

    private static class AuthData {
        String playerName;
        String passwordHash;
        AuthData(String playerName, String passwordHash) {
            this.playerName = playerName;
            this.passwordHash = passwordHash;
        }
    }

    public JsonAuthStorage(StrictGeoGuardian plugin) {
        this.plugin = plugin;
        this.authFile = new File(plugin.getDataFolder(), "authentication.json");
        loadDataAsync();
    }

    private CompletableFuture<Void> loadDataAsync() {
        return CompletableFuture.runAsync(() -> {
            if (authFile.exists()) {
                try (FileReader reader = new FileReader(authFile)) {
                    Type type = new TypeToken<ConcurrentHashMap<UUID, AuthData>>(){}.getType();
                    authCache = gson.fromJson(reader, type);
                    if (authCache == null) {
                        authCache = new ConcurrentHashMap<>();
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Could not load authentication data: " + e.getMessage());
                    authCache = new ConcurrentHashMap<>();
                }
            }
        });
    }

    private CompletableFuture<Void> saveDataAsync() {
        return CompletableFuture.runAsync(() -> {
            try (FileWriter writer = new FileWriter(authFile)) {
                gson.toJson(authCache, writer);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save authentication data: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> isPlayerRegistered(UUID uuid) {
        return CompletableFuture.completedFuture(authCache.containsKey(uuid));
    }

    @Override
    public CompletableFuture<String> getPasswordHash(UUID uuid) {
        AuthData data = authCache.get(uuid);
        return CompletableFuture.completedFuture(data != null ? data.passwordHash : null);
    }

    @Override
    public CompletableFuture<Void> savePasswordHash(UUID uuid, String playerName, String hash) {
        authCache.put(uuid, new AuthData(playerName, hash));
        return saveDataAsync();
    }

    @Override
    public CompletableFuture<Void> removePlayer(UUID uuid) {
        authCache.remove(uuid);
        return saveDataAsync();
    }
}
