package com.sumirelabs.sum1rem0d.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;

/** One definition drives validation, defaults and the configuration screen. */
public final class ConfigSchema {
    public static final int VERSION = 1;
    public static final int MAX_LENGTH = 65536;
    public enum Kind { BOOLEAN, INTEGER, DECIMAL, TEXT, LIST, MAP }
    public record Option(String key, String label, Kind kind, JsonPrimitive initial, double min, double max) {
        public boolean isBoolean() { return kind == Kind.BOOLEAN; }
        public boolean isNumber() { return kind == Kind.INTEGER || kind == Kind.DECIMAL; }
        public void validate(JsonElement value) {
            if (value == null || !value.isJsonPrimitive()) throw new IllegalArgumentException(label);
            JsonPrimitive primitive = value.getAsJsonPrimitive();
            if (initial.isBoolean()) {
                if (!primitive.isBoolean()) throw new IllegalArgumentException(label + ": boolean required");
            } else if (initial.isNumber()) {
                if (!primitive.isNumber()) throw new IllegalArgumentException(label + ": number required");
                double number = primitive.getAsDouble();
                if (!Double.isFinite(number) || number < min || number > max || (kind == Kind.INTEGER && number != Math.rint(number)))
                    throw new IllegalArgumentException(label + ": " + (int) min + " .. " + (int) max);
            } else if (!primitive.isString() || primitive.getAsString().length() > max) {
                throw new IllegalArgumentException(label + ": invalid text");
            }
            if (kind == Kind.LIST || kind == Kind.MAP) {
                for (String entry : split(primitive.getAsString())) validateEntry(this, entry);
            } else if (kind == Kind.TEXT && !key.equals("text")) validateEntry(this, primitive.getAsString());
        }
    }
    public record Feature(String id, String label, boolean implemented, List<Option> options) {}
    private static final List<Feature> FEATURES = new ArrayList<>();

    static {
        try (var input = ConfigSchema.class.getResourceAsStream("/assets/sum1rem0d/config-schema.json")) {
            if (input == null) throw new IllegalStateException("Missing bundled config schema");
            var entries = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();
            for (var rawFeature : entries) {
                var f = rawFeature.getAsJsonObject();
                List<Option> options = new ArrayList<>();
                for (var rawOption : f.getAsJsonArray("options")) {
                    var o = rawOption.getAsJsonObject();
                    options.add(new Option(o.get("key").getAsString(), o.get("label").getAsString(), Kind.valueOf(o.get("type").getAsString()), o.get("initial").getAsJsonPrimitive(), o.get("min").getAsDouble(), o.get("max").getAsDouble()));
                }
                FEATURES.add(new Feature(f.get("id").getAsString(), f.get("label").getAsString(), f.get("implemented").getAsBoolean(), List.copyOf(options)));
            }
        } catch (Exception exception) { throw new ExceptionInInitializerError(exception); }
    }
    private ConfigSchema() {}
    public static List<String> split(String value) { return java.util.Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList(); }
    /** Syntax validation works before registries are initialized and on dedicated servers. */
    public static void validateEntry(Option option, String entry) {
        String key = option.key();
        boolean valid;
        if (option.kind() == Kind.MAP) {
            String[] pair = entry.split("=", -1);
            valid = pair.length == 2 && resourceId(pair[0], false) && resourceId(pair[1], false);
        } else if (key.equals("mode")) {
            valid = com.sumirelabs.sum1rem0d.game.OreDropMapping.MODES.contains(entry);
        } else if (key.contains("url")) {
            if (key.equals("urls") && entry.equals("sum1rem0d:default_ad")) return;
            try { var uri = java.net.URI.create(entry); valid = ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) && uri.getHost() != null; }
            catch (IllegalArgumentException exception) { valid = false; }
        } else if (key.equals("moon_phases")) valid = entry.matches("[0-7]");
        else if (key.equals("target_mods") || key.equals("namespaces")) valid = entry.matches("[a-z0-9_.-]+");
        else valid = resourceId(entry, option.kind() == Kind.LIST && !key.equals("soap_materials") && !key.equals("dimensions") && !key.equals("candidate_blocks"));
        if (!valid) throw new IllegalArgumentException(option.label() + ": 値の形式を確認してください: " + entry);
    }
    private static boolean resourceId(String value, boolean tags) { return value.matches((tags ? "#?" : "") + "[a-z0-9_.-]+:[a-z0-9_./-]+"); }
    public static List<Feature> features() { return List.copyOf(FEATURES); }
    public static JsonObject defaults() { return normalize(new JsonObject()); }

    /** Fill missing defaults, preserve future/unknown keys, reject invalid known values. */
    public static JsonObject normalize(JsonObject input) {
        JsonObject result = input.deepCopy();
        if (result.has("schema_version")) {
            JsonElement version = result.get("schema_version");
            if (!version.isJsonPrimitive() || !version.getAsJsonPrimitive().isNumber()
                || version.getAsDouble() != VERSION)
                throw new IllegalArgumentException("Unsupported configuration schema");
        }
        result.addProperty("schema_version", VERSION);
        if (!result.has("features")) result.add("features", new JsonObject());
        if (!result.get("features").isJsonObject()) throw new IllegalArgumentException("features must be an object");
        JsonObject features = result.getAsJsonObject("features");
        for (Feature feature : FEATURES) {
            if (!features.has(feature.id())) features.add(feature.id(), new JsonObject());
            if (!features.get(feature.id()).isJsonObject()) throw new IllegalArgumentException(feature.id());
            JsonObject values = features.getAsJsonObject(feature.id());
            for (Option option : feature.options()) {
                if (!values.has(option.key())) values.add(option.key(), option.initial());
                option.validate(values.get(option.key()));
            }
        }
        ordered(features, "economy", "starting_balance", "maximum_balance");
        ordered(features, "daily_fuel", "minimum", "maximum");
        ordered(features, "knockback_mob", "minimum_distance", "maximum_distance");
        ordered(features, "sleep_deprivation", "hunger_days", "slow_days");
        ordered(features, "sleep_deprivation", "slow_days", "death_days");
        ordered(features, "cleanliness", "health_threshold", "maximum");
        var swap = features.getAsJsonObject("inventory_swap");
        if (swap.get("start_slot").getAsInt() + swap.get("slots").getAsInt() > 36) throw new IllegalArgumentException("交換する枠がインベントリの範囲を超えています。");
        var soap = split(features.getAsJsonObject("cleanliness").get("soap_materials").getAsString());
        if (soap.isEmpty() || soap.size() > 9) throw new IllegalArgumentException("石鹸の材料は1～9枠で指定してください。");
        var ores = new java.util.HashSet<String>();
        if (!com.sumirelabs.sum1rem0d.game.OreDropMapping.MODES.contains(features.getAsJsonObject("ore_drops").get("mode").getAsString()))
            throw new IllegalArgumentException("鉱石ドロップのモードは fixed / world / daily / mining から選んでください。");
        for (String pair : split(features.getAsJsonObject("ore_drops").get("mapping").getAsString()))
            if (!ores.add(pair.split("=", 2)[0])) throw new IllegalArgumentException("鉱石ドロップの交換元が重複しています。");
        if (result.toString().length() > MAX_LENGTH) throw new IllegalArgumentException("Configuration is too large");
        com.sumirelabs.sum1rem0d.network.JsonCompression.encode(result.toString());
        return result;
    }
    private static void ordered(JsonObject features, String feature, String low, String high) {
        var values = features.getAsJsonObject(feature);
        if (values.get(low).getAsDouble() > values.get(high).getAsDouble()) throw new IllegalArgumentException(feature + ": " + low + " は " + high + " 以下にしてください。");
    }
}
