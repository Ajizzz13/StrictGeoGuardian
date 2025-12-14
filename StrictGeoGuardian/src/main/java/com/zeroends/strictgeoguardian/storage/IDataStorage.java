package com.zeroends.strictgeoguardian.storage;

import com.zeroends.strictgeoguardian.model.Fingerprint;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IDataStorage {

    CompletableFuture<Void> saveFingerprint(Fingerprint fingerprint);

    CompletableFuture<Fingerprint> loadFingerprint(String playerName);

    CompletableFuture<Fingerprint> loadFingerprintByUuid(UUID uuid);

    CompletableFuture<Void> deleteFingerprint(String playerName);

    CompletableFuture<java.util.List<Fingerprint>> getAllFingerprints();
}
