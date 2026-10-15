package com.sumirelabs.sum1rem0d.game;

import java.util.*;

/** Seeded permutations need no persisted state and survive server restarts. */
public final class OreDropMapping {
    private OreDropMapping() {}
    public static final Set<String> MODES = Set.of("fixed", "world", "daily", "mining");

    public static String output(List<String> entries, String block, String mode, long worldSeed, long day, int miningChoice) {
        List<String> sources = new ArrayList<>(), outputs = new ArrayList<>();
        for (String entry : entries) {
            String[] pair = entry.split("=", 2);
            sources.add(pair[0].trim()); outputs.add(pair[1].trim());
        }
        int index = sources.indexOf(block);
        if (index < 0) return null;
        switch (mode) {
            case "fixed" -> { return outputs.get(index); }
            case "mining" -> { return outputs.get(Math.floorMod(miningChoice, outputs.size())); }
            case "world", "daily" -> {
                long seed = worldSeed ^ 0x43DB7BA36E2C914FL;
                if (mode.equals("daily")) seed ^= day * 0x9E3779B97F4A7C15L;
                Collections.shuffle(outputs, new Random(seed));
                return outputs.get(index);
            }
            default -> throw new IllegalArgumentException("Unknown ore drop mode: " + mode);
        }
    }
}
