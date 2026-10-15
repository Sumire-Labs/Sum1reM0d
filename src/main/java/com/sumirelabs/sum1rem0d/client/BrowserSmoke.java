package com.sumirelabs.sum1rem0d.client;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import com.sun.net.httpserver.HttpServer;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.mixin.ContainerScreenAccess;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.input.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.EnchantmentMenu;

/** Explicit development flag; exercises native container overlays and browser input on loopback. */
public final class BrowserSmoke {
    private static int stage;
    private static int searchAttempts;
    private static long changedAt;
    private static JsonObject original;
    private static JsonObject originalRules;
    private static double backgroundTime;
    private static HttpServer fixture;
    private static BrowserSession first, second, ad;
    private static net.dimaskama.mcef.api.MCEFBrowser releasedFirst, releasedYoutube;
    private static CountingEnchantmentMenu enchantment;
    private static String pointerTarget;
    private static long pointerMovedAt;
    private static final java.util.Map<String, CompletableFuture<String>> evaluations = new java.util.HashMap<>();
    private static final java.util.List<String> passed = new java.util.ArrayList<>();
    private static final JsonObject observations = new JsonObject();
    private static final java.util.Set<String> screenshots = new java.util.HashSet<>();
    private BrowserSmoke() {}
    public static void tick(Minecraft mc) {
        if (!Boolean.getBoolean("sum1rem0d.browserSmoke") || stage == -1 || !mc.isGameLoadFinished()) return;
        long now = System.nanoTime();
        try {
            if (changedAt != 0 && now - changedAt > 120_000_000_000L) throw new AssertionError("Timeout at browser stage " + stage);
            if (changedAt != 0 && now - changedAt < 2_000_000_000L) return;
            switch (stage) {
                case 0 -> {
                    if (mc.gui.overlay() != null) return;
                    java.awt.EventQueue.invokeLater(() -> Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> {
                        try { var writer = new java.io.StringWriter(); error.printStackTrace(new java.io.PrintWriter(writer)); Files.writeString(mc.gameDirectory.toPath().resolve("sum1rem0d-browser-awt-errors.txt"), writer.toString(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); }
                        catch (Throwable ignored) { }
                        try { Sum1reM0d.LOGGER.error("BROWSER_SMOKE AWT failure on {}", thread.getName(), error); } catch (Throwable ignored) { }
                    }));
                    String address = System.getProperty("sum1rem0d.networkAddress", "127.0.0.1:25589");
                    ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(address), new ServerData("Browser verification", address, ServerData.Type.OTHER), false, null);
                    advance(now);
                }
                case 1 -> {
                    if (mc.player == null || !ClientRules.received() || !ClientGameState.received()) return;
                    if (!mc.player.isAlive()) { screenshot(mc, "death-screen"); mc.player.respawn(); return; }
                    original = PersonalConfig.store().snapshot();
                    originalRules = ClientRules.snapshot();
                    fixture = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                    fixture.createContext("/", exchange -> {
                        byte[] bytes = ("<!doctype html><meta charset=utf-8><style>body{margin:0;background:#203346;color:white;font:24px sans-serif}input,button{display:block;width:90%;height:60px;margin:8px;font:24px sans-serif}main{height:3000px}</style><input id=entry placeholder='type here'><button id=button onclick='window.clicks=(window.clicks||0)+1'>Click test</button><audio id=volumeprobe></audio><main>MCEF container input test</main>").getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8"); exchange.sendResponseHeaders(200, bytes.length); try (var out = exchange.getResponseBody()) { out.write(bytes); }
                    }); fixture.start();
                    var draft = PersonalConfig.store().snapshot(); var yt = draft.getAsJsonObject("features").getAsJsonObject("youtube");
                    yt.addProperty("url", "http://127.0.0.1:" + fixture.getAddress().getPort() + "/"); yt.addProperty("stop_on_close", true); yt.addProperty("random_video", false);
                    PersonalConfig.store().update(draft, PersonalConfig.store().revision()); BrowserOverlays.closeAll();
                    mc.gui.setScreen(new InventoryScreen(mc.player)); advance(now);
                }
                case 2 -> {
                    first = BrowserOverlays.youtubeSession(); if (first == null || !first.ready()) return;
                    if (!probe(first, "Boolean(document.getElementById('entry'))").getAsBoolean()) { evaluations.clear(); return; }
                    var rect = probe(first, "(()=>{const r=document.getElementById('entry').getBoundingClientRect();return {x:r.x+20,y:r.y+20}})()").getAsJsonObject();
                    screenshot(mc, "inventory-browser");
                    click(mc, first, rect.get("x").getAsDouble(), rect.get("y").getAsDouble());
                    BrowserOverlays.character(new CharacterEvent('A')); BrowserOverlays.character(new CharacterEvent(0x3042)); advance(now);
                }
                case 3 -> {
                    String typed = probe(first, "document.getElementById('entry').value").getAsString(); observations.addProperty("expectedTextReceived", typed.contains("Aあ"));
                    require(typed.contains("Aあ"), "Native browser receives Latin and Japanese characters");
                    var rect = probe(first, "(()=>{const r=document.getElementById('button').getBoundingClientRect();return {x:r.x+20,y:r.y+20}})()").getAsJsonObject();
                    pass("inventory browser receives native click and Latin/Japanese text input");
                    click(mc, first, rect.get("x").getAsDouble(), rect.get("y").getAsDouble()); advance(now);
                }
                case 4 -> {
                    require(probe(first, "window.clicks||0").getAsInt() == 1, "Browser button receives exactly one click");
                    var access = (ContainerScreenAccess) mc.gui.screen();
                    BrowserOverlays.scroll(mc.gui.screen(), access.sum1rem0d$leftPos() - 25, mc.gui.screen().height / 2.0, -6); advance(now);
                }
                case 5 -> {
                    require(probe(first, "window.scrollY").getAsDouble() > 0, "Native scroll reaches browser"); pass("inventory browser button and scroll work");
                    releasedFirst = (net.dimaskama.mcef.api.MCEFBrowser) first.cef();
                    mc.gui.setScreen(null); require(first.closed() && BrowserOverlays.youtubeSession() == null, "Closing inventory releases browser");
                    pass("closing inventory releases browser and focus"); mc.gui.setScreen(new InventoryScreen(mc.player)); advance(now);
                }
                case 6 -> {
                    require(releasedFirst.getTexture() == null && releasedFirst.getTextureView() == null, "Closed browser does not recreate GPU resources from queued paints");
                    pass("closed browser keeps GPU texture and view released");
                    second = BrowserOverlays.youtubeSession(); if (second == null || !second.ready()) return;
                    require(second != first && second.url().equals(first.url()), "Reopening creates a new browser on the same page");
                    pass("reopening inventory restores the same URL");
                    var draft = PersonalConfig.store().snapshot(); var yt = draft.getAsJsonObject("features").getAsJsonObject("youtube"); yt.addProperty("width", 110); yt.addProperty("height", 100); yt.addProperty("volume", 25); yt.addProperty("stop_on_close", false); PersonalConfig.store().update(draft, PersonalConfig.store().revision());
                    stage = 20; changedAt = now; evaluations.clear();
                }
                case 7 -> {
                    ad = BrowserOverlays.adSession(); if (ad == null || !ad.ready()) return;
                    screenshot(mc, "enchantment-ad");
                    require(second.closed(), "Changing screens closes inventory browser");
                    require(!probe(ad, "document.body.innerText").getAsString().isBlank(), "Advertisement HTML loaded");
                    pass("enchantment screen renders MCEF advertisement and closes previous browser");
                    nativeClick(mc, (mc.gui.screen().width - 176) / 2.0 + 80, (mc.gui.screen().height - 166) / 2.0 + 22);
                    require(enchantment.clicks == 0, "Advertisement consumes click before underlying enchantment action");
                    pass("advertisement consumes native click before underlying enchantment action");
                    BrowserOverlays.key(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0), false);
                    require(BrowserOverlays.adSession() == ad && !ad.closed(), "Advertisement cannot be skipped before five seconds"); advance(now);
                }
                case 8 -> {
                    if (now - changedAt < 4_000_000_000L) return;
                    BrowserOverlays.key(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0), false);
                    require(ad.closed() && BrowserOverlays.adSession() == null, "Advertisement can be skipped after five seconds");
                    pass("advertisement skip is blocked initially and releases browser after configured wait");
                    mc.gui.setScreen(null);
                    var rules = ClientRules.snapshot(); var options = rules.getAsJsonObject("features").getAsJsonObject("advertisements"); options.addProperty("urls", localUrl() + "ad?campaign=sumire&variant=2"); options.addProperty("interval", 5); options.addProperty("skip_after", 0); applyRules(rules);
                    var draft = PersonalConfig.store().snapshot(); draft.getAsJsonObject("features").getAsJsonObject("advertisements").addProperty("volume", 35); PersonalConfig.store().update(draft, PersonalConfig.store().revision());
                    mc.gui.setScreen(new EnchantmentScreen(enchantment, mc.player.getInventory(), Component.literal("Repeated advertisement verification"))); stage = 23; changedAt = now; evaluations.clear();
                }
                case 9 -> {
                    if (now - changedAt < 8_000_000_000L) return;
                    var youtube = BrowserOverlays.youtubeSession(); if (youtube == null || !youtube.ready()) return;
                    JsonElement result = probe(youtube, "({title:document.title,video:!!document.querySelector('video'),error:document.querySelector('.ytp-error-content-wrap-reason')?.innerText||'',time:document.querySelector('video')?.currentTime||0,paused:document.querySelector('video')?.paused??true,width:document.querySelector('video')?.videoWidth||0,frames:document.querySelector('video')?.getVideoPlaybackQuality().totalVideoFrames||0,volume:document.querySelector('video')?.volume??-1,state:document.readyState})");
                    observations.add("youtube", result); var object = result.getAsJsonObject();
                    if ((object.get("time").getAsDouble() < 1 || object.get("frames").getAsInt() < 2) && now - changedAt < 75_000_000_000L) { evaluations.clear(); return; }
                    require(object.get("video").getAsBoolean() && object.get("time").getAsDouble() >= 1 && object.get("frames").getAsInt() >= 2 && object.get("width").getAsInt() > 0, "YouTube video must actually decode and play: " + result);
                    require(Math.abs(object.get("volume").getAsDouble() - .25) < .001, "Configured volume applies to the actual YouTube video");
                    var button = playerButton(mc, youtube, "Pause");
                    observations.add("pauseButton", button);
                    screenshot(mc, "youtube"); click(mc, youtube, button.get("x").getAsDouble(), button.get("y").getAsDouble());
                    pass("YouTube video loads and playback time advances"); advance(now);
                }
                case 10 -> {
                    var youtube = BrowserOverlays.youtubeSession(); require(probe(youtube, "document.querySelector('video').paused").getAsBoolean(), "YouTube pause works");
                    screenshot(mc, "youtube-paused");
                    var button = playerButton(mc, youtube, "Play");
                    observations.add("resumeButton", button); click(mc, youtube, button.get("x").getAsDouble(), button.get("y").getAsDouble());
                    pass("YouTube native pause button works"); advance(now);
                }
                case 11 -> {
                    var youtube = BrowserOverlays.youtubeSession();
                    var resumed = probe(youtube, "({paused:document.querySelector('video').paused,time:document.querySelector('video').currentTime,error:document.querySelector('.ytp-error-content-wrap-reason')?.innerText||''})").getAsJsonObject();
                    observations.add("youtubeResume", resumed);
                    if (resumed.get("paused").getAsBoolean() && now - changedAt < 10_000_000_000L) { evaluations.clear(); return; }
                    require(!resumed.get("paused").getAsBoolean(), "YouTube native play button resumes playback: " + resumed);
                    var search = probe(youtube, "({inputs:[...document.querySelectorAll('input')].map(e=>({name:e.name,id:e.id,placeholder:e.placeholder,visible:!!e.getClientRects().length})),buttons:[...document.querySelectorAll('button[aria-label]')].map(e=>e.getAttribute('aria-label')).filter(s=>/search/i.test(s))})");
                    observations.add("searchControls", search);
                    pass("YouTube native play button resumes playback");
                    backgroundTime = resumed.get("time").getAsDouble();
                    var draft = PersonalConfig.store().snapshot(); draft.getAsJsonObject("features").getAsJsonObject("youtube").addProperty("stop_on_close", false); PersonalConfig.store().update(draft, PersonalConfig.store().revision());
                    first = youtube; mc.gui.setScreen(null); require(!youtube.closed() && BrowserOverlays.youtubeSession() == youtube, "Background setting retains the playing YouTube browser"); stage = 25; changedAt = now; evaluations.clear();
                }
                case 12 -> {
                    require(releasedYoutube.getTexture() == null && releasedYoutube.getTextureView() == null, "Closed YouTube browser releases GPU resources");
                    if (now - changedAt < 8_000_000_000L) return;
                    var youtube = BrowserOverlays.youtubeSession(); if (youtube == null || !youtube.ready()) return;
                    JsonElement target = probe(youtube, "(()=>{const e=[...document.querySelectorAll('button[aria-label=Search]')].find(e=>{const r=e.getBoundingClientRect();return r.width>0&&r.height>0});if(!e)return null;const r=e.getBoundingClientRect();return {x:r.x+r.width/2,y:r.y+r.height/2}})()");
                    if (target.isJsonNull()) { evaluations.clear(); return; }
                    var rect = target.getAsJsonObject(); click(mc, youtube, rect.get("x").getAsDouble(), rect.get("y").getAsDouble()); searchAttempts++; advance(now);
                }
                case 13 -> {
                    var youtube = BrowserOverlays.youtubeSession();
                    JsonElement target = probe(youtube, "(()=>{const e=document.querySelector('input[name=search_query]');if(!e)return null;const r=e.getBoundingClientRect();if(!r.width||!r.height)return null;return {x:r.x+20,y:r.y+r.height/2}})()");
                    if (target.isJsonNull()) {
                        evaluations.clear();
                        if (now - changedAt > 3_000_000_000L) {
                            require(searchAttempts < 3, "YouTube search button must reveal the input"); stage = 12; changedAt = now - 8_000_000_000L;
                        }
                        return;
                    }
                    var rect = target.getAsJsonObject(); click(mc, youtube, rect.get("x").getAsDouble(), rect.get("y").getAsDouble());
                    "Me at the zoo".codePoints().forEach(point -> BrowserOverlays.character(new CharacterEvent(point)));
                    BrowserOverlays.key(new KeyEvent(InputConstants.KEY_RETURN, 0, 0), false); BrowserOverlays.key(new KeyEvent(InputConstants.KEY_RETURN, 0, 0), true); advance(now);
                }
                case 14 -> {
                    var youtube = BrowserOverlays.youtubeSession();
                    var result = probe(youtube, "({path:location.pathname,query:new URL(location.href).searchParams.get('search_query'),results:!!document.querySelector('ytd-search,ytm-search'),text:document.body.innerText.toLowerCase().includes('me at the zoo')})").getAsJsonObject(); observations.add("youtubeSearch", result);
                    if ((!result.get("path").getAsString().equals("/results") || !result.get("text").getAsBoolean()) && now - changedAt < 30_000_000_000L) { evaluations.clear(); return; }
                    require(result.get("path").getAsString().equals("/results") && result.get("query").getAsString().equals("Me at the zoo") && result.get("text").getAsBoolean(), "Native YouTube search input and Enter produce results: " + result);
                    screenshot(mc, "youtube-search"); pass("YouTube native search input and Enter load search results"); finish(mc, true, "");
                }
                case 20 -> {
                    var viewport = probe(second, "({width:innerWidth,height:innerHeight,volume:document.getElementById('volumeprobe').volume})").getAsJsonObject(); observations.add("configuredViewport", viewport);
                    int scale = Math.max(1, (int) mc.getWindow().getGuiScale());
                    require(viewport.get("width").getAsInt() == 110 * scale && viewport.get("height").getAsInt() == 100 * scale && Math.abs(viewport.get("volume").getAsDouble() - .25) < .001, "Config changes actual CEF viewport and media volume");
                    pass("configured browser dimensions resize the native viewport and volume updates media elements");
                    var draft = PersonalConfig.store().snapshot(); draft.getAsJsonObject("features").getAsJsonObject("youtube").addProperty("volume", 0); PersonalConfig.store().update(draft, PersonalConfig.store().revision()); advanceTo(21, now);
                }
                case 21 -> {
                    require(probe(second, "document.getElementById('volumeprobe').volume").getAsDouble() == 0, "Volume zero mutes media elements");
                    mc.gui.setScreen(null); require(!second.closed() && BrowserOverlays.youtubeSession() == second, "Background mode retains local browser");
                    pass("muting and background session retention apply without recreating the browser");
                    var draft = PersonalConfig.store().snapshot(); draft.getAsJsonObject("features").getAsJsonObject("youtube").addProperty("volume", 65); PersonalConfig.store().update(draft, PersonalConfig.store().revision()); advanceTo(26, now);
                }
                case 26 -> {
                    double backgroundVolume = probe(second, "document.getElementById('volumeprobe').volume").getAsDouble();
                    require(mc.gui.screen() == null && Math.abs(backgroundVolume - .65) < .001, "Volume changes apply while inventory is closed: volume=" + backgroundVolume + ", screen=" + mc.gui.screen()); pass("background browser applies Config volume changes without reopening inventory");
                    mc.gui.setScreen(new InventoryScreen(mc.player)); require(BrowserOverlays.youtubeSession() == second, "Reopening reuses the retained browser");
                    var draft = PersonalConfig.store().snapshot(); var yt = draft.getAsJsonObject("features").getAsJsonObject("youtube"); yt.addProperty("stop_on_close", true); yt.addProperty("random_video", true); yt.addProperty("random_urls", localUrl() + "?selected=1&variant=2"); PersonalConfig.store().update(draft, PersonalConfig.store().revision()); mc.gui.setScreen(null); mc.gui.setScreen(new InventoryScreen(mc.player)); second = BrowserOverlays.youtubeSession(); advanceTo(22, now);
                }
                case 22 -> {
                    if (second == null || !second.ready()) return; if (!probe(second, "Boolean(document.getElementById('volumeprobe'))").getAsBoolean()) { evaluations.clear(); return; }
                    require(probe(second, "location.search").getAsString().equals("?selected=1&variant=2"), "Random URL selection preserves query parameters"); pass("configured random URL pool selects and loads the expected URL including query parameters");
                    var draft = PersonalConfig.store().snapshot(); var yt = draft.getAsJsonObject("features").getAsJsonObject("youtube"); yt.addProperty("random_video", false); for (String key : java.util.List.of("width", "height")) yt.add(key, original.getAsJsonObject("features").getAsJsonObject("youtube").get(key)); PersonalConfig.store().update(draft, PersonalConfig.store().revision());
                    enchantment = new CountingEnchantmentMenu(mc.player.getInventory()); mc.gui.setScreen(new EnchantmentScreen(enchantment, mc.player.getInventory(), Component.literal("Enchantment verification"))); advanceTo(7, now);
                }
                case 23 -> {
                    ad = BrowserOverlays.adSession(); if (ad == null || !ad.ready()) return; if (!probe(ad, "Boolean(document.getElementById('volumeprobe'))").getAsBoolean()) { evaluations.clear(); return; }
                    require(probe(ad, "location.search").getAsString().equals("?campaign=sumire&variant=2") && Math.abs(probe(ad, "document.getElementById('volumeprobe').volume").getAsDouble() - .35) < .001, "External advertisement URL and personal volume apply"); pass("external advertisement URL loads with query parameters and configured media volume");
                    BrowserOverlays.key(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0), false); require(ad.closed(), "Zero skip delay permits immediate close"); advanceTo(24, now);
                }
                case 24 -> {
                    var repeated = BrowserOverlays.adSession(); if (repeated == null || !repeated.ready()) return; if (!probe(repeated, "Boolean(document.getElementById('volumeprobe'))").getAsBoolean()) { evaluations.clear(); return; }
                    require(repeated != ad && !repeated.closed(), "Advertisement interval creates a new browser after skipping");
                    var rules = ClientRules.snapshot(); rules.getAsJsonObject("features").getAsJsonObject("advertisements").addProperty("enabled", false); applyRules(rules); BrowserOverlays.tick(); require(repeated.closed() && BrowserOverlays.adSession() == null, "Disabling advertisements closes the active popup"); pass("advertisements repeat at the configured interval and disabling releases the popup");
                    mc.gui.setScreen(null); applyRules(originalRules);
                    var draft = PersonalConfig.store().snapshot(); var yt = draft.getAsJsonObject("features").getAsJsonObject("youtube"); yt.addProperty("url", "https://www.youtube.com/watch?v=jNQXAC9IVRw"); yt.addProperty("volume", 25); PersonalConfig.store().update(draft, PersonalConfig.store().revision()); mc.gui.setScreen(new InventoryScreen(mc.player)); advanceTo(9, now);
                }
                case 25 -> {
                    var playback = probe(first, "({time:document.querySelector('video').currentTime,paused:document.querySelector('video').paused})").getAsJsonObject();
                    if (playback.get("time").getAsDouble() <= backgroundTime + .5 && now - changedAt < 15_000_000_000L) { evaluations.clear(); return; }
                    require(!playback.get("paused").getAsBoolean() && playback.get("time").getAsDouble() > backgroundTime + .5, "Actual YouTube video keeps playing after inventory closes in background mode"); pass("background setting keeps actual YouTube playback advancing while inventory is closed");
                    mc.gui.setScreen(new InventoryScreen(mc.player)); require(BrowserOverlays.youtubeSession() == first, "Background YouTube reopens the same session");
                    var draft = PersonalConfig.store().snapshot(); draft.getAsJsonObject("features").getAsJsonObject("youtube").addProperty("stop_on_close", true); PersonalConfig.store().update(draft, PersonalConfig.store().revision()); releasedYoutube = (net.dimaskama.mcef.api.MCEFBrowser) first.cef(); mc.gui.setScreen(null); require(first.closed() && BrowserOverlays.youtubeSession() == null, "Closing inventory stops the actual YouTube browser"); pass("closing inventory releases playing YouTube browser"); mc.gui.setScreen(new InventoryScreen(mc.player)); advanceTo(12, now);
                }
            }
        } catch (ProbePending pendingProbe) { /* Resume after CEF replies; never block the render thread. */ }
        catch (Throwable failure) { screenshot(mc, "failure"); Sum1reM0d.LOGGER.error("BROWSER_SMOKE failed", failure); finish(mc, false, failure.toString()); }
    }
    private static JsonElement probe(BrowserSession session, String expression) {
        var pending = evaluations.get(expression);
        if (pending == null) {
            var params = new JsonObject(); params.addProperty("expression", expression); params.addProperty("returnByValue", true); params.addProperty("awaitPromise", true);
            evaluations.put(expression, session.cef().getDevToolsClient().executeDevToolsMethod("Runtime.evaluate", params.toString())); throw new ProbePending();
        }
        if (!pending.isDone()) throw new ProbePending();
        String text = pending.join();
        var result = JsonParser.parseString(text).getAsJsonObject();
        if (result.has("exceptionDetails")) throw new AssertionError("Browser JavaScript evaluation failed: " + result);
        return result.getAsJsonObject("result").get("value");
    }
    private static void click(Minecraft mc, BrowserSession browser, double px, double py) {
        var screen = mc.gui.screen(); var access = (ContainerScreenAccess) screen;
        int width = Math.max(40, Math.min(PersonalConfig.store().integer("youtube", "width"), access.sum1rem0d$leftPos() - 12));
        int height = Math.max(30, Math.min(PersonalConfig.store().integer("youtube", "height"), screen.height - 42));
        double scale = mc.getWindow().getGuiScale();
        double x = access.sum1rem0d$leftPos() - width - 8 + px / scale, y = Math.max(25, (screen.height - height) / 2) + py / scale;
        String target = browser.cef().getIdentifier() + ":" + px + ":" + py;
        if (!target.equals(pointerTarget)) { pointerTarget = target; pointerMovedAt = System.nanoTime(); BrowserOverlays.moved(x, y); throw new ProbePending(); }
        if (System.nanoTime() - pointerMovedAt < 300_000_000L) throw new ProbePending();
        nativeClick(mc, x, y);
        pointerTarget = null;
    }
    private static void nativeClick(Minecraft mc, double x, double y) {
        var screen = mc.gui.screen();
        double windowX = x * mc.getWindow().getScreenWidth() / screen.width, windowY = y * mc.getWindow().getScreenHeight() / screen.height;
        mc.mouseHandler.onMove(mc.getWindow().handle(), windowX, windowY, 0, 0);
        var button = new MouseButtonInfo(org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT, 0);
        mc.mouseHandler.onButton(mc.getWindow().handle(), button, 1); mc.mouseHandler.onButton(mc.getWindow().handle(), button, 0);
    }
    private static JsonObject playerButton(Minecraft mc, BrowserSession session, String action) {
        String expression = "(()=>{const es=[...document.querySelectorAll('.ytp-play-button,button[aria-label^=" + action + "]')];const e=es.find(e=>{const r=e.getBoundingClientRect();return r.width>0&&r.height>0});if(!e)return null;const r=e.getBoundingClientRect();return {x:r.x+r.width/2,y:r.y+r.height/2,width:r.width,height:r.height}})()";
        JsonElement button = probe(session, expression);
        if (!button.isJsonNull()) return button.getAsJsonObject();
        var video = probe(session, "(()=>{const r=document.querySelector('video').getBoundingClientRect();return {x:r.x+r.width/2,y:r.y+r.height/2}})()").getAsJsonObject();
        var screen = mc.gui.screen(); var access = (ContainerScreenAccess) screen;
        int width = Math.max(40, Math.min(PersonalConfig.store().integer("youtube", "width"), access.sum1rem0d$leftPos() - 12));
        int height = Math.max(30, Math.min(PersonalConfig.store().integer("youtube", "height"), screen.height - 42));
        double scale = mc.getWindow().getGuiScale();
        BrowserOverlays.moved(access.sum1rem0d$leftPos() - width - 8 + video.get("x").getAsDouble() / scale, Math.max(25, (screen.height - height) / 2) + video.get("y").getAsDouble() / scale);
        evaluations.remove(expression); throw new ProbePending();
    }
    private static void screenshot(Minecraft mc, String name) { if (!screenshots.add(name)) return; Screenshot.grab(mc.gameDirectory, "sumire-browser-" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> Sum1reM0d.LOGGER.info("BROWSER_SMOKE: {}", message.getString())); }
    private static void advance(long now) { stage++; changedAt = now; evaluations.clear(); }
    private static void advanceTo(int target, long now) { stage = target; changedAt = now; evaluations.clear(); }
    private static String localUrl() { return "http://127.0.0.1:" + fixture.getAddress().getPort() + "/"; }
    private static void applyRules(JsonObject rules) { ClientRuntime.acceptRules(new com.sumirelabs.sum1rem0d.network.ConfigStatePayload(ClientRules.revision, ClientRules.editable, rules.toString(), "", 0, false)); }
    private static void pass(String value) { if (passed.contains(value)) return; passed.add(value); Sum1reM0d.LOGGER.info("BROWSER_SMOKE: {}", value); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void finish(Minecraft mc, boolean success, String failure) {
        stage = -1; BrowserOverlays.closeAll(); if (fixture != null) fixture.stop(0);
        if (originalRules != null) applyRules(originalRules);
        if (original != null) try { PersonalConfig.store().update(original, PersonalConfig.store().revision()); } catch (Exception error) { success = false; failure += "; restore: " + error; }
        var result = new JsonObject(); result.addProperty("success", success); result.addProperty("failure", failure); result.add("passed", new Gson().toJsonTree(passed)); result.add("observations", observations);
        try { Files.writeString(mc.gameDirectory.toPath().resolve("sum1rem0d-browser-verification.json"), new GsonBuilder().setPrettyPrinting().create().toJson(result)); } catch (Exception error) { Sum1reM0d.LOGGER.error("Cannot save browser report", error); }
        Sum1reM0d.LOGGER.info("BROWSER_SMOKE: completed success={}", success); mc.disconnectFromWorld(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE); mc.stop();
    }
    private static final class ProbePending extends RuntimeException { @Override public synchronized Throwable fillInStackTrace() { return this; } }
    private static final class CountingEnchantmentMenu extends EnchantmentMenu {
        int clicks;
        CountingEnchantmentMenu(net.minecraft.world.entity.player.Inventory inventory) { super(40, inventory); }
        @Override public boolean clickMenuButton(net.minecraft.world.entity.player.Player player, int button) { clicks++; return false; }
    }
}
