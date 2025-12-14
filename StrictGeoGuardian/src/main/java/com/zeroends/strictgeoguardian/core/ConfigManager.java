package com.zeroends.strictgeoguardian.core;

import com.zeroends.strictgeoguardian.StrictGeoGuardian;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class ConfigManager {

    private final StrictGeoGuardian plugin;
    private FileConfiguration config;
    private String api1_url;
    private String api1_token;
    private String api2_url;
    private String api3_url;
    private String hmacKey;
    private double geoToleranceKm;
    private double scoreAutoAllow;
    private double scoreAllowMonitor;
    private double scoreManualReview;
    private List<String> whitelist;

    public ConfigManager(StrictGeoGuardian plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        if (!new File(plugin.getDataFolder(), "config.yml").exists()) {
            plugin.saveDefaultConfig();
        }
        plugin.reloadConfig();
        config = plugin.getConfig();

        api1_url = config.getString("security.api.primary-url", "https://api.findip.net/%s/");
        api1_token = config.getString("security.api.primary-token", "2b26073772e4472db5ddeb906534ac7e");
        api2_url = config.getString("security.api.secondary-url", "http://ip-api.com/json/%s");
        api3_url = config.getString("security.api.fallback-url", "https://ipwho.is/%s");

        hmacKey = config.getString("security.hmac-key");
        if (hmacKey == null || hmacKey.isEmpty()) {
            hmacKey = UUID.randomUUID().toString();
            config.set("security.hmac-key", hmacKey);
            plugin.getLogger().warning("No HMAC key found. Generated a new one. Please save this key.");
            plugin.saveConfig();
        }

        geoToleranceKm = config.getDouble("rules.strict-geo.tolerance-km", 10.0);
        scoreAutoAllow = config.getDouble("rules.scores.auto-allow", 80.0);
        scoreAllowMonitor = config.getDouble("rules.scores.allow-monitor", 60.0);
        scoreManualReview = config.getDouble("rules.scores.manual-review", 40.0);

        whitelist = config.getStringList("rules.whitelist");
    }

    public boolean addWhitelist(String playerName) {
        if (whitelist.contains(playerName.toLowerCase())) {
            return false;
        }
        whitelist.add(playerName.toLowerCase());
        config.set("rules.whitelist", whitelist);
        plugin.saveConfig();
        return true;
    }

    public boolean removeWhitelist(String playerName) {
        if (!whitelist.contains(playerName.toLowerCase())) {
            return false;
        }
        whitelist.remove(playerName.toLowerCase());
        config.set("rules.whitelist", whitelist);
        plugin.saveConfig();
        return true;
    }

    public boolean isPlayerWhitelisted(String playerName) {
        return whitelist.contains(playerName.toLowerCase());
    }

    public String getApi1_url() {
        return api1_url;
    }

    public String getApi1_token() {
        return api1_token;
    }

    public String getApi2_url() {
        return api2_url;
    }

    public String getApi3_url() {
        return api3_url;
    }

    public String getHmacKey() {
        return hmacKey;
    }

    public double getGeoToleranceKm() {
        return geoToleranceKm;
    }

    public double getScoreAutoAllow() {
        return scoreAutoAllow;
    }

    public double getScoreAllowMonitor() {
        return scoreAllowMonitor;
    }

    public double getScoreManualReview() {
        return scoreManualReview;
    }
}
