package com.zeroends.nameguard.storage;

import com.zeroends.nameguard.model.Binding;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;

public interface IStorage {
    void init() throws IOException;

    Optional<Binding> loadBinding(@NotNull String normalizedName) throws IOException;

    void saveBinding(@NotNull Binding binding) throws IOException;

    void removeBinding(@NotNull String normalizedName) throws IOException;

    long countBindings() throws IOException;

    void shutdown();
}
