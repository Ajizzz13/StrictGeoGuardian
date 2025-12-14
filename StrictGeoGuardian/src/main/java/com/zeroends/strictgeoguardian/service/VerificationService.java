package com.zeroends.strictgeoguardian.service;

import com.zeroends.strictgeoguardian.StrictGeoGuardian;
import com.zeroends.strictgeoguardian.core.AuthManager;
import com.zeroends.strictgeoguardian.core.ConfigManager;
import com.zeroends.strictgeoguardian.model.AuthStatus;
import com.zeroends.strictgeoguardian.model.Fingerprint;
import com.zeroends.strictgeoguardian.model.GeoData;
import com.zeroends.strictgeoguardian.model.VerificationResult;
import com.zeroends.strictgeoguardian.storage.IAuthStorage;
import com.zeroends.strictgeoguardian.storage.IDataStorage;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VerificationService {

    private final StrictGeoGuardian plugin;
    private final IDataStorage fingerprintStorage;
    private final IAuthStorage authStorage;
    private final GeoService geoService;
    private final FingerprintService fingerprintService;
    private final ConfigManager configManager;
    private AuthManager authManager;

    public VerificationService(StrictGeoGuardian plugin, IDataStorage fingerprintStorage, IAuthStorage authStorage, GeoService geoService, FingerprintService fingerprintService, ConfigManager configManager) {
        this.plugin = plugin;
        this.fingerprintStorage = fingerprintStorage;
        this.authStorage = authStorage;
        this.geoService = geoService;
        this.fingerprintService = fingerprintService;
        this.configManager = configManager;
    }
    
    public void setAuthManager(AuthManager authManager) {
        this.authManager = authManager;
    }

    public VerificationResult verifyPlayer(String playerName, UUID playerUuid, String ipAddress) {
        try {
            if (configManager.isPlayerWhitelisted(playerName)) {
                return VerificationResult.autoAllow(100.0);
            }

            CompletableFuture<Boolean> isRegisteredFuture = authStorage.isPlayerRegistered(playerUuid);
            CompletableFuture<Fingerprint> storedFingerprintFuture = fingerprintStorage.loadFingerprint(playerName);

            boolean isRegistered = isRegisteredFuture.join();
            Fingerprint storedFingerprint = storedFingerprintFuture.join();

            if (!isRegistered) {
                return handleFirstLogin(playerUuid, ipAddress);
            }

            if (storedFingerprint == null) {
                plugin.getLogger().warning("Player " + playerName + " is registered for Auth, but has no fingerprint. Forcing re-registration.");
                return handleFirstLogin(playerUuid, ipAddress);
            }

            return handleReturningPlayer(playerName, playerUuid, ipAddress, storedFingerprint);

        } catch (Exception e) {
            plugin.getLogger().severe("Exception during verification for " + playerName + ": " + e.getMessage());
            e.printStackTrace();
            return VerificationResult.autoBlockError("Internal Verification Error");
        }
    }

    private VerificationResult handleFirstLogin(UUID playerUuid, String ipAddress) {
        GeoData registrationGeoData = geoService.fetchRegistrationData(ipAddress).join();
        if (registrationGeoData == null || !registrationGeoData.isSuccess()) {
            plugin.getLogger().warning("Failed to fetch valid GeoData for new player " + playerUuid + " on first login.");
            return VerificationResult.autoBlockError("GeoIP Lookup Failed");
        }
        authManager.storePendingGeoData(playerUuid, registrationGeoData);
        return VerificationResult.needsRegistration();
    }

    private VerificationResult handleReturningPlayer(String playerName, UUID playerUuid, String ipAddress, Fingerprint stored) {
        double geoTolerance = configManager.getGeoToleranceKm();
        GeoData lastValidGeoData = null;

        // 1. Try API 3 (ipwho.is)
        GeoData api3Data = geoService.fetchApi3(ipAddress).join();
        if (api3Data != null && api3Data.isSuccess()) {
            lastValidGeoData = api3Data;
            Fingerprint fp3 = fingerprintService.createFingerprint(playerName, playerUuid, ipAddress, api3Data).join();
            if (fingerprintService.isGeographicalIdentical(fp3, stored, geoTolerance)) {
                plugin.getLogger().info("API 3 check passed for " + playerName);
                return calculateScoreAndDecide(fp3, stored);
            }
        }
        plugin.getLogger().warning("API 3 check failed/mismatched for " + playerName + ". Trying API 2...");

        // 2. Try API 2 (ip-api.com)
        GeoData api2Data = geoService.fetchApi2(ipAddress).join();
        if (api2Data != null && api2Data.isSuccess()) {
            lastValidGeoData = api2Data;
            Fingerprint fp2 = fingerprintService.createFingerprint(playerName, playerUuid, ipAddress, api2Data).join();
            if (fingerprintService.isGeographicalIdentical(fp2, stored, geoTolerance)) {
                plugin.getLogger().info("API 2 re-check passed for " + playerName);
                return calculateScoreAndDecide(fp2, stored);
            }
        }
        plugin.getLogger().warning("API 2 check failed/mismatched for " + playerName + ". Trying API 1 (Source of Truth)...");

        // 3. Try API 1 (findip.net)
        GeoData api1Data = geoService.fetchApi1(ipAddress).join();
        if (api1Data != null && api1Data.isSuccess()) {
            lastValidGeoData = api1Data;
            Fingerprint fp1 = fingerprintService.createFingerprint(playerName, playerUuid, ipAddress, api1Data).join();
            if (fingerprintService.isGeographicalIdentical(fp1, stored, geoTolerance)) {
                plugin.getLogger().info("API 1 (Source of Truth) re-check passed for " + playerName);
                return calculateScoreAndDecide(fp1, stored);
            }
        } else {
             plugin.getLogger().severe("FINAL CHECK FAILED: API 1 (Source of Truth) could not fetch data for " + playerName + ".");
        }

        plugin.getLogger().warning("All API checks failed for " + playerName + ". Forcing password verification.");
        if (lastValidGeoData != null) {
            authManager.storePendingGeoData(playerUuid, lastValidGeoData);
        } else {
            plugin.getLogger().severe("Could not get ANY valid GeoData for " + playerName + ". Cannot update fingerprint even if password is correct.");
            GeoData fallbackData = geoService.createLocalHostData(); 
            authManager.storePendingGeoData(playerUuid, fallbackData);
        }
        
        return VerificationResult.needsLogin("Geographical Mismatch");
    }

    private VerificationResult calculateScoreAndDecide(Fingerprint current, Fingerprint stored) {
        double similarityScore = fingerprintService.calculateSimilarity(current, stored);

        if (similarityScore >= configManager.getScoreAutoAllow()) {
            return VerificationResult.autoAllow(similarityScore);
        } else if (similarityScore >= configManager.getScoreAllowMonitor()) {
            return VerificationResult.allowMonitor(similarityScore);
        } else {
            plugin.getLogger().info("Geo match OK, but low similarity score (" + similarityScore + "). Forcing password verification.");
            return VerificationResult.needsLogin("Low Similarity Score");
        }
    }
}
