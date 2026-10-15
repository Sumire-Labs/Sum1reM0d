package com.sumirelabs.sum1rem0d.client;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.wiki.WikiContent;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/** Explicit local verifier: no OP required, three actual connections, settings restored. */
final class WikiSmoke {
    private static int stage;
    private static long changedAt;
    private static JsonObject original;
    private static boolean primed;
    private static final List<String> passed = new ArrayList<>();
    private WikiSmoke() {}
    static void tick(Minecraft mc) {
        if (!Boolean.getBoolean("sum1rem0d.wikiSmoke") || stage < 0 || !mc.isGameLoadFinished()) return;
        long now = System.nanoTime();
        try {
            if (changedAt != 0 && now - changedAt > 90_000_000_000L) throw new AssertionError("Wiki timeout at " + stage);
            if (changedAt != 0 && now - changedAt < 1_500_000_000L) return;
            switch (stage) {
                case 0 -> {
                    if (mc.gui.overlay() != null) return;
                    original = PersonalConfig.store().snapshot(); configure(true, true, false); WikiRuntime.reset(); connect(mc); advance(now);
                }
                case 1 -> {
                    if (!(mc.gui.screen() instanceof WikiScreen wiki)) return;
                    if (!primed) { mc.gui.toastManager().clear(); primed = true; changedAt = now; return; }
                    require(ClientRules.received() && ClientGameState.received() && mc.player != null && wiki.view().equals("intro"), "Welcome waits for actual world and rule synchronization");
                    require(PersonalConfig.store().bool("wiki", "seen"), "Automatic display marks the guide read");
                    var disk = com.sumirelabs.sum1rem0d.config.Json5Config.read(Files.readString(mc.gameDirectory.toPath().resolve("config/sum1rem0d-client.json5")));
                    require(disk.getAsJsonObject("features").getAsJsonObject("wiki").get("seen").getAsBoolean(), "Read status is saved to the actual client config");
                    passed.add("first welcome opens after world synchronization and persists read status");
                    mc.gui.toastManager().clear(); screenshot(mc, "introduction"); press(wiki, "精錬とクラフト", false); advance(now);
                }
                case 2 -> {
                    var wiki = current(mc); require(wiki.view().equals("category"), "Category button opens the feature list"); screenshot(mc, "category");
                    press(wiki, WikiContent.article("fletching").title(), true); advance(now);
                }
                case 3 -> {
                    var wiki = current(mc); require(wiki.feature().equals("fletching"), "Feature button opens its article"); screenshot(mc, "fletching");
                    if (wiki.pages() > 1) { wiki.mouseScrolled(0, 0, 0, -1); require(wiki.page() == 1, "Mouse wheel advances article pagination"); }
                    var input = wiki.children().stream().filter(child -> child instanceof EditBox).map(child -> (EditBox) child).findFirst().orElseThrow();
                    input.setValue("石鹸"); press(wiki, "検索", false); passed.add("native category/article navigation and pagination work"); advance(now);
                }
                case 4 -> {
                    var wiki = current(mc); require(wiki.view().equals("search"), "Search button opens results"); screenshot(mc, "search");
                    press(wiki, WikiContent.article("cleanliness").title(), true); advance(now);
                }
                case 5 -> {
                    var wiki = current(mc); require(wiki.feature().equals("cleanliness"), "Japanese search finds soap documentation"); screenshot(mc, "soap");
                    require(!ClientRules.editable, "Wiki test connection has no OP permissions");
                    press(wiki, "サーバー設定", false); require(mc.gui.screen() instanceof ConfigScreen config && !config.editable(), "Read-only server settings remain viewable without OP");
                    mc.gui.setScreen(wiki); press(wiki, "閉じる", false);
                    for (int tick = 0; tick < 60; tick++) WikiRuntime.tick(mc);
                    require(mc.gui.screen() == null, "Closing the guide never reopens it in the same connection");
                    passed.add("Japanese search, soap article and permission-safe settings access work; closing stays closed");
                    mc.disconnectFromWorld(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE); advance(now);
                }
                case 6 -> { connect(mc); advance(now); }
                case 7 -> {
                    if (mc.player == null || !ClientRules.received() || !ClientGameState.received() || mc.player.tickCount < 60) return;
                    require(!(mc.gui.screen() instanceof WikiScreen), "First-only mode suppresses welcome on a real reconnect");
                    passed.add("first-only preference suppresses welcome after reconnect using saved read status");
                    mc.gui.setScreen(new PauseScreen(true)); press(mc.gui.screen(), "Sum1reM0d Wiki", false);
                    require(mc.gui.screen() instanceof WikiScreen, "Pause menu can reopen the guide manually"); passed.add("native pause-menu button reopens Wiki manually");
                    configure(true, false, true); mc.disconnectFromWorld(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE); advance(now);
                }
                case 8 -> { connect(mc); advance(now); }
                case 9 -> {
                    if (!(mc.gui.screen() instanceof WikiScreen wiki)) return;
                    passed.add("every-join mode reopens the guide on the next actual connection even when already read");
                    configure(false, false, true); wiki.tick(); require(!(mc.gui.screen() instanceof WikiScreen), "Disabling Wiki closes the active reader");
                    passed.add("individual Config switch disables the active Wiki screen"); finish(mc, true, "");
                }
            }
        } catch (Throwable failure) { Sum1reM0d.LOGGER.error("WIKI_SMOKE failed", failure); finish(mc, false, failure.toString()); }
    }
    private static void configure(boolean enabled, boolean firstOnly, boolean seen) throws Exception {
        var store = PersonalConfig.store(); var draft = store.snapshot(); var wiki = draft.getAsJsonObject("features").getAsJsonObject("wiki");
        wiki.addProperty("enabled", enabled); wiki.addProperty("auto_show", true); wiki.addProperty("first_join_only", firstOnly); wiki.addProperty("seen", seen); wiki.addProperty("delay_seconds", 0); store.update(draft, store.revision());
    }
    private static WikiScreen current(Minecraft mc) { if (!(mc.gui.screen() instanceof WikiScreen wiki)) throw new AssertionError("Wiki reader is not open"); return wiki; }
    private static void press(Screen screen, String text, boolean prefix) {
        var button = screen.children().stream().filter(child -> child instanceof Button item && (prefix ? item.getMessage().getString().startsWith(text) : item.getMessage().getString().equals(text))).map(child -> (Button) child).findFirst().orElseThrow();
        require(button.active, "Wiki control must be active: " + text); button.onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
    }
    private static void connect(Minecraft mc) { String address = System.getProperty("sum1rem0d.networkAddress", "127.0.0.1:25589"); ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(address), new ServerData("Wiki verification", address, ServerData.Type.OTHER), false, null); }
    private static void advance(long now) { stage++; changedAt = now; }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void screenshot(Minecraft mc, String name) { net.minecraft.client.Screenshot.grab(mc.gameDirectory, "sumire-wiki-" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> Sum1reM0d.LOGGER.info("WIKI_SMOKE: {}", message.getString())); }
    private static void finish(Minecraft mc, boolean success, String failure) {
        stage = -1;
        if (original != null) try { PersonalConfig.store().update(original, PersonalConfig.store().revision()); passed.add("original personal settings restored"); } catch (Exception error) { success = false; failure += "; restore: " + error; }
        var result = new JsonObject(); result.addProperty("success", success); result.addProperty("failure", failure); result.add("passed", new Gson().toJsonTree(passed));
        try { Files.writeString(mc.gameDirectory.toPath().resolve("sum1rem0d-wiki-verification.json"), new GsonBuilder().setPrettyPrinting().create().toJson(result)); } catch (Exception error) { throw new IllegalStateException(error); }
        Sum1reM0d.LOGGER.info("WIKI_SMOKE: completed success={}", success); mc.disconnectFromWorld(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE); mc.stop();
    }
}
