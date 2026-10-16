package com.sumirelabs.sum1rem0d.game;

import com.google.gson.JsonObject;
import com.sumirelabs.sum1rem0d.config.ConfigSchema;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Mob;
import java.util.*;

/** Syntax is checked before bootstrap; registered objects and world data are checked when available. */
public final class RegisteredConfigValidation {
    private RegisteredConfigValidation() {}
    public static void validate(JsonObject candidate) { validate(candidate, WorldState.validationServer()); }
    public static void validate(JsonObject candidate, MinecraftServer server) {
        var features = candidate.getAsJsonObject("features");
        Set<String> dimensions = new HashSet<>();
        if (server != null) {
            for (var level : server.getAllLevels()) dimensions.add(level.dimension().identifier().toString());
            server.registryAccess().lookup(net.minecraft.core.registries.Registries.LEVEL_STEM).ifPresent(registry -> registry.listElementIds().forEach(key -> dimensions.add(key.identifier().toString())));
        }
        for (var feature : ConfigSchema.features()) {
            var values = features.getAsJsonObject(feature.id());
            if (!values.get("enabled").getAsBoolean()) continue;
            for (var option : feature.options()) {
                String key = option.key(), label = feature.label() + " / " + option.label();
                if (feature.id().equals("ore_drops") && key.equals("mapping")) {
                    for (String entry : ConfigSchema.split(values.get(key).getAsString())) {
                        var pair = entry.split("=", 2); check(BuiltInRegistries.BLOCK, pair[0], label, server != null); check(BuiltInRegistries.ITEM, pair[1], label, server != null);
                    }
                    continue;
                }
                Registry<?> registry = switch (key) {
                    case "target_items", "forbidden_items", "allowed_items", "input_item", "output_item", "soap_materials", "mud_materials", "fuel_items" -> BuiltInRegistries.ITEM;
                    case "target_blocks", "excluded_blocks", "candidate_blocks", "replacement", "first_block", "second_block" -> BuiltInRegistries.BLOCK;
                    case "buff_effects", "debuff_effects" -> BuiltInRegistries.MOB_EFFECT;
                    case "mob_type" -> BuiltInRegistries.ENTITY_TYPE;
                    default -> null;
                };
                if (registry != null) for (String entry : ConfigSchema.split(values.get(key).getAsString())) check(registry, entry, label, server != null);
                if (key.equals("dimensions") && server != null)
                    for (String entry : ConfigSchema.split(values.get(key).getAsString())) if (!dimensions.contains(entry)) throw new IllegalArgumentException(label + ": 登録されていないディメンション: " + entry);
            }
        }
        var mob = features.getAsJsonObject("knockback_mob");
        if (mob.get("enabled").getAsBoolean() && server != null) {
            var type = BuiltInRegistries.ENTITY_TYPE.getOptional(Rules.id(mob.get("mob_type").getAsString())).orElseThrow();
            // Validate the type, not whether this world's difficulty permits spawning it.
            // The preview is never added to the world.
            //? if >=26.2 {
            var preview = type.create(server.overworld(), new net.minecraft.world.entity.EntitySpawnRequest(net.minecraft.world.entity.EntitySpawnReason.COMMAND, true));
            //?} else {
            /*var preview = type.create(server.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            *///?}
            try { if (!(preview instanceof Mob)) throw new IllegalArgumentException("ノックバック棒のモブ: Mob以外のエンティティは指定できません。"); }
            finally { if (preview != null) preview.discard(); }
        }
        ConfiguredCraftingRecipe.validateConfig(candidate);
    }
    private static void check(Registry<?> registry, String entry, String label, boolean tagsAvailable) {
        if (entry.startsWith("#")) {
            if (tagsAvailable && registry.getTags().noneMatch(tag -> tag.key().location().toString().equals(entry.substring(1))))
                throw new IllegalArgumentException(label + ": 存在しないタグ: " + entry);
        } else {
            var id = Rules.id(entry);
            if (id == null || registry.getOptional(id).isEmpty()) throw new IllegalArgumentException(label + ": 登録されていないID: " + entry);
        }
    }
}
