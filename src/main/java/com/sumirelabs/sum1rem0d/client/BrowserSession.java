package com.sumirelabs.sum1rem0d.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.google.gson.JsonParser;
import net.dimaskama.mcef.api.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.input.*;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import org.joml.Matrix3x2f;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** One bounded, asynchronously initialized browser. All GPU/UI work stays on the client thread. */
public final class BrowserSession implements AutoCloseable {
    private MCEFBrowser browser;
    private volatile boolean closed;
    private String error;
    private final String initialUrl;
    private int x, y, width, height, scale = 1, pixelWidth, pixelHeight;
    private boolean focused;
    private double pointerX = Double.NaN, pointerY;
    private long lastVolumeUpdate;
    private int lastVolume = -1;
    private volatile PlaybackState playback = PlaybackState.UNKNOWN;
    private volatile long lastPlayingSampleAt;
    private CompletableFuture<String> playbackProbe;
    private long lastPlaybackProbe;
    public record PlaybackState(boolean hasMedia, boolean playing, boolean paused, boolean ended, int readyState) {
        static final PlaybackState UNKNOWN = new PlaybackState(false, false, false, false, 0);
    }
    public BrowserSession(String url) {
        initialUrl = url;
        MCEFApi.getInstanceFuture().whenComplete((api, failure) -> Minecraft.getInstance().execute(() -> {
            if (closed) return;
            if (failure != null) { error = "ブラウザの初期化に失敗しました"; Sum1reM0d.LOGGER.error("MCEF initialization failed", failure); return; }
            try { browser = api.createBrowser(url, false); browser.resize(Math.max(1, width * scale), Math.max(1, height * scale)); browser.setFocus(focused); }
            catch (RuntimeException | LinkageError exception) { error = "ブラウザを作成できません"; Sum1reM0d.LOGGER.error("Cannot create MCEF browser", exception); }
        }));
    }
    public void draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int volume) {
        this.x = x; this.y = y; this.width = Math.max(1, width); this.height = Math.max(1, height);
        scale = Math.max(1, (int) Minecraft.getInstance().getWindow().getGuiScale());
        if (browser != null && (pixelWidth != width * scale || pixelHeight != height * scale)) {
            pixelWidth = width * scale; pixelHeight = height * scale; browser.resize(pixelWidth, pixelHeight);
        }
        graphics.fill(x, y, x + width, y + height, 0xFF15171B);
        if (browser == null) {
            var font = Minecraft.getInstance().font;
            String progress = error != null ? error : "ブラウザを準備中… " + (int) MCEFApi.initialize().getPercentage() + "%";
            graphics.textWithWordWrap(font, net.minecraft.network.chat.Component.literal(progress), x + 5, y + 8, Math.max(1, width - 10), -1);
            return;
        }
        var view = browser.getTextureView();
        if (view != null) graphics.blit(view, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR), x, y, x + width, y + height, 0, 1, 0, 1);
        if (focused && contains(pointerX, pointerY)) graphics.requestCursor(browser.getCursorType());
        updateVolume(volume);
    }
    public void updateVolume(int volume) {
        if (closed || browser == null) return;
        int bounded = Math.max(0, Math.min(100, volume));
        long now = System.nanoTime();
        if (bounded != lastVolume || now - lastVolumeUpdate > 1_000_000_000L) {
            lastVolumeUpdate = now;
            lastVolume = bounded;
            browser.getCefBrowser().executeJavaScript("document.querySelectorAll('video,audio').forEach(v=>v.volume=" + bounded / 100.0 + ");", browser.getCefBrowser().getURL(), 0);
        }
    }
    public void pauseMedia() {
        if (!closed && browser != null) browser.getCefBrowser().executeJavaScript("document.querySelectorAll('video,audio').forEach(v=>v.pause());", browser.getCefBrowser().getURL(), 0);
    }
    public void playMedia() {
        if (!closed && browser != null) browser.getCefBrowser().executeJavaScript("document.querySelectorAll('video,audio').forEach(v=>{if(!v.ended)v.play().catch(()=>{})});", browser.getCefBrowser().getURL(), 0);
    }
    /** Polls real HTMLMediaElement state asynchronously; a browser/session alone never means playback. */
    public void pollPlayback() {
        if (closed || browser == null) { playback = PlaybackState.UNKNOWN; lastPlayingSampleAt = 0; return; }
        long now = System.nanoTime();
        if (playbackProbe != null && !playbackProbe.isDone() || now - lastPlaybackProbe < 500_000_000L) return;
        lastPlaybackProbe = now;
        MCEFBrowser owner = browser;
        org.cef.browser.CefBrowser cef = owner.getCefBrowser();
        String url = cef.getURL();
        var parameters = new com.google.gson.JsonObject();
        parameters.addProperty("expression", "(()=>{const media=[...document.querySelectorAll('video,audio')];const active=media.filter(v=>!v.paused&&!v.ended&&v.readyState>=2);return {hasMedia:media.length>0,playing:active.length>0,paused:media.some(v=>v.paused&&!v.ended),ended:media.length>0&&media.every(v=>v.ended),readyState:Math.max(0,...media.map(v=>v.readyState))}})()");
        parameters.addProperty("returnByValue", true);
        CompletableFuture<String> request;
        try {
            request = cef.getDevToolsClient().executeDevToolsMethod("Runtime.evaluate", parameters.toString()).orTimeout(3, TimeUnit.SECONDS);
        } catch (RuntimeException failure) {
            playback = PlaybackState.UNKNOWN;
            return;
        }
        playbackProbe = request;
        request.whenComplete((response, failure) -> Minecraft.getInstance().execute(() -> {
            if (browser != owner || closed) return;
            if (playbackProbe == request) playbackProbe = null;
            if (failure != null) { playback = PlaybackState.UNKNOWN; return; }
            if (!cef.getURL().equals(url)) { playback = PlaybackState.UNKNOWN; lastPlayingSampleAt = 0; return; }
            try {
                playback = parsePlaybackResponse(response);
                lastPlayingSampleAt = playback.playing() ? System.nanoTime() : 0;
            } catch (RuntimeException malformed) { playback = PlaybackState.UNKNOWN; }
        }));
    }
    static PlaybackState parsePlaybackResponse(String response) {
        var root = JsonParser.parseString(response).getAsJsonObject();
        if (root.has("exceptionDetails") || !root.has("result") || !root.getAsJsonObject("result").has("value"))
            return PlaybackState.UNKNOWN;
        var value = root.getAsJsonObject("result").getAsJsonObject("value");
        if (!value.has("hasMedia") || !value.has("playing") || !value.has("paused")
                || !value.has("ended") || !value.has("readyState")) return PlaybackState.UNKNOWN;
        return new PlaybackState(value.get("hasMedia").getAsBoolean(), value.get("playing").getAsBoolean(),
                value.get("paused").getAsBoolean(), value.get("ended").getAsBoolean(), value.get("readyState").getAsInt());
    }
    public PlaybackState playback() { return playback; }
    int configuredVolume() { return lastVolume; }
    public boolean playing() {
        PlaybackState current = playback;
        if (current.playing()) return true;
        // A transient DevTools timeout while focus/screen ownership changes must not
        // erase a recently confirmed playing state before the next media poll arrives.
        long sampledAt = lastPlayingSampleAt;
        return sampledAt != 0 && !current.paused() && !current.ended()
                && System.nanoTime() - sampledAt <= TimeUnit.SECONDS.toNanos(2);
    }
    public boolean contains(double mouseX, double mouseY) { return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height; }
    private MouseButtonEvent local(MouseButtonEvent event) { return new MouseButtonEvent((event.x() - x) * scale, (event.y() - y) * scale, event.buttonInfo()); }
    public boolean click(MouseButtonEvent event, boolean doubled) {
        pointerX = event.x(); pointerY = event.y();
        boolean inside = contains(event.x(), event.y()); focus(inside);
        if (inside && browser != null) browser.onMouseClicked(local(event), doubled);
        return inside;
    }
    public void release(MouseButtonEvent event) { if (browser != null && focused) browser.onMouseReleased(local(event)); }
    public void move(double mouseX, double mouseY) { pointerX = mouseX; pointerY = mouseY; if (browser != null) browser.onMouseMoved((int) ((mouseX - x) * scale), (int) ((mouseY - y) * scale)); }
    public boolean scroll(double mouseX, double mouseY, double amount) { if (!contains(mouseX, mouseY)) return false; if (browser != null) browser.onMouseScrolled((int) ((mouseX - x) * scale), (int) ((mouseY - y) * scale), amount); return true; }
    public boolean key(KeyEvent event, boolean released) { if (!focused) return false; if (browser != null) { if (released) browser.onKeyReleased(event); else browser.onKeyPressed(event); } return true; }
    public boolean character(CharacterEvent event) { if (!focused) return false; if (browser != null) browser.onCharTyped(event); return true; }
    public void focus(boolean value) { focused = value; if (browser != null) browser.setFocus(value); Minecraft.getInstance().textInputManager().onTextInputFocusChange(this, value); }
    public boolean focused() { return focused; }
    public boolean ready() { return browser != null && browser.getTextureView() != null; }
    public boolean failed() { return error != null; }
    public void back() { if (browser != null) browser.getCefBrowser().goBack(); }
    public void forward() { if (browser != null) browser.getCefBrowser().goForward(); }
    public void reload() { if (browser != null) browser.getCefBrowser().reload(); }
    public String url() {
        if (browser == null) return initialUrl;
        String current = browser.getCefBrowser().getURL();
        // MCEF can expose a browser before its first navigation commits. Keep the
        // requested URL observable during that short loading window.
        return current == null || current.isBlank() ? initialUrl : current;
    }
    org.cef.browser.CefBrowser cef() { return browser == null ? null : browser.getCefBrowser(); }
    boolean closed() { return closed; }
    @Override public void close() {
        if (closed) return;
        closed = true; playback = PlaybackState.UNKNOWN; lastPlayingSampleAt = 0; focus(false);
        if (browser != null) {
            try { browser.getCefBrowser().executeJavaScript("document.querySelectorAll('video,audio').forEach(v=>{v.pause();v.muted=true});", browser.getCefBrowser().getURL(), 0); }
            finally { browser.close(); browser = null; }
        }
    }
}
