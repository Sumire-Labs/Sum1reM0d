package com.sumirelabs.sum1rem0d.game;

import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.config.ClientRules;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class Rules {
    private Rules() {}
    private static boolean remote() { return ClientRules.received() && !WorldState.serverThread(); }
    public static boolean on(String feature) { return Sum1reM0d.initialized() && (remote() ? ClientRules.enabled(feature) : Sum1reM0d.config().enabled(feature)); }
    public static int i(String feature, String key) { return remote() ? ClientRules.integer(feature, key) : Sum1reM0d.config().integer(feature, key); }
    public static double n(String feature, String key) { return remote() ? ClientRules.number(feature, key) : Sum1reM0d.config().number(feature, key); }
    public static boolean b(String feature, String key) { return remote() ? ClientRules.bool(feature, key) : Sum1reM0d.config().bool(feature, key); }
    public static String s(String feature, String key) { return remote() ? ClientRules.text(feature, key) : Sum1reM0d.config().text(feature, key); }
    public static List<String> list(String feature, String key) { return remote() ? ClientRules.list(feature, key) : Sum1reM0d.config().list(feature, key); }
    public static boolean roll(net.minecraft.util.RandomSource random, String feature) { return random.nextDouble() * 100 < n(feature, "chance"); }
    public static Identifier id(String value) { return Identifier.tryParse(value); }
    public static boolean matches(BlockState block, List<String> targets) {
        Identifier actual = BuiltInRegistries.BLOCK.getKey(block.getBlock());
        for (String raw : targets) {
            if (raw.startsWith("#")) {
                Identifier tag = id(raw.substring(1));
                if (tag != null && block.is(TagKey.create(Registries.BLOCK, tag))) return true;
            } else if (raw.equals(actual.toString())) return true;
        }
        return false;
    }
    public static boolean matches(ItemStack item, List<String> targets) {
        Identifier actual = BuiltInRegistries.ITEM.getKey(item.getItem());
        for (String raw : targets) {
            if (raw.startsWith("#")) {
                Identifier tag = id(raw.substring(1));
                if (tag != null && item.is(TagKey.create(Registries.ITEM, tag))) return true;
            } else if (raw.equals(actual.toString())) return true;
        }
        return false;
    }
}
