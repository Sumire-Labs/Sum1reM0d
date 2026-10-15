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
        return feature.equals("wiki") || feature.equals("recipe_viewers") || feature.equals("youtube") && !option.equals("enabled")
            || feature.equals("economy") && option.equals("hud") || feature.equals("cleanliness") && option.equals("hud") || feature.equals("advertisements") && option.equals("volume");
    }
}
