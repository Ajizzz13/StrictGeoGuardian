package com.zeroends.strictgeoguardian;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zeroends.strictgeoguardian.commands.IdentityCommand;
import com.zeroends.strictgeoguardian.commands.PassCommand;
import com.zeroends.strictgeoguardian.core.AuthManager;
import com.zeroends.strictgeoguardian.core.ConfigManager;
import com.zeroends.strictgeoguardian.listener.AuthListener;
import com.zeroends.strictgeoguardian.listener.PlayerLoginListener;
import com.zeroends.strictgeoguardian.service.FingerprintService;
import com.zeroends.strictgeoguardian.service.GeoService;
import com.zeroends.strictgeoguardian.service.VerificationService;
import com.zeroends.strictgeoguardian.storage.IAuthStorage;
import com.zeroends.strictgeoguardian.storage.IDataStorage;
import com.zeroends.strictgeoguardian.storage.JsonAuthStorage;
import com.zeroends.strictgeoguardian.storage.JsonStorage;
import com.zeroends.strictgeoguardian.util.InstantAdapter;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.Objects;

public final class StrictGeoGuardian extends JavaPlugin {

    private IDataStorage fingerprintStorage;
    private IAuthStorage authStorage;
    private ConfigManager configManager;
    private GeoService geoService;
    private FingerprintService fingerprintService;
    private VerificationService verificationService;
    private AuthManager authManager;
    private Gson gson;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        configManager.loadConfig();

        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantAdapter())
                .setPrettyPrinting()
                .create();

        this.fingerprintStorage = new JsonStorage(this, gson);
        this.authStorage = new JsonAuthStorage(this);

        this.geoService = new GeoService(this,
                configManager.getApi1_url(),
                configManager.getApi1_token(),
                configManager.getApi2_url(),
                configManager.getApi3_url()
        );
        this.fingerprintService = new FingerprintService(this);
        
        this.verificationService = new VerificationService(this, fingerprintStorage, authStorage, geoService, fingerprintService, configManager);
        this.authManager = new AuthManager(this, authStorage, fingerprintStorage, geoService, fingerprintService);
        
        verificationService.setAuthManager(authManager);

        getServer().getPluginManager().registerEvents(new PlayerLoginListener(this, verificationService, authManager), this);
        getServer().getPluginManager().registerEvents(new AuthListener(this, authManager), this);

        Objects.requireNonNull(getCommand("identity")).setExecutor(new IdentityCommand(this, fingerprintStorage, verificationService, gson));
        Objects.requireNonNull(getCommand("pass")).setExecutor(new PassCommand(authManager));

        getLogger().info("StrictGeoGuardian has been enabled. Protecting identities.");
    }

    @Override
    public void onDisable() {
        getLogger().info("StrictGeoGuardian has been disabled.");
    }

    public IDataStorage getFingerprintStorage() {
        return fingerprintStorage;
    }

    public IAuthStorage getAuthStorage() {
        return authStorage;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public GeoService getGeoService() {
        return geoService;
    }

    public FingerprintService getFingerprintService() {
        return fingerprintService;
    }

    public VerificationService getVerificationService() {
        return verificationService;
    }
    
    public AuthManager getAuthManager() {
        return authManager;
    }
    
    public Gson getGson() {
        return gson;
    }
}
