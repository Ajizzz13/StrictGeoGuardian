package com.zeroends.strictgeoguardian.storage;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IAuthStorage {

    CompletableFuture<Boolean> isPlayerRegistered(UUID uuid);

    CompletableFuture<String> getPasswordHash(UUID uuid);

    CompletableFuture<Void> savePasswordHash(UUID uuid, String playerName, String hash);

    CompletableFuture<Void> removePlayer(UUID uuid);
}
