package com.zeroends.nameguard.manager;

import com.zeroends.nameguard.NameGuard;
import com.zeroends.nameguard.model.AccountType;
import com.zeroends.nameguard.model.Binding;
import com.zeroends.nameguard.model.Fingerprint;
import com.zeroends.nameguard.model.LoginResult;
import com.zeroends.nameguard.storage.IStorage;
import com.zeroends.nameguard.util.NormalizationUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the core logic of creating, verifying, and persisting identity bindings.
 * PATCH: Always flush new fingerprint to disk immediately after soft/hard allow and always reload binding from disk on login verification.
 */
public class BindingManager {

    private final NameGuard plugin;
    private final IStorage storage;
    private final NormalizationUtil normalizationUtil;
    private final FingerprintManager fingerprintManager;
    private final ConfigManager configManager;

    private final Map<String, Object> bindingCache = new ConcurrentHashMap<>();

    public BindingManager(NameGuard plugin,
                          IStorage storage,
                          NormalizationUtil normalizationUtil,
                          FingerprintManager fingerprintManager) {
        this.plugin = plugin;
        this.storage = storage;
        this.normalizationUtil = normalizationUtil;
        this.fingerprintManager = fingerprintManager;
        this.configManager = plugin.getConfigManager();
    }

    public void saveCacheToDisk() {
        plugin.getSLF4JLogger().info("Saving {} cached bindings to storage...", bindingCache.size());
        long purgeMillis = configManager.getFingerprintPurgeMillis();

        for (Object obj : bindingCache.values()) {
            if (obj instanceof Binding binding) {
                try {
                    if (purgeMillis > 0) {
                        binding.purgeOldFingerprints(purgeMillis, 1);
                    }
                    storage.saveBinding(binding);
                } catch (IOException e) {
                    plugin.getSLF4JLogger().error("Failed to save binding for: {}", binding.getNormalizedName(), e);
                }
            }
        }
    }

    @NotNull
    public LoginResult verifyLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        String originalName = event.getName();
        String normalizedName = normalizationUtil.normalizeName(originalName);

        // Always reload from disk for the latest data!
        Binding binding = loadBinding(normalizedName).orElse(null);

        Fingerprint newFingerprint = fingerprintManager.createFingerprint(event);
        if (binding == null) {
            // Handle new binding creation
            binding = createNewBinding(normalizedName, originalName, newFingerprint);
            return new LoginResult.Allowed(binding, true, false);
        }

        binding.updateLastSeen();
        return processFingerprintMatches(originalName, binding, newFingerprint);
    }

    @NotNull
    private Optional<Binding> loadBinding(@NotNull String normalizedName) {
        try {
            Optional<Binding> fromDisk = storage.loadBinding(normalizedName);
            fromDisk.ifPresent(binding -> bindingCache.put(normalizedName, binding));
            return fromDisk;
        } catch (Exception e) {
            plugin.getSLF4JLogger().error("I/O error when loading binding for {}: {}", normalizedName, e.getMessage());
            return Optional.empty();
        }
    }

    private Binding createNewBinding(String normalizedName, String originalName, Fingerprint newFingerprint) {
        String preferredName = stripLegacyPrefix(originalName);
        plugin.getSLF4JLogger().info(
                "Creating new binding for '{}': preferredName={}, normalized={}",
                originalName, preferredName, normalizedName
        );
        Binding binding = new Binding(normalizedName, preferredName, newFingerprint.getEdition(), newFingerprint);
        saveBinding(binding);
        return binding;
    }

    private LoginResult processFingerprintMatches(String playerName, Binding binding, Fingerprint newFingerprint) {
        int highestScore = -1;
        boolean hardMatch = false;

        if (binding.getFingerprints().isEmpty()) {
            plugin.getSLF4JLogger().warn("Binding for {} has no fingerprints. Allowing and adding new one.", playerName);
            binding.addFingerprint(newFingerprint, configManager.getRollingFpLimit());
            saveBinding(binding);
            return new LoginResult.Allowed(binding, false, true);
        }

        for (Fingerprint oldFingerprint : binding.getFingerprints()) {
            FingerprintManager.SimilarityResult result = fingerprintManager.getSimilarityDetailed(newFingerprint, oldFingerprint);
            if (result.score() > highestScore) {
                highestScore = result.score();
            }
            if (result.isHardMatch()) {
                hardMatch = true;
                break; // A hard match is definitive, no need to check others.
            }
        }

        boolean softAllow = highestScore >= configManager.getScoreSoftAllow();
        boolean hardAllow = hardMatch || highestScore >= configManager.getScoreHardAllow();

        if (hardAllow || softAllow) {
            // Player is recognized, add the new fingerprint to their binding
            binding.addFingerprint(newFingerprint, configManager.getRollingFpLimit());
            // Update preferred name casing if it has changed
            if (!Objects.equals(stripLegacyPrefix(playerName), binding.getPreferredName())) {
                binding.setPreferredName(stripLegacyPrefix(playerName));
            }
            saveBinding(binding);
            return new LoginResult.Allowed(binding, false, !hardAllow); // isSoftMatch is true if not a hard allow
        } else {
            // Score is too low, deny login
            return new LoginResult.Denied(LoginResult.Reason.HARD_MISMATCH, configManager.getKickMessage("hardMismatch"));
        }
    }

    public Optional<Binding> getBinding(@NotNull String normalizedName) {
        Object cached = bindingCache.get(normalizedName);
        if (cached instanceof Binding) {
            return Optional.of((Binding) cached);
        }
        if (cached != null) { // Placeholder for not found
            return Optional.empty();
        }
        return loadBinding(normalizedName);
    }

    public void saveBinding(@NotNull Binding binding) {
        bindingCache.put(binding.getNormalizedName(), binding);
        try {
            storage.saveBinding(binding);
        } catch (IOException e) {
            plugin.getSLF4JLogger().error("Failed to save binding for {}", binding.getNormalizedName(), e);
        }
    }

    public boolean removeBinding(@NotNull String normalizedName) {
        bindingCache.remove(normalizedName);
        try {
            storage.removeBinding(normalizedName);
            return true;
        } catch (IOException e) {
            plugin.getSLF4JLogger().error("Failed to remove binding for {}", normalizedName, e);
            return false;
        }
    }

    public void reloadBindings() {
        saveCacheToDisk();
        bindingCache.clear();
        plugin.getSLF4JLogger().info("Cleared binding cache. Bindings will be reloaded from storage on demand.");
    }
