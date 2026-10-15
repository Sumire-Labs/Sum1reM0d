package com.sumirelabs.sum1rem0d.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Shared user-facing timing descriptions, also used by the configuration reference generator. */
public final class ConfigTiming {
    private static final JsonObject DESCRIPTIONS;
    static {
        try (var input = ConfigTiming.class.getResourceAsStream("/assets/sum1rem0d/config-timing.json")) {
            if (input == null) throw new IllegalStateException("Missing configuration timing descriptions");
            DESCRIPTIONS = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception error) { throw new ExceptionInInitializerError(error); }
    }
    private ConfigTiming() {}
    public static String description(String feature, String key) {
        var features = DESCRIPTIONS.getAsJsonObject("features");
        var entry = features.getAsJsonObject(feature);
        if (entry != null && entry.has("options") && entry.getAsJsonObject("options").has(key)) return entry.getAsJsonObject("options").get(key).getAsString();
        if (key.equals("enabled")) return DESCRIPTIONS.get("enabled").getAsString();
        return entry != null && entry.has("default") ? entry.get("default").getAsString() : DESCRIPTIONS.get("default").getAsString();
    }
}
