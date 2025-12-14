package com.zeroends.strictgeoguardian.storage;

import com.google.gson.Gson;
import com.zeroends.strictgeoguardian.StrictGeoGuardian;
import com.zeroends.strictgeoguardian.model.Fingerprint;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class JsonStorage implements IDataStorage {

    private final StrictGeoGuardian plugin;
    private final File dataFolder;
    private final Gson gson;

    public JsonStorage(StrictGeoGuardian plugin, Gson gson) {
        this.plugin = plugin;
        this.gson = gson;
        this.dataFolder = new File(plugin.getDataFolder(), "fingerprints");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    private File getPlayerFile(String playerName) {
        return new File(dataFolder, playerName.toLowerCase() + ".json");
    }
    
    private File getPlayerFile(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".json");
    }

    @Override
    public CompletableFuture<Void> saveFingerprint(Fingerprint fingerprint) {
        return CompletableFuture.runAsync(() -> {
            File playerFile = getPlayerFile(fingerprint.playerName());
            File uuidFile = getPlayerFile(fingerprint.javaUuid());

            try (FileWriter writer = new FileWriter(playerFile)) {
                gson.toJson(fingerprint, writer);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save fingerprint for " + fingerprint.playerName() + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
            
            try {
                if(uuidFile.exists()) uuidFile.delete();
                Files.createSymbolicLink(uuidFile.toPath(), playerFile.toPath());
            } catch (Exception e) {
                plugin.getLogger().warning("Could not create UUID symlink for " + fingerprint.playerName() + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Fingerprint> loadFingerprint(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            File playerFile = getPlayerFile(playerName);
            if (!playerFile.exists()) {
                return null;
            }

            try (FileReader reader = new FileReader(playerFile)) {
                return gson.fromJson(reader, Fingerprint.class);
            } catch (Exception e) {
                plugin.getLogger().severe("Could not load fingerprint for " + playerName + ": " + e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Fingerprint> loadFingerprintByUuid(UUID uuid) {
         return CompletableFuture.supplyAsync(() -> {
            File playerFile = getPlayerFile(uuid);
            if (!playerFile.exists()) {
                return null;
            }

            try (FileReader reader = new FileReader(playerFile)) {
                return gson.fromJson(reader, Fingerprint.class);
            } catch (Exception e) {
                plugin.getLogger().severe("Could not load fingerprint for UUID " + uuid + ": " + e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteFingerprint(String playerName) {
        return CompletableFuture.runAsync(() -> {
            try {
                Fingerprint fp = loadFingerprint(playerName).join();
                if(fp != null) {
                    File uuidFile = getPlayerFile(fp.javaUuid());
                    if(uuidFile.exists()) Files.delete(uuidFile.toPath());
                }
                File playerFile = getPlayerFile(playerName);
                if (playerFile.exists()) {
                    Files.delete(playerFile.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not delete fingerprint for " + playerName + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<List<Fingerprint>> getAllFingerprints() {
        return CompletableFuture.supplyAsync(() -> {
            List<Fingerprint> fingerprints = new ArrayList<>();
            try (Stream<Path> paths = Files.walk(dataFolder.toPath())) {
                paths.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".json"))
                     .filter(path -> !Files.isSymbolicLink(path))
                     .forEach(path -> {
                         try (FileReader reader = new FileReader(path.toFile())) {
                             Fingerprint fp = gson.fromJson(reader, Fingerprint.class);
                             if (fp != null) {
                                 fingerprints.add(fp);
                             }
                         } catch (Exception e) {
                             plugin.getLogger().warning("Failed to load fingerprint file: " + path + ": " + e.getMessage());
                         }
                     });
            } catch (IOException e) {
                plugin.getLogger().severe("Could not read fingerprint data folder: " + e.getMessage());
            }
            return fingerprints;
        });
    }
}
