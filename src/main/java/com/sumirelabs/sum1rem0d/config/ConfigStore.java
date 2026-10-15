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
    private final boolean json5;
    private final java.util.function.Consumer<JsonObject> runtimeValidator;
    private volatile JsonObject values = ConfigSchema.defaults();
    private long revision;

    public ConfigStore(Path path) { this(path, candidate -> {}); }
    /** Registry-dependent checks run on edits, after game registries have been populated. */
    public ConfigStore(Path path, java.util.function.Consumer<JsonObject> runtimeValidator) { this.path = path; this.json5 = path.getFileName().toString().endsWith(".json5"); this.runtimeValidator = runtimeValidator; }

    public synchronized void load() {
        Path reading = path;
        try {
            if (Files.exists(path)) {
                values = ConfigSchema.normalize(read(path));
            } else {
                Path legacy = path.resolveSibling(path.getFileName().toString().replaceFirst("\\.json5$", ".json"));
                if (json5 && Files.exists(legacy)) reading = legacy;
                JsonObject migrated = json5 && Files.exists(legacy) ? ConfigSchema.normalize(read(legacy)) : values;
                save(migrated, json5 && Files.exists(legacy) ? Files.readString(legacy,StandardCharsets.UTF_8) : null); values = migrated;
            }
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot load configuration; original file preserved: " + reading, exception);
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

    public Path path() { return path; }
    private JsonObject read(Path file) throws IOException {
        if (Files.size(file) > Json5Config.MAX_FILE_LENGTH) throw new IOException("Configuration file too large: " + file);
        String source = Files.readString(file, StandardCharsets.UTF_8);
        return file.getFileName().toString().endsWith(".json5") ? Json5Config.read(source) : JsonParser.parseString(source).getAsJsonObject();
    }
    private void save(JsonObject candidate) throws IOException { save(candidate,null); }
    private void save(JsonObject candidate,String imported) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), path.getFileName().toString(), ".tmp");
        try {
            if (json5 && Files.exists(path) && Files.size(path)>Json5Config.MAX_FILE_LENGTH) throw new IOException("Configuration file too large: " + path);
            String previous = json5 && Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : imported;
            Files.writeString(temporary, json5 ? Json5Config.write(candidate, previous) : GSON.toJson(candidate) + "\n", StandardCharsets.UTF_8);
            moveIntoPlace(temporary);
        } catch (RuntimeException failure) {
            throw new IOException("Cannot preserve comments or serialize configuration: " + path, failure);
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
