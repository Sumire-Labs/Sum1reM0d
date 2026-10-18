package com.sumirelabs.sum1rem0d.config;

import java.nio.file.Path;

public final class PersonalConfig {
    private static ConfigStore store;
    private static boolean viewerRoll;
    private PersonalConfig() {}
    public static void initialize(Path directory) {
        store = new ConfigStore(directory.resolve("sum1rem0d-client.json5")); store.load();
        viewerRoll = java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 100 < store.number("recipe_viewers", "chance");
    }
    public static ConfigStore store() { if (store == null) throw new IllegalStateException("Client configuration is not initialized"); return store; }
    public static boolean viewersBlocked() { return store != null && store.enabled("recipe_viewers") && viewerRoll; }
    public static boolean personal(String feature, String option) {
        return feature.equals("player_trading") && option.startsWith("toast_") || feature.equals("no_chat_reports") && option.equals("suppress_signing") || feature.equals("wiki") || feature.equals("recipe_viewers") || feature.equals("youtube") && !option.equals("enabled")
            || feature.equals("economy") && option.startsWith("hud") || feature.equals("cleanliness") && option.startsWith("hud") || feature.equals("advertisements") && option.equals("volume") || feature.equals("pvp_death_media") && (option.equals("local_file") || option.equals("volume")) || feature.equals("japanese_ime") || feature.equals("last_tool_back") || feature.equals("furnace_info") || feature.equals("programmer_pufferfish") || feature.equals("tab_ping") || feature.equals("clock_ticks");
    }
}
