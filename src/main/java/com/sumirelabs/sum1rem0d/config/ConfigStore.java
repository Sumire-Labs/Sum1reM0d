package com.sumirelabs.sum1rem0d.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Atomic persistence; invalid files are never overwritten with defaults. */
public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path path;
    private final java.util.function.Consumer<JsonObject> runtimeValidator;
    private volatile JsonObject values = ConfigSchema.defaults();
    private long revision;

    public ConfigStore(Path path) { this(path, candidate -> {}); }
    /** Registry-dependent checks run on edits, after game registries have been populated. */
    public ConfigStore(Path path, java.util.function.Consumer<JsonObject> runtimeValidator) { this.path = path; this.runtimeValidator = runtimeValidator; }

    public synchronized void load() {
        try {
            if (Files.exists(path)) {
                values = ConfigSchema.normalize(JsonParser.parseString(Files.readString(path)).getAsJsonObject());
            } else {
                save(values);
            }
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot load configuration; original file preserved: " + path, exception);
        }
    }

    public synchronized JsonObject snapshot() { return values.deepCopy(); }
    public synchronized long revision() { return revision; }
    public synchronized void update(JsonObject candidate, long expectedRevision) throws IOException {
        if (revision != expectedRevision) throw new IllegalArgumentException("設定が更新されています。開き直してください。");
        JsonObject normalized = ConfigSchema.normalize(candidate);
        runtimeValidator.accept(normalized);
        save(normalized);
        values = normalized;
        revision++;
    }
    public boolean enabled(String feature) {
        return values.getAsJsonObject("features").getAsJsonObject(feature).get("enabled").getAsBoolean();
    }
    public int integer(String feature, String option) {
        return values.getAsJsonObject("features").getAsJsonObject(feature).get(option).getAsInt();
    }
    public String text(String feature, String option) {
        return values.getAsJsonObject("features").getAsJsonObject(feature).get(option).getAsString();
    }
    public double number(String feature, String option) { return values.getAsJsonObject("features").getAsJsonObject(feature).get(option).getAsDouble(); }
    public boolean bool(String feature, String option) { return values.getAsJsonObject("features").getAsJsonObject(feature).get(option).getAsBoolean(); }
    public java.util.List<String> list(String feature, String option) { return ConfigSchema.split(text(feature, option)); }

    private void save(JsonObject candidate) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), path.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(candidate) + "\n", StandardCharsets.UTF_8);
            moveIntoPlace(temporary);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
    private void moveIntoPlace(Path temporary) throws IOException {
        for (int attempt = 0; ; attempt++) {
            try {
                try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
                return;
            } catch (java.nio.file.AccessDeniedException busy) {
                if (attempt >= 7) throw busy;
                try { Thread.sleep(25); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IOException("Interrupted while replacing configuration", interrupted); }
            }
        }
    }
}
