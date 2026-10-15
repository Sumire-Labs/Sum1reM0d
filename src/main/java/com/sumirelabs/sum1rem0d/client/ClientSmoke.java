package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.config.ConfigSchema;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;

/** Explicit development-only launch flag; never runs in normal game sessions. */
public final class ClientSmoke {
    private static int stage;
    private static long changedAt;
    private static long browserReadyAt;
    private static java.util.concurrent.CompletableFuture<String> pageProbe;
    private static com.google.gson.JsonObject pageState;
    private static boolean mappingVerified;
    private static Screen title;
    private static ConfigScreen config;
    private static FeatureScreen feature;
    private static BrowserCheck browser;
    private ClientSmoke() {}
    public static void tick(Minecraft mc) {
        if (!Boolean.getBoolean("sum1rem0d.clientSmoke") || !mc.isGameLoadFinished() || mc.gui.overlay() != null) return;
        long now = System.nanoTime();
        if (stage == 0) { title = new TitleScreen(); mc.gui.setScreen(title); changedAt = now; stage = 1; return; }
        if (now - changedAt < 3_000_000_000L) return;
        switch (stage) {
            case 1 -> { screenshot(mc, "01-title"); config = new ConfigScreen(title); mc.gui.setScreen(config); }
            case 2 -> { screenshot(mc, "02-config"); feature = new FeatureScreen(config, ConfigSchema.features().stream().filter(value -> value.id().equals("vegan")).findFirst().orElseThrow()); mc.gui.setScreen(feature); }
            case 3 -> { screenshot(mc, "03-feature"); var f = ConfigSchema.features().stream().filter(value -> value.id().equals("vegan")).findFirst().orElseThrow(); mc.gui.setScreen(new ListOptionScreen(feature, config, f, f.options().stream().filter(o -> o.key().equals("forbidden_items")).findFirst().orElseThrow())); }
            case 4 -> {
                screenshot(mc, "04-list");
                try { ConfigGuiVerification.verifyMap(mc); mappingVerified = true; }
                catch (Throwable error) { Sum1reM0d.LOGGER.error("CLIENT_SMOKE mapping GUI failed", error); finish(mc, false, error.toString()); return; }
                var f = ConfigSchema.features().stream().filter(value -> value.id().equals("vegan")).findFirst().orElseThrow(); mc.gui.setScreen(new OptionScreen(feature, config, f, f.options().stream().filter(o -> o.key().equals("chance")).findFirst().orElseThrow()));
            }
            case 5 -> { screenshot(mc, "06-option"); browser = new BrowserCheck(); mc.gui.setScreen(browser); }
            case 6 -> {
                if (now - changedAt > 300_000_000_000L) { finish(mc, false, "Browser verification timed out"); return; }
                if (!browser.session.ready() && !browser.session.failed()) {
                    if (now - changedAt < 300_000_000_000L) return;
                    finish(mc, false, "MCEF timed out"); return;
                }
                if (browser.session.failed()) { finish(mc, false, "MCEF initialization failed"); return; }
                // Initialization completes before the first native paint is delivered.
                if (browserReadyAt == 0) { browserReadyAt = now; return; }
                if (now - browserReadyAt < 3_000_000_000L) return;
                if (pageProbe == null) {
                    var params = new com.google.gson.JsonObject();
                    params.addProperty("expression", "(()=>{const m=document.querySelector('main');const r=m?.getBoundingClientRect();return {width:innerWidth,height:innerHeight,text:document.body?.innerText,rect:r?{x:r.x,y:r.y,width:r.width,height:r.height}:null}})()");
                    params.addProperty("returnByValue", true);
                    pageProbe = browser.session.cef().getDevToolsClient().executeDevToolsMethod("Runtime.evaluate", params.toString());
                    return;
                }
                if (!pageProbe.isDone()) {
                    if (now - browserReadyAt > 20_000_000_000L) finish(mc, false, "Browser page probe timed out");
                    return;
                }
                pageState = com.google.gson.JsonParser.parseString(pageProbe.join()).getAsJsonObject().getAsJsonObject("result").getAsJsonObject("value");
                Sum1reM0d.LOGGER.info("CLIENT_SMOKE: page={}", pageState);
                var texture = ((net.dimaskama.mcef.api.MCEFBrowser) browser.session.cef()).getTexture();
                int expectedWidth = (browser.width - 40) * (int) mc.getWindow().getGuiScale();
                int expectedHeight = (browser.height - 68) * (int) mc.getWindow().getGuiScale();
                Sum1reM0d.LOGGER.info("CLIENT_SMOKE: texture={}x{}, expected={}x{}", texture.getWidth(0), texture.getHeight(0), expectedWidth, expectedHeight);
                if (texture.getWidth(0) != expectedWidth || texture.getHeight(0) != expectedHeight) {
                    finish(mc, false, "Browser texture does not match its configured viewport"); return;
                }
                screenshot(mc, "05-browser");
                Sum1reM0d.LOGGER.info("CLIENT_SMOKE: browser ready={}, failed={}", browser.session.ready(), browser.session.failed());
                try { Class.forName("net.minecraft.client.gui.screens.DeathScreen"); }
                catch (ClassNotFoundException exception) { throw new IllegalStateException(exception); }
            }
            case 7 -> { finish(mc, true, ""); return; }
            default -> { return; }
        }
        stage++; changedAt = now;
    }
    private static void finish(Minecraft mc, boolean success, String failure) {
        var result = new com.google.gson.JsonObject();
        result.addProperty("success", success);
        result.addProperty("failure", failure);
        result.addProperty("browserReady", browser != null && browser.session.ready());
        result.addProperty("browserFailed", browser != null && browser.session.failed());
        result.addProperty("mappingVerified", mappingVerified);
        if (pageState != null) result.add("page", pageState);
        result.add("screenshots", new com.google.gson.Gson().toJsonTree(java.util.List.of(
                "sumire-smoke-01-title.png", "sumire-smoke-02-config.png", "sumire-smoke-03-feature.png",
                "sumire-smoke-04-list.png", "sumire-smoke-06-option.png", "sumire-smoke-05-browser.png")));
        try { java.nio.file.Files.writeString(mc.gameDirectory.toPath().resolve("sum1rem0d-client-verification.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result)); }
        catch (java.io.IOException error) { throw new IllegalStateException("Cannot save client verification report", error); }
        stage = -1;
        mc.gui.setScreen(title); BrowserOverlays.closeAll();
        Sum1reM0d.LOGGER.info("CLIENT_SMOKE: completed success={}", success); mc.stop();
    }
    private static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "sumire-smoke-" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> Sum1reM0d.LOGGER.info("CLIENT_SMOKE: {}", message.getString()));
    }
    private static final class BrowserCheck extends Screen {
        private final BrowserSession session = new BrowserSession(BrowserOverlays.defaultAd());
        BrowserCheck() { super(Component.literal("MCEF 動作確認")); }
        @Override protected void init() { addRenderableWidget(Button.builder(Component.literal("閉じる"), button -> onClose()).bounds(width / 2 - 50, height - 26, 100, 20).build()); }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float tick) {
            super.extractRenderState(graphics, x, y, tick); graphics.centeredText(font, title, width / 2, 12, -1);
            session.draw(graphics, 20, 32, width - 40, height - 68, 0);
        }
        @Override public void removed() { session.close(); }
    }
}
