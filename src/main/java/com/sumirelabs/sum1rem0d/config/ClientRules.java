package com.sumirelabs.sum1rem0d.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.network.ConfigStatePayload;

/** No client classes: common item code can safely consult the synchronized snapshot. */
public final class ClientRules {
    private static volatile JsonObject remote;
    public static long revision;
    public static boolean editable;
    public static String message = "";
    public static long responseId;
    public static boolean responseApplied;
    public static String responseMessage = "";
    private static long nextRequestId;
    public static long nextRequestId() { return ++nextRequestId; }
    private ClientRules() {}

    public static void accept(ConfigStatePayload state) {
        remote = ConfigSchema.normalize(JsonParser.parseString(state.json()).getAsJsonObject());
        revision = state.revision();
        editable = state.editable();
        message = state.message();
        if (state.requestId() != 0) {
            responseId = state.requestId(); responseApplied = state.applied(); responseMessage = state.message();
        }
    }
    public static void clear() { remote = null; editable = false; revision = 0; message = ""; responseId = 0; responseApplied = false; responseMessage = ""; }
    public static boolean received() { return remote != null; }
    public static JsonObject snapshot() { return remote == null ? Sum1reM0d.config().snapshot() : remote.deepCopy(); }
    public static JsonObject values(String feature) { return snapshot().getAsJsonObject("features").getAsJsonObject(feature); }
    public static boolean enabled(String feature) {
        JsonObject current = remote;
        return current == null ? Sum1reM0d.config().enabled(feature)
            : current.getAsJsonObject("features").getAsJsonObject(feature).get("enabled").getAsBoolean();
    }
    public static int integer(String feature, String option) {
        JsonObject current = remote;
        return current == null ? Sum1reM0d.config().integer(feature, option)
            : current.getAsJsonObject("features").getAsJsonObject(feature).get(option).getAsInt();
    }
    public static String text(String feature, String option) {
        JsonObject current = remote;
        return current == null ? Sum1reM0d.config().text(feature, option)
            : current.getAsJsonObject("features").getAsJsonObject(feature).get(option).getAsString();
    }
    public static double number(String feature, String option) {
        JsonObject current = remote;
        return current == null ? Sum1reM0d.config().number(feature, option) : current.getAsJsonObject("features").getAsJsonObject(feature).get(option).getAsDouble();
    }
    public static boolean bool(String feature, String option) {
        JsonObject current = remote;
        return current == null ? Sum1reM0d.config().bool(feature, option) : current.getAsJsonObject("features").getAsJsonObject(feature).get(option).getAsBoolean();
    }
    public static java.util.List<String> list(String feature, String option) { return ConfigSchema.split(text(feature, option)); }
}
