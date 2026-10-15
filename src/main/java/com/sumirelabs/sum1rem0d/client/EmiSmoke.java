package com.sumirelabs.sum1rem0d.client;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.network.ConfigStatePayload;
import java.nio.file.Files;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/** Native optional-EMI integration check; invoked only with explicit viewer smoke flags. */
public final class EmiSmoke {
    private static int stage;
    private static long started, changed;
    private static JsonObject personal, rules;
    private static EditBox search;
    private static String originalSearch, blockedSearch;
    private static Class<?> manager, api;
    private static final java.util.List<String> passed = new ArrayList<>();
    private EmiSmoke() {}
    public static void tick(Minecraft mc) {
        if (!Boolean.getBoolean("sum1rem0d.viewerSmoke") || !System.getProperty("sum1rem0d.viewer", "jei").equals("emi") || stage < 0 || !mc.isGameLoadFinished()) return;
        long now = System.nanoTime();
        try {
            if (started != 0 && now - started > 120_000_000_000L) throw new AssertionError("EMI verification timeout at " + stage);
            switch (stage) {
                case 0 -> {
                    if (mc.gui.overlay() != null) return; started = now;
                    String address = System.getProperty("sum1rem0d.networkAddress", "127.0.0.1:25589");
                    ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(address), new ServerData("EMI verification", address, ServerData.Type.OTHER), false, null); stage = 1;
                }
                case 1 -> {
                    if (mc.player == null || !ClientRules.received()) return;
                    if (!(Boolean) Class.forName("dev.emi.emi.runtime.EmiReloadManager").getMethod("isLoaded").invoke(null)) return;
                    manager = Class.forName("dev.emi.emi.screen.EmiScreenManager"); api = Class.forName("dev.emi.emi.api.EmiApi"); search = (EditBox) manager.getField("search").get(null);
                    personal = PersonalConfig.store().snapshot(); rules = ClientRules.snapshot(); originalSearch = search.getValue();
                    var fixture = rules.deepCopy(); fixture.getAsJsonObject("features").getAsJsonObject("youtube").addProperty("enabled", false); fixture.getAsJsonObject("features").getAsJsonObject("advertisements").addProperty("enabled", false);
                    ClientRuntime.acceptRules(new ConfigStatePayload(ClientRules.revision, ClientRules.editable, fixture.toString(), "", 0, false));
                    toggle(false); mc.gui.setScreen(new InventoryScreen(mc.player)); changed = now; stage = 2;
                }
                case 2 -> {
                    if (now - changed < 2_000_000_000L) return;
                    require(!(Boolean) manager.getMethod("isDisabled").invoke(null), "EMI overlay is enabled in the baseline");
                    search.setValue(""); type(mc, 'a'); require(search.getValue().contains("a"), "Real EMI search receives native click and character input"); shot(mc, "enabled");
                    api.getMethod("displayAllRecipes").invoke(null); require(recipeScreen(mc), "Real EMI recipe API opens a recipe screen");
                    passed.add("real EMI loads, overlay is enabled, search receives native input and recipe API opens its screen");
                    mc.gui.setScreen(new InventoryScreen(mc.player)); search.setFocused(true); blockedSearch = search.getValue(); toggle(true); changed = now; stage = 3;
                }
                case 3 -> {
                    if (now - changed < 1_000_000_000L) return;
                    require((Boolean) manager.getMethod("isDisabled").invoke(null), "Startup roll disables actual EMI overlay");
                    require(!search.isFocused(), "Blocked EMI search releases focus");
                    type(mc, 'z'); require(search.getValue().equals(blockedSearch), "Blocked EMI cannot receive search input");
                    api.getMethod("displayAllRecipes").invoke(null); require(mc.gui.screen() instanceof InventoryScreen, "Blocked EMI recipe API cannot replace the inventory screen");
                    require(Boolean.FALSE.equals(manager.getMethod("mouseClicked", MouseButtonEvent.class).invoke(null, new Object[]{null})), "Blocked EMI mouse callback is bypassed before processing its arguments");
                    require(Boolean.FALSE.equals(manager.getMethod("keyPressed", KeyEvent.class).invoke(null, new Object[]{null})), "Blocked EMI key callback is bypassed before processing its arguments");
                    passed.add("startup roll disables actual EMI overlay, releases focus, blocks native search and recipe screens and bypasses input callbacks"); shot(mc, "blocked");
                    toggle(false); changed = now; stage = 4;
                }
                case 4 -> {
                    if (now - changed < 1_000_000_000L) return;
                    require(!(Boolean) manager.getMethod("isDisabled").invoke(null), "Config disable restores EMI without restart"); api.getMethod("displayAllRecipes").invoke(null); require(recipeScreen(mc), "EMI recipes reopen after blocking is disabled");
                    passed.add("Config disable restores EMI overlay and recipe API without restarting"); finish(mc, true, "");
                }
            }
        } catch (Throwable failure) { Sum1reM0d.LOGGER.error("EMI_SMOKE failed", failure); finish(mc, false, failure.toString()); }
    }
    private static void toggle(boolean value) throws Exception {
        var store = PersonalConfig.store(); var draft = store.snapshot(); var options = draft.getAsJsonObject("features").getAsJsonObject("recipe_viewers"); options.addProperty("enabled", value); options.addProperty("target_mods", "emi"); store.update(draft, store.revision());
        if (value) require(PersonalConfig.viewersBlocked(), "EMI smoke requires startup chance 100");
    }
    private static void type(Minecraft mc, char character) {
        var screen = mc.gui.screen(); double x = (search.getX() + search.getWidth() / 2.0) * mc.getWindow().getScreenWidth() / screen.width;
        double y = (search.getY() + search.getHeight() / 2.0) * mc.getWindow().getScreenHeight() / screen.height;
        mc.mouseHandler.onMove(mc.getWindow().handle(), x, y, 0, 0); var button = new MouseButtonInfo(org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT, 0);
        mc.mouseHandler.onButton(mc.getWindow().handle(), button, 1); mc.mouseHandler.onButton(mc.getWindow().handle(), button, 0);
        mc.keyboardHandler.charTyped(mc.getWindow().handle(), new CharacterEvent(character));
    }
    private static boolean recipeScreen(Minecraft mc) { return mc.gui.screen() != null && mc.gui.screen().getClass().getName().startsWith("dev.emi.emi."); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void shot(Minecraft mc, String name) { Screenshot.grab(mc.gameDirectory, "sumire-viewer-emi-" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> Sum1reM0d.LOGGER.info("EMI_SMOKE: {}", message.getString())); }
    private static void finish(Minecraft mc, boolean success, String failure) {
        stage = -1;
        try { if (personal != null) PersonalConfig.store().update(personal, PersonalConfig.store().revision()); if (search != null && originalSearch != null) { search.setFocused(false); search.setValue(originalSearch); } if (rules != null) ClientRuntime.acceptRules(new ConfigStatePayload(ClientRules.revision, ClientRules.editable, rules.toString(), "", 0, false)); }
        catch (Exception error) { success = false; failure += "; restore: " + error; }
        var result = new JsonObject(); result.addProperty("success", success); result.addProperty("failure", failure); result.add("passed", new Gson().toJsonTree(passed));
        try { Files.writeString(mc.gameDirectory.toPath().resolve("sum1rem0d-emi-verification.json"), new GsonBuilder().setPrettyPrinting().create().toJson(result)); } catch (Exception error) { Sum1reM0d.LOGGER.error("Cannot save EMI report", error); }
        mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE); mc.stop();
    }
}
