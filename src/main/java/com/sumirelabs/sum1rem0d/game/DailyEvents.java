package com.sumirelabs.sum1rem0d.game;

import java.util.*;

/** A single weighted draw, with a separate chance of no event. */
public final class DailyEvents {
    public static final List<String> FEATURES = List.of("vegan", "daily_fuel", "upside_down_grass", "diamond_disguise", "hardness_swap", "furnace_textures", "inventory_swap", "growth_sleep", "sideways_nylium", "retro_netherrack", "ardite_disguise");
    private DailyEvents() {}
    public static String choose(long seed, long day, double chance, Map<String, Double> weights) {
        var random = new Random(seed ^ day * 0x9E3779B97F4A7C15L ^ 0x15C7DA894B26E30FL);
        if (random.nextDouble() * 100 >= chance) return "";
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) return "";
        double draw = random.nextDouble() * total;
        for (var entry : weights.entrySet()) if ((draw -= entry.getValue()) < 0) return entry.getKey();
        return "";
    }
    public static String label(String feature) { return switch (feature) {
        case "vegan" -> "ヴィーガン"; case "daily_fuel" -> "燃費変動"; case "upside_down_grass" -> "草の上下反転";
        case "diamond_disguise" -> "ダイヤ偽装"; case "hardness_swap" -> "硬さ交換"; case "furnace_textures" -> "精錬装置の見た目交換";
        case "inventory_swap" -> "インベントリ交換"; case "growth_sleep" -> "木・作物の成長停止"; case "sideways_nylium" -> "ナイリウム横倒し"; case "retro_netherrack" -> "旧ネザーラック"; case "ardite_disguise" -> "アーダイトとマニュリン"; default -> "異変なし";
    }; }
}
