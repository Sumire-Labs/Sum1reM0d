package com.sumirelabs.sum1rem0d.config;

import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ConfigStoreTest {
    @TempDir Path directory;

    @Test void switchesAreIndependentAndNormalizationDoesNotMutateDefaults() {
        var defaults = ConfigSchema.defaults().getAsJsonObject("features");
        defaults.getAsJsonObject("early_fall").addProperty("enabled", false);
        assertTrue(defaults.getAsJsonObject("wooden_pickaxe").get("enabled").getAsBoolean());
        assertTrue(ConfigSchema.defaults().getAsJsonObject("features").getAsJsonObject("early_fall").get("enabled").getAsBoolean());
    }
    @Test void missingValuesAreMigratedWithoutLosingUnknownKeys() {
        var input = new JsonObject();
        input.addProperty("extension", "preserve me");
        var result = ConfigSchema.normalize(input);
        assertEquals("preserve me", result.get("extension").getAsString());
        assertTrue(result.has("features"));
        assertFalse(input.has("features"));
    }
    @Test void invalidTypesAndOutOfRangeValuesAreRejected() {
        var input = ConfigSchema.defaults();
        var tool = input.getAsJsonObject("features").getAsJsonObject("wooden_pickaxe");
        tool.addProperty("enabled", "true");
        assertThrows(IllegalArgumentException.class, () -> ConfigSchema.normalize(input));
        tool.addProperty("enabled", true);
        tool.addProperty("durability", 0);
        assertThrows(IllegalArgumentException.class, () -> ConfigSchema.normalize(input));
        tool.addProperty("durability", 1.5);
        assertThrows(IllegalArgumentException.class, () -> ConfigSchema.normalize(input));
    }
    @Test void futureSchemaAndOversizedDocumentsAreRejected() {
        var input = ConfigSchema.defaults();
        input.addProperty("schema_version", ConfigSchema.VERSION + 1);
        assertThrows(IllegalArgumentException.class, () -> ConfigSchema.normalize(input));
        input.addProperty("schema_version", ConfigSchema.VERSION);
        input.addProperty("extension", "x".repeat(ConfigSchema.MAX_LENGTH));
        assertThrows(IllegalArgumentException.class, () -> ConfigSchema.normalize(input));
    }
    @Test void changesPersistAndSnapshotsCannotMutateTheStore() throws Exception {
        var path = directory.resolve("sum1rem0d.json");
        var store = new ConfigStore(path);
        store.load();
        var candidate = store.snapshot();
        candidate.getAsJsonObject("features").getAsJsonObject("death_message").addProperty("text", "変更後");
        assertEquals("You Suck!! 👎🖕", store.text("death_message", "text"));
        store.update(candidate, 0);
        assertEquals(1, store.revision());
        candidate.getAsJsonObject("features").getAsJsonObject("death_message").addProperty("text", "not applied");
        var reloaded = new ConfigStore(path);
        reloaded.load();
        assertEquals("変更後", reloaded.text("death_message", "text"));
        assertEquals("変更後", store.text("death_message", "text"));
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }
    @Test void staleOrInvalidUpdateDoesNotChangeMemoryOrDisk() throws Exception {
        var path = directory.resolve("sum1rem0d.json");
        var store = new ConfigStore(path);
        store.load();
        var before = Files.readString(path);
        assertThrows(IllegalArgumentException.class, () -> store.update(store.snapshot(), -1));
        var invalid = store.snapshot();
        invalid.getAsJsonObject("features").getAsJsonObject("early_fall").addProperty("start_height", 0);
        assertThrows(IllegalArgumentException.class, () -> store.update(invalid, 0));
        assertEquals(before, Files.readString(path));
        assertEquals(0, store.revision());
        assertEquals(2, store.integer("early_fall", "start_height"));
    }
    @Test void corruptFileIsPreserved() throws Exception {
        var path = directory.resolve("sum1rem0d.json");
        Files.writeString(path, "{broken");
        assertThrows(IllegalStateException.class, () -> new ConfigStore(path).load());
        assertEquals("{broken", Files.readString(path));
    }
    @Test
    @org.junit.jupiter.api.condition.EnabledOnOs(org.junit.jupiter.api.condition.OS.WINDOWS)
    void temporaryWindowsSharingLockIsRetriedBeforeCommitting() throws Exception {
        var path = directory.resolve("sum1rem0d.json"); var store = new ConfigStore(path); store.load();
        var options = new java.util.HashSet<java.nio.file.OpenOption>(); options.add(java.nio.file.StandardOpenOption.READ); options.add(com.sun.nio.file.ExtendedOpenOption.NOSHARE_DELETE);
        var channel = Files.newByteChannel(path, options); var scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        try {
            var probe = Files.createTempFile(directory, "locked-rename", ".tmp");
            try { assertThrows(java.nio.file.AccessDeniedException.class, () -> Files.move(probe, path, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING)); }
            finally { Files.deleteIfExists(probe); }
            var released = scheduler.schedule(() -> { channel.close(); return true; }, 80, java.util.concurrent.TimeUnit.MILLISECONDS);
            var draft = store.snapshot(); draft.getAsJsonObject("features").getAsJsonObject("death_message").addProperty("text", "Saved after lock"); store.update(draft, 0);
            assertTrue(released.get()); assertEquals(1, store.revision());
            var reloaded = new ConfigStore(path); reloaded.load(); assertEquals("Saved after lock", reloaded.text("death_message", "text"));
        } finally { channel.close(); scheduler.shutdownNow(); }
    }
    @Test void runtimeValidationFailurePreservesFileValuesAndRevision() throws Exception {
        var path = directory.resolve("sum1rem0d.json");
        var store = new ConfigStore(path, candidate -> { throw new IllegalArgumentException("Registry validation failed"); });
        store.load();
        var saved = store.snapshot(); var bytes = Files.readAllBytes(path);
        var draft = store.snapshot();
        draft.getAsJsonObject("features").getAsJsonObject("death_message").addProperty("text", "Not saved");
        assertThrows(IllegalArgumentException.class, () -> store.update(draft, 0));
        assertEquals(saved, store.snapshot()); assertEquals(0, store.revision());
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }
    @Test void failedDiskWriteDoesNotCommitMemoryOrRevision() throws Exception {
        var path = directory.resolve("sum1rem0d.json");
        var store = new ConfigStore(path);
        store.load();
        Files.delete(path);
        Files.createDirectory(path);
        Files.writeString(path.resolve("blocker"), "keep directory non-empty");
        var candidate = store.snapshot();
        candidate.getAsJsonObject("features").getAsJsonObject("wooden_pickaxe").addProperty("durability", 10);
        assertThrows(java.io.IOException.class, () -> store.update(candidate, 0));
        assertEquals(1, store.integer("wooden_pickaxe", "durability"));
        assertEquals(0, store.revision());
    }
}
