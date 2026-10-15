package com.sumirelabs.sum1rem0d.config;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.network.GameStatePayload;
import java.util.UUID;

/** Snapshot synchronized from the server. Kept free of client classes. */
public final class ClientGameState {
    private static JsonObject state = new JsonObject();
    public static void accept(GameStatePayload payload) { state = JsonParser.parseString(payload.json()).getAsJsonObject(); }
    public static void clear() { state = new JsonObject(); }
    public static JsonObject snapshot() { return state.deepCopy(); }
    public static boolean daily(String feature) { return state.has("daily") && state.getAsJsonObject("daily").has(feature) && state.getAsJsonObject("daily").get(feature).getAsBoolean() && ClientRules.enabled(feature); }
    public static double number(String key, double fallback) { return state.has(key) ? state.get(key).getAsDouble() : fallback; }
    public static boolean bool(String key) { return state.has(key) && state.get(key).getAsBoolean(); }
    public static boolean white(UUID id) { return state.has("white") && state.getAsJsonObject("white").has(id.toString()); }
    public static String alias(UUID id) { return state.has("aliases") && state.getAsJsonObject("aliases").has(id.toString()) ? state.getAsJsonObject("aliases").get(id.toString()).getAsString() : null; }
    public static boolean received() { return state.has("day"); }
}
