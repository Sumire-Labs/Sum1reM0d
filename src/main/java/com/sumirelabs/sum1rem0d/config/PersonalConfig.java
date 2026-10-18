package com.sumirelabs.sum1rem0d.config;

import java.nio.file.Path;

public final class PersonalConfig {
    private static ConfigStore store;
    private static boolean viewerRoll;
    private PersonalConfig() {}
    public static void initialize(Path directory) {
        store = new ConfigStore(directory.resolve("sum1rem0d-client.json5")); store.load();
        migrateBackToolLayout();
        migrateInventoryMedia();
        viewerRoll = java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 100 < store.number("recipe_viewers", "chance");
    }
    private static void migrateBackToolLayout() {
        var root = store.snapshot();
        if (root.has("back_tool_layout_revision")) return;
        var tool = root.getAsJsonObject("features").getAsJsonObject("last_tool_back");
        if (tool.get("scale").getAsDouble() == .45) tool.addProperty("scale", .60);
        if (tool.get("rotation").getAsDouble() == 90) tool.addProperty("rotation", 100);
        root.addProperty("back_tool_layout_revision", 1);
        try { store.update(root, store.revision()); }
        catch (java.io.IOException e) { throw new IllegalStateException("Cannot update back tool layout", e); }
    }
    private static void migrateInventoryMedia(){
        var root=store.snapshot();if(root.has("inventory_media_revision"))return;
        root.getAsJsonObject("features").getAsJsonObject("youtube").addProperty("stop_on_close",false);
        root.addProperty("inventory_media_revision",1);
        try{store.update(root,store.revision());}catch(java.io.IOException e){throw new IllegalStateException("Cannot update persistent inventory media settings",e);}
    }
    public static ConfigStore store() { if (store == null) throw new IllegalStateException("Client configuration is not initialized"); return store; }
    public static boolean viewersBlocked() { return store != null && store.enabled("recipe_viewers") && viewerRoll; }
    public static boolean personal(String feature, String option) {
        return feature.equals("second_offhand") && option.startsWith("slot_") || feature.equals("player_trading") && option.startsWith("toast_") || feature.equals("no_chat_reports") && option.equals("suppress_signing") || feature.equals("wiki") || feature.equals("recipe_viewers") || feature.equals("youtube") && !option.equals("enabled")
            || feature.equals("economy") && option.startsWith("hud") || feature.equals("cleanliness") && option.startsWith("hud") || feature.equals("advertisements") && option.equals("volume") || feature.equals("pvp_death_media") && (option.equals("local_file") || option.equals("volume")) || feature.equals("japanese_ime") || feature.equals("last_tool_back") || feature.equals("furnace_info") || feature.equals("programmer_pufferfish") || feature.equals("tab_ping") || feature.equals("clock_ticks");
    }
}
