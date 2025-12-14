package com.zeroends.strictgeoguardian.service;

import com.zeroends.strictgeoguardian.StrictGeoGuardian;
import com.zeroends.strictgeoguardian.model.Fingerprint;
import com.zeroends.strictgeoguardian.model.GeoData;
import com.zeroends.strictgeoguardian.util.GeoUtils;
import com.zeroends.strictgeoguardian.util.HashUtils;

import java.net.InetAddress;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FingerprintService {

    private final StrictGeoGuardian plugin;

    public FingerprintService(StrictGeoGuardian plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Fingerprint> createFingerprint(String playerName, UUID playerUuid, String ipAddress, GeoData geoData) {
        return CompletableFuture.supplyAsync(() -> {
            Fingerprint.Builder builder = new Fingerprint.Builder(playerName, playerUuid);
            
            builder.geoData(geoData);

            try {
                InetAddress inetAddress = InetAddress.getByName(ipAddress);
                String type = geoData.getType() != null ? geoData.getType() : "IPv4";
                String subnetPrefix = HashUtils.getSubnetPrefix(inetAddress, type.equals("IPv4") ? 24 : 64);
                builder.hashedPrefix(HashUtils.hmacSha256(plugin.getConfigManager().getHmacKey(), subnetPrefix));
            } catch (Exception e) {
                plugin.getLogger().warning("Could not create subnet prefix for " + ipAddress);
                builder.hashedPrefix("error");
            }
            
            builder.hashedPseudoAsn(HashUtils.hmacSha256(plugin.getConfigManager().getHmacKey(), geoData.getAsn()));
            builder.hashedPtr(HashUtils.hmacSha256(plugin.getConfigManager().getHmacKey(), geoData.getDomain()));

            return builder.build();
        });
    }

    public double calculateSimilarity(Fingerprint current, Fingerprint stored) {
        double score = 0.0;
        
        score += identitySimilarity(current, stored) * 0.30;
        score += networkSimilarity(current, stored) * 0.25;
        score += clientSimilarity(current, stored) * 0.30;
        score += geographicalSimilarity(current, stored) * 0.15;
        
        return Math.min(100.0, score * 100.0);
    }
    
    private double identitySimilarity(Fingerprint current, Fingerprint stored) {
        double score = 0.0;
        if (Objects.equals(current.javaUuid(), stored.javaUuid())) score += 0.15;
        if (Objects.equals(current.edition(), stored.edition())) score += 0.08;
        if (Objects.equals(current.profilePropertyDigest(), stored.profilePropertyDigest())) score += 0.07;
        return score / 0.30; 
    }
    
    private double networkSimilarity(Fingerprint current, Fingerprint stored) {
        double score = 0.0;
        if (Objects.equals(current.ipVersion(), stored.ipVersion())) score += 0.02;
        if (Objects.equals(current.hashedPrefix(), stored.hashedPrefix())) score += 0.07;
        if (Objects.equals(current.hashedPseudoAsn(), stored.hashedPseudoAsn())) score += 0.06;
        if (Objects.equals(current.hashedPtr(), stored.hashedPtr())) score += 0.05;
        if (current.tcpTtl() == stored.tcpTtl()) score += 0.03;
        if (current.tcpMss() == stored.tcpMss()) score += 0.02;
        return score / 0.25;
    }
    
    private double clientSimilarity(Fingerprint current, Fingerprint stored) {
        double score = 0.0;
        if (Objects.equals(current.clientBrand(), stored.clientBrand())) score += 0.07;
        if (Objects.equals(current.modListHash(), stored.modListHash())) score += 0.07;
        if (Objects.equals(current.resourcePackHash(), stored.resourcePackHash())) score += 0.05;
        if (Objects.equals(current.viewportSettings(), stored.viewportSettings())) score += 0.04;
        if (Objects.equals(current.locale(), stored.locale())) score += 0.03;
        if (Objects.equals(current.skinParts(), stored.skinParts())) score += 0.04;
        return score / 0.30;
    }

    private double geographicalSimilarity(Fingerprint current, Fingerprint stored) {
        double score = 0.0;
        if (Objects.equals(current.countryCode(), stored.countryCode())) score += 0.04;
        if (Objects.equals(current.continentCode(), stored.continentCode())) score += 0.01;
        if (Objects.equals(current.region(), stored.region())) score += 0.03;
        if (Objects.equals(current.city(), stored.city())) score += 0.03;
        if (Objects.equals(current.timezone(), stored.timezone())) score += 0.02;

        double distance = GeoUtils.calculateDistance(current.latitude(), current.longitude(), stored.latitude(), stored.longitude());
        if (distance <= 10.0) score += 0.02; 
        else if (distance <= 50.0) score += 0.01;
        
        return score / 0.15;
    }

    public boolean isGeographicalIdentical(Fingerprint current, Fingerprint stored, double toleranceKm) {
        if (current == null || stored == null) return false;
        
        if (!Objects.equals(current.countryCode(), stored.countryCode())) {
            return false;
        }
        if (!Objects.equals(current.region(), stored.region())) {
            return false;
        }
        if (!Objects.equals(current.city(), stored.city())) {
            return false;
        }
        
        double distance = GeoUtils.calculateDistance(current.latitude(), current.longitude(), stored.latitude(), stored.longitude());
        return distance <= toleranceKm;
    }
}
