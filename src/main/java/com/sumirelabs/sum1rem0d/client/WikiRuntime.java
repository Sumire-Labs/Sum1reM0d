package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.wiki.WikiContent;
import net.minecraft.client.Minecraft;

/** One welcome per connection, deferred until loading and other screens finish. */
public final class WikiRuntime {
    private static Object connection;
    private static boolean completed;
    private static int readyTicks;
    private WikiRuntime() {}
    public static boolean enabled() { return PersonalConfig.store().enabled("wiki"); }
    static void reset() { connection = null; completed = false; readyTicks = 0; }
    static void tick(Minecraft mc) {
        if (java.util.List.of("clientSmoke", "networkSmoke", "browserSmoke", "toastSmoke", "viewerSmoke", "multiSmoke", "economySmoke", "coinServicesSmoke", "chatSmoke").stream().anyMatch(name -> Boolean.getBoolean("sum1rem0d." + name))) return;
        Object next = mc.getConnection();
        if (connection != next) { connection = next; completed = false; readyTicks = 0; }
        if (connection == null || completed || mc.level == null || mc.player == null || !ClientRules.received() || !ClientGameState.received()) return;
        if (mc.gui.screen() instanceof WikiScreen) { completed = true; return; }
        var config = PersonalConfig.store();
        if (!WikiContent.autoEligible(enabled(), config.bool("wiki", "auto_show"), config.bool("wiki", "first_join_only"), config.bool("wiki", "seen"))) { completed = true; return; }
        readyTicks++;
        if (readyTicks < config.integer("wiki", "delay_seconds") * 20 || !mc.player.isAlive() || mc.gui.overlay() != null || mc.gui.screen() != null) return;
        completed = true;
        mc.gui.setScreen(new WikiScreen(null));
        if (!config.bool("wiki", "seen")) {
            var draft = config.snapshot(); draft.getAsJsonObject("features").getAsJsonObject("wiki").addProperty("seen", true);
            try { config.update(draft, config.revision()); }
            catch (Exception failure) { Sum1reM0d.LOGGER.warn("Cannot save Wiki read status", failure); }
        }
    }
}
