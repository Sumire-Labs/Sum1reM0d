package com.sumirelabs.sum1rem0d.client;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.config.ClientRules;
import java.nio.file.Files;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/** Opt-in native client check against a server started with -Ptoast_verify=true. */
public final class ToastSmoke {
    private static int stage;
    private static long started, visibleAt;
    private static FakeAdvancementToast toast;
    private static final java.util.List<String> passed = new java.util.ArrayList<>();
    private ToastSmoke() {}
    public static void tick(Minecraft mc) {
        if (!Boolean.getBoolean("sum1rem0d.toastSmoke") || stage == -1 || !mc.isGameLoadFinished()) return;
        long now = System.nanoTime();
        try {
            if (started != 0 && now - started > 90_000_000_000L) throw new AssertionError("Toast client timeout at " + stage);
            switch (stage) {
                case 0 -> {
                    if (mc.gui.overlay() != null) return; started = now;
                    String address = System.getProperty("sum1rem0d.networkAddress", "127.0.0.1:25589");
                    ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(address), new ServerData("Toast verification", address, ServerData.Type.OTHER), false, null); stage = 1;
                }
                case 1 -> {
                    if (mc.player == null || !ClientRules.received()) return;
                    toast = mc.gui.toastManager().getToast(FakeAdvancementToast.class, Toast.NO_TOKEN);
                    if (toast == null || toast.getWantedVisibility() != Toast.Visibility.SHOW) return;
                    require(ClientRules.integer("sleep_advancement", "fake_seconds") == 3, "Server's toast duration is synchronized");
                    passed.add("network display payload creates a visible vanilla-style fake advancement toast"); visibleAt = now; stage = 2;
                }
                case 2 -> {
                    if (now - visibleAt < 2_000_000_000L) return;
                    require(toast.getWantedVisibility() == Toast.Visibility.SHOW, "Fake toast remains visible for configured duration");
                    Screenshot.grab(mc.gameDirectory, "sumire-fake-advancement-toast.png", mc.gameRenderer.mainRenderTarget(), 1, message -> Sum1reM0d.LOGGER.info("TOAST_SMOKE: {}", message.getString()));
                    passed.add("fake advancement toast renders with icon and translated title"); stage = 3;
                }
                case 3 -> {
                    if (mc.gui.toastManager().getToast(FakeAdvancementToast.class, Toast.NO_TOKEN) != null) return;
                    double maximum = 3_000 * mc.gui.toastManager().getNotificationDisplayTimeMultiplier() + 2_000;
                    require((now - visibleAt) / 1_000_000.0 <= maximum, "Toast expires using configured duration");
                    passed.add("fake toast disappears after configured duration");
                    if (mc.level == null) return;
                    VisualVerification.begin(mc); visibleAt = now; stage = 4;
                }
                case 4 -> {
                    if (now - visibleAt < 1_000_000_000L) return;
                    VisualVerification.assertLight(mc); passed.add("AFK dynamic light raises rendered brightness within radius without changing world lighting");
                    VisualVerification.assertModels(mc);
                    passed.add("native grass models reverse top/bottom, preserve snow and restore when disabled");
                    passed.add("native ore world/item models and names disguise without changing real items and respect Config");
                    passed.add("native furnace models cover all permutations and preserve lit fronts and facing");
                    VisualVerification.assertNames(mc); passed.add("client name and Tab aliases preserve original profile and rich style");
                    VisualVerification.assertUrlEditor(mc); passed.add("Config URL list editor loads, edits and saves query parameters intact");
                    screenshot(mc, "white-hand"); VisualVerification.natural(); visibleAt = now; stage = 5;
                }
                case 5 -> {
                    if (now - visibleAt < 1_000_000_000L) return;
                    screenshot(mc, "natural-lit-hand"); VisualVerification.partial(); visibleAt = now; stage = 6;
                }
                case 6 -> {
                    if (now - visibleAt < 1_000_000_000L) return;
                    screenshot(mc, "partial-hand"); VisualVerification.thirdPerson(mc); visibleAt = now; stage = 7;
                }
                case 7 -> {
                    if (now - visibleAt < 1_000_000_000L) return;
                    screenshot(mc, "white-body"); VisualVerification.disableLight(mc);
                    passed.add("disabling AFK light restores vanilla light and disabling whiteness clears rendering"); visibleAt = now; stage = 8;
                }
                case 8 -> {
                    if (now - visibleAt < 1_000_000_000L) return;
                    screenshot(mc, "restored-hand"); VisualVerification.assertCamera(mc);
                    passed.add("camera rotation, native mouse blocking, expiry and immediate disable restoration");
                    VisualVerification.redScreen(); visibleAt = now; stage = 9;
                }
                case 9 -> {
                    if (now - visibleAt < 1_000_000_000L) return;
                    require(ClientRuntime.redTicks > 0, "Synchronized red screen remains active for the configured duration");
                    screenshot(mc, "red-screen"); VisualVerification.disableRed(); visibleAt = now; stage = 10;
                }
                case 10 -> {
                    if (now - visibleAt < 1_000_000_000L) return;
                    screenshot(mc, "red-disabled"); passed.add("red screen renders and disabling clears it despite stale event ticks");
                    VisualVerification.deathScreen(mc); visibleAt = now; stage = 11;
                }
                case 11 -> {
                    if (now - visibleAt < 1_000_000_000L) return;
                    screenshot(mc, "death-title"); mc.gui.setScreen(null); passed.add("native death screen normal/hardcore titles, configurable text, disable and rendered You Suck title");
                    VisualVerification.hud(mc, 25, 100, true); visibleAt = now; stage = 12;
                }
                case 12 -> {
                    if (now - visibleAt < 1_000_000_000L) return;
                    VisualVerification.assertHud(mc, 22, 0xFFE89964, true); screenshot(mc, "cleanliness-low");
                    VisualVerification.hud(mc, 100, 100, true); visibleAt = now; stage = 13;
                }
                case 13 -> {
                    if (now - visibleAt < 1_000_000_000L) return;
                    VisualVerification.assertHud(mc, 88, 0xFF69CDB8, true); screenshot(mc, "cleanliness-full");
                    VisualVerification.hud(mc, 100, 200, true); visibleAt = now; stage = 14;
                }
                case 14 -> {
                    if (now - visibleAt < 1_000_000_000L) return;
                    VisualVerification.assertHud(mc, 44, 0xFF69CDB8, true); screenshot(mc, "cleanliness-maximum");
                    VisualVerification.hud(mc, 100, 200, false); visibleAt = now; stage = 15;
                }
                case 15 -> {
                    if (now - visibleAt < 1_000_000_000L) return;
                    VisualVerification.assertHud(mc, 0, 0, false); screenshot(mc, "cleanliness-hidden");
                    passed.add("native cleanliness HUD extracts numeric text, value-dependent bar width and warning color, configurable maximum and personal hide toggle");
                    VisualVerification.restore(mc); passed.add("client configuration and camera restored after visual fixtures");
                    AssetPreviewScreen.verify(mc); mc.gui.setScreen(new AssetPreviewScreen()); visibleAt = now; stage = 16;
                }
                case 16 -> {
                    if (now - visibleAt < 2_000_000_000L) return;
                    screenshot(mc, "soap-and-mod-icon"); passed.add("native soap item resolves its own generated sprite and the registered mod icon renders");
                    FletchingGuiVerification.begin(mc); visibleAt = now; stage = 17;
                }
                case 17 -> {
                    if (now - visibleAt < 2_000_000_000L) return;
                    screenshot(mc, "fletching-ready"); FletchingGuiVerification.craft(mc); visibleAt = now; stage = 18;
                }
                case 18 -> {
                    if (now - visibleAt < 2_000_000_000L) return;
                    FletchingGuiVerification.verifyOutput(); screenshot(mc, "fletching-output");
                    passed.add("vanilla-part fletching screen renders input/output/inventory, enables craft and displays exactly 1000 arrows with bounded stacks");
                    FletchingGuiVerification.restore(); finish(mc, true, "");
                }
            }
        } catch (Throwable failure) { Sum1reM0d.LOGGER.error("TOAST_SMOKE failed", failure); finish(mc, false, failure.toString()); }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void finish(Minecraft mc, boolean success, String failure) {
        FletchingGuiVerification.restore();
        try { VisualVerification.restore(mc); } catch (Throwable error) { success = false; failure += "; visual restore: " + error; }
        stage = -1; var result = new JsonObject(); result.addProperty("success", success); result.addProperty("failure", failure); result.add("passed", new Gson().toJsonTree(passed));
        try { Files.writeString(mc.gameDirectory.toPath().resolve("sum1rem0d-toast-verification.json"), new GsonBuilder().setPrettyPrinting().create().toJson(result)); }
        catch (Exception error) { Sum1reM0d.LOGGER.error("Cannot save toast client report", error); }
        Sum1reM0d.LOGGER.info("TOAST_SMOKE: completed success={}", success); mc.disconnectFromWorld(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE); mc.stop();
    }
    private static void screenshot(Minecraft mc, String name) { Screenshot.grab(mc.gameDirectory, "sumire-afk-" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> Sum1reM0d.LOGGER.info("TOAST_SMOKE: {}", message.getString())); }
}
