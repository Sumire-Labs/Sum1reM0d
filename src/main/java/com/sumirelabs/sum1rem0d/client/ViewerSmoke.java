package com.sumirelabs.sum1rem0d.client;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.network.ConfigStatePayload;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/** Opt-in verification with actual optional viewer jars and a local development server. */
public final class ViewerSmoke {
    private static int stage;
    private static long started, changed;
    private static JsonObject personal, rules;
    private static Object runtime, overlay, recipes;
    private static Object filter;
    private static String originalFilter;
    private static int vanillaLeft;
    private static final boolean REI = System.getProperty("sum1rem0d.viewer", "jei").equals("rei");
    private static final String ID = REI ? "roughlyenoughitems" : "jei", LABEL = REI ? "REI" : "JEI";
    private static final List<String> passed = new ArrayList<>();
    private ViewerSmoke() {}
    private static Object jeiRuntime() throws ReflectiveOperationException {
        var internal = Class.forName("mezz.jei.common.Internal");
        try { return ((Optional<?>) internal.getMethod("getOptionalJeiRuntime").invoke(null)).orElse(null); }
        catch (NoSuchMethodException olderApi) {
            try { return internal.getMethod("getJeiRuntime").invoke(null); }
            catch (java.lang.reflect.InvocationTargetException pending) {
                if (pending.getCause() instanceof IllegalStateException) return null;
                throw pending;
            }
        }
    }
    public static void tick(Minecraft mc) {
        if (!Boolean.getBoolean("sum1rem0d.viewerSmoke") || System.getProperty("sum1rem0d.viewer", "jei").equals("emi") || stage < 0 || !mc.isGameLoadFinished()) return;
        long now = System.nanoTime();
        try {
            if (started != 0 && now - started > 120_000_000_000L) throw new AssertionError("Viewer timeout at " + stage);
            switch (stage) {
                case 0 -> {
                    if (mc.gui.overlay() != null) return; started = now;
                    String address = System.getProperty("sum1rem0d.networkAddress", "127.0.0.1:25589");
                    ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(address), new ServerData("Sumire viewer verification", address, ServerData.Type.OTHER), false, null); stage = 1;
                }
                case 1 -> {
                    if (mc.player == null || !ClientRules.received()) return;
                    if (REI) {
                        if (mc.player.tickCount < 60 || (Boolean) Class.forName("me.shedaniel.rei.api.common.plugins.PluginManager").getMethod("areAnyReloading").invoke(null)) return;
                        runtime = Class.forName("me.shedaniel.rei.api.client.REIRuntime").getMethod("getInstance").invoke(null);
                        filter = call(runtime, "getSearchTextField"); if (filter == null) return;
                    } else {
                        runtime = jeiRuntime(); if (runtime == null) return;
                        overlay = call(runtime, "getIngredientListOverlay"); recipes = call(runtime, "getRecipesGui"); filter = call(runtime, "getIngredientFilter");
                    }
                    originalFilter = searchText();
                    personal = PersonalConfig.store().snapshot(); rules = ClientRules.snapshot();
                    var fixture = rules.deepCopy(); fixture.getAsJsonObject("features").getAsJsonObject("youtube").addProperty("enabled", false); fixture.getAsJsonObject("features").getAsJsonObject("advertisements").addProperty("enabled", false);
                    ClientRuntime.acceptRules(new ConfigStatePayload(ClientRules.revision, ClientRules.editable, fixture.toString(), "", 0, false));
                    toggle(false); mc.gui.setScreen(new InventoryScreen(mc.player));
                    vanillaLeft = ((com.sumirelabs.sum1rem0d.mixin.ContainerScreenAccess) mc.gui.screen()).sum1rem0d$leftPos();
                    fixture.getAsJsonObject("features").getAsJsonObject("youtube").addProperty("enabled", true);
                    ClientRuntime.acceptRules(new ConfigStatePayload(ClientRules.revision, ClientRules.editable, fixture.toString(), "", 0, false));
                    var browserOptions = PersonalConfig.store().snapshot(); browserOptions.getAsJsonObject("features").getAsJsonObject("youtube").addProperty("stop_on_close", false);
                    PersonalConfig.store().update(browserOptions, PersonalConfig.store().revision());
                    BrowserOverlays.initialize(mc.gui.screen()); changed = now; stage = 2;
                    if (REI) overlay = Class.forName("me.shedaniel.rei.impl.client.gui.ScreenOverlayImpl").getMethod("getInstance").invoke(null);
                }
                case 2 -> {
                    if (now - changed < 2_000_000_000L) return;
                    require(visible(), "Actual " + LABEL + " overlay is visible when blocking is disabled"); shot(mc, "enabled");
                    require(((com.sumirelabs.sum1rem0d.mixin.ContainerScreenAccess) mc.gui.screen()).sum1rem0d$leftPos() == vanillaLeft, "Browser does not move the vanilla container");
                    require(BrowserOverlays.youtubeSession() != null && BrowserOverlays.layout(mc.gui.screen()).visible(), "Browser and recipe viewer coexist beside the unchanged container");
                    if (!REI) {
                        var screenHelper = call(runtime, "getScreenHelper");
                        var areaStream = (java.util.stream.Stream<?>) screenHelper.getClass().getMethod("getGuiExclusionAreas", Screen.class).invoke(screenHelper, mc.gui.screen());
                        var areas = areaStream.toList(); areaStream.close();
                        int[] browserArea = BrowserOverlays.exclusion();
                        require(areas.stream().anyMatch(area -> {
                            try { return ((Number) call(area, "getX")).intValue() == browserArea[0] && ((Number) call(area, "getWidth")).intValue() == browserArea[2]; }
                            catch (Exception failure) { throw new IllegalStateException(failure); }
                        }), "JEI excludes the actual browser bounds from its ingredient and bookmark grids");
                    }
                    passed.add("browser preserves vanilla inventory position beside the actual recipe viewer; JEI reserves its bounds");
                    setSearch(""); typeSearch(mc, 'q');
                    require(searchText().equals("q"), "Actual KeyboardHandler focuses and types into unblocked " + LABEL + " search");
                    showRecipes(); require(recipeScreen(mc), "Actual " + LABEL + " recipes can open normally");
                    passed.add("real " + LABEL + " overlay displays, search accepts input and recipes open with blocking disabled");
                    mc.gui.setScreen(new InventoryScreen(mc.player)); toggle(true); changed = now; stage = 3;
                }
                case 3 -> {
                    if (now - changed < 1_000_000_000L) return;
                    require(ViewerBlocker.blocked(ID) && !visible(), "Startup roll blocks actual overlay");
                    require(REI ? call(overlay, "getFocused") == null : !(Boolean) call(overlay, "hasKeyboardFocus"), "Blocked overlay cannot hold keyboard focus");
                    String beforeSearch = searchText(); typeSearch(mc, 'x');
                    require(searchText().equals(beforeSearch), "Actual KeyboardHandler cannot type into blocked " + LABEL + " search");
                    if (!REI) {
                    var bookmarks = call(runtime, "getBookmarkOverlay");
                    require(!(Boolean) call(bookmarks, "isListDisplayed"), "Actual bookmark overlay is disabled");
                    var draggable = (java.util.stream.Stream<?>) bookmarks.getClass().getMethod("getDraggableIngredientUnderMouse", double.class, double.class).invoke(bookmarks, 20.0, 100.0);
                    try (draggable) { require(draggable.findAny().isEmpty(), "Blocked bookmarks cannot expose draggable ingredients"); }
                    try { require(!(Boolean) call(bookmarks, "hasBookmarkDrag"), "Actual bookmark drag is disabled"); }
                    catch (NoSuchMethodException olderApi) { /* Older JEI has no active-bookmark-drag query. */ }
                    var hovered = (java.util.stream.Stream<?>) overlay.getClass().getMethod("getIngredientUnderMouse", double.class, double.class).invoke(overlay, 800.0, 100.0);
                    try (hovered) { require(hovered.findAny().isEmpty(), "Blocked overlay cannot expose hovered ingredients"); }
                    showRecipes(); require(mc.gui.screen() instanceof InventoryScreen, "Blocked JEI recipe screen cannot replace the inventory");
                    var handlerType = Class.forName("mezz.jei.gui.input.ClientInputHandler"); var constructor = handlerType.getConstructors()[0];
                    Object[] constructorArguments = new Object[constructor.getParameterCount()];
                    for (int i = 0; i < constructorArguments.length; i++) if (List.class.isAssignableFrom(constructor.getParameterTypes()[i])) constructorArguments[i] = List.of();
                    var handler = constructor.newInstance(constructorArguments);
                    for (var method : handlerType.getDeclaredMethods()) if (method.getReturnType() == boolean.class && method.getName().startsWith("on")) {
                        Object[] args = new Object[method.getParameterCount()]; for (int i = 0; i < args.length; i++) if (method.getParameterTypes()[i] == double.class) args[i] = 0.0;
                        require(Boolean.FALSE.equals(method.invoke(handler, args)), "Blocked input callback does not consume or process input: " + method.getName());
                    }
                    } else {
                        require(!showRecipes() && mc.gui.screen() instanceof InventoryScreen, "Blocked REI view API refuses to open before constructing recipe screens");
                        for (var method : overlay.getClass().getMethods()) if (Set.of("mouseClicked", "mouseReleased", "mouseDragged", "mouseScrolled", "keyPressed", "keyReleased", "charTyped").contains(method.getName()) && method.getReturnType() == boolean.class) {
                            Object[] args = new Object[method.getParameterCount()]; for (int i = 0; i < args.length; i++) { if (method.getParameterTypes()[i] == double.class) args[i] = 0.0; else if (method.getParameterTypes()[i] == boolean.class) args[i] = false; }
                            require(Boolean.FALSE.equals(method.invoke(overlay, args)), "Blocked REI input callback: " + method.getName());
                        }
                    }
                    passed.add("startup roll hides real " + LABEL + " overlay, clears focus, blocks search and recipe screens and bypasses native input handlers"); shot(mc, "blocked");
                    toggle(false); changed = now; stage = 4;
                }
                case 4 -> {
                    if (now - changed < 1_000_000_000L) return;
                    require(visible(), "Disabling blocking restores actual overlay"); showRecipes();
                    require(recipeScreen(mc), "Recipe screen reopens after disabling blocking");
                    passed.add("Config disable restores " + LABEL + " overlay and recipe screen without restarting");
                    net.dimaskama.mcef.api.MCEFApi.initialize();
                    finish(mc, true, "");
                }
            }
        } catch (Throwable failure) { Sum1reM0d.LOGGER.error("VIEWER_SMOKE failed", failure); finish(mc, false, failure.toString()); }
    }
    private static Object call(Object target, String method) throws Exception { return target.getClass().getMethod(method).invoke(target); }
    private static boolean visible() throws Exception { return (Boolean) call(REI ? runtime : overlay, REI ? "isOverlayVisible" : "isListDisplayed"); }
    private static boolean recipeScreen(Minecraft mc) { return mc.gui.screen().getClass().getName().startsWith(REI ? "me.shedaniel.rei." : "mezz.jei."); }
    private static String searchText() throws Exception { return (String) call(filter, REI ? "getText" : "getFilterText"); }
    private static void setSearch(String text) throws Exception { filter.getClass().getMethod(REI ? "setText" : "setFilterText", String.class).invoke(filter, text); }
    private static void typeSearch(Minecraft mc, char character) throws Exception {
        var screen = mc.gui.screen();
        double x = (screen.width - 80) * (double) mc.getWindow().getScreenWidth() / screen.width;
        double y = (screen.height - 16) * (double) mc.getWindow().getScreenHeight() / screen.height;
        if (REI) { var bounds = call(filter, "getBounds"); x = ((Number) call(bounds, "getCenterX")).doubleValue() * mc.getWindow().getScreenWidth() / screen.width; y = ((Number) call(bounds, "getCenterY")).doubleValue() * mc.getWindow().getScreenHeight() / screen.height; }
        mc.mouseHandler.onMove(mc.getWindow().handle(), x, y, 0, 0);
        var button = new net.minecraft.client.input.MouseButtonInfo(org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT, 0);
        mc.mouseHandler.onButton(mc.getWindow().handle(), button, 1); mc.mouseHandler.onButton(mc.getWindow().handle(), button, 0);
        mc.keyboardHandler.charTyped(mc.getWindow().handle(), new net.minecraft.client.input.CharacterEvent(character));
    }
    private static boolean showRecipes() throws Exception {
        if (REI) { var builder = Class.forName("me.shedaniel.rei.api.client.view.ViewSearchBuilder").getMethod("builder").invoke(null); call(builder, "addAllCategories"); return (Boolean) call(builder, "open"); }
        else { var type = Class.forName("mezz.jei.api.constants.RecipeTypes").getField("CRAFTING").get(null); recipes.getClass().getMethod("showTypes", List.class).invoke(recipes, List.of(type)); return true; }
    }
    private static void toggle(boolean enabled) throws Exception {
        var store = PersonalConfig.store(); var draft = store.snapshot(); var feature = draft.getAsJsonObject("features").getAsJsonObject("recipe_viewers"); feature.addProperty("enabled", enabled); feature.addProperty("target_mods", ID); store.update(draft, store.revision());
        if (enabled) require(PersonalConfig.viewersBlocked(), "Viewer verification requires startup chance 100 in its temporary personal config");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void shot(Minecraft mc, String name) { Screenshot.grab(mc.gameDirectory, "sumire-viewer-" + (REI ? "rei" : "jei") + "-" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> Sum1reM0d.LOGGER.info("VIEWER_SMOKE: {}", message.getString())); }
    private static void finish(Minecraft mc, boolean success, String failure) {
        stage = -1;
        try {
            if (filter != null && originalFilter != null) setSearch(originalFilter);
            if (personal != null) PersonalConfig.store().update(personal, PersonalConfig.store().revision());
            if (rules != null) ClientRuntime.acceptRules(new ConfigStatePayload(ClientRules.revision, ClientRules.editable, rules.toString(), "", 0, false));
        } catch (Exception error) { success = false; failure += "; restore: " + error; }
        var result = new JsonObject(); result.addProperty("success", success); result.addProperty("failure", failure); result.add("passed", new Gson().toJsonTree(passed));
        try { Files.writeString(mc.gameDirectory.toPath().resolve("sum1rem0d-viewer-verification.json"), new GsonBuilder().setPrettyPrinting().create().toJson(result)); } catch (Exception error) { Sum1reM0d.LOGGER.error("Cannot save viewer report", error); }
        mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE); mc.stop();
    }
}
