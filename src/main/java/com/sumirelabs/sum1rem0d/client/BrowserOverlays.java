package com.sumirelabs.sum1rem0d.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sumirelabs.sum1rem0d.config.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.input.*;
import com.sumirelabs.sum1rem0d.mixin.ContainerScreenAccess;

public final class BrowserOverlays {
    private static Screen owner;
    private static BrowserSession youtube, ad, worldAd, deathMedia;
    private static String lastYoutubeUrl;
    private static String configuredYoutubeUrl;
    private static long nextAd, adOpened;
    private static int youtubeX, youtubeY, youtubeWidth, youtubeHeight, adX, adY, adWidth, adHeight;
    private static int hudX = 8, hudY = 26, hudWidth, hudHeight;
    private static boolean draggingHud,hudMoved;
    private static long pausedBarConfigRevision = Long.MIN_VALUE;
    private static boolean cachedPausedBar;
    private static int worldAdTicksRemaining;
    private static long deathPlayAttemptAt;
    private static double dragOffsetX, dragOffsetY;
    private BrowserOverlays() {}
    static BrowserSession youtubeSession() { return youtube; }
    static int[] hudBounds() { return Minecraft.getInstance().gui.screen()==null && hudVisible() ? new int[] { hudX, hudY, hudWidth, hudHeight } : null; }
    private static boolean pausedBar() {
        if (youtube == null) return false;
        var state = youtube.playback();
        var store = PersonalConfig.store();
        if (pausedBarConfigRevision != store.revision()) {
            var options = store.snapshot().getAsJsonObject("features").getAsJsonObject("youtube");
            cachedPausedBar = options.has("paused_bar") && options.get("paused_bar").getAsBoolean();
            pausedBarConfigRevision = store.revision();
        }
        return state.hasMedia() && state.paused() && !state.ended() && cachedPausedBar;
    }
    private static boolean hudVisible() { return youtube != null && PersonalConfig.store().bool("youtube","mini_player") && (youtube.playing() || pausedBar()); }
    static BrowserSession adSession() { return ad; }
    private static boolean inventory(Screen screen) { return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen; }
    private static boolean advertisementScreen(Screen screen) {
        return screen instanceof EnchantmentScreen && ClientRules.bool("advertisements", "enchanting")
                || screen instanceof AnvilScreen && ClientRules.bool("advertisements", "anvil");
    }
    static BrowserLayout layout(Screen screen) {
        int left = ((ContainerScreenAccess) screen).sum1rem0d$leftPos();
        if (screen instanceof com.sumirelabs.sum1rem0d.mixin.RecipeBookScreenAccess access && access.sum1rem0d$recipeBook().isVisible()) left -= 147;
        return BrowserLayout.fit(left, screen.height, PersonalConfig.store().integer("youtube", "width"),
                PersonalConfig.store().integer("youtube", "height"), PersonalConfig.store().integer("youtube", "left_gap"));
    }
    public static int[] exclusion() {
        var screen = Minecraft.getInstance().gui.screen();
        if (!inventory(screen) || !ClientRules.enabled("youtube")) return null;
        var bounds = layout(screen);
        return bounds.visible() ? bounds.exclusion() : null;
    }
    public static void initialize(Screen screen) {
        if (!inventory(screen) && !advertisementScreen(screen)) return;
        if (owner != screen) { changed(screen); owner = screen; nextAd = 0; }
        if (inventory(screen) && ClientRules.enabled("youtube")) {
            if (youtube == null) youtube = new BrowserSession(youtubeUrl());
        }
    }
    private static String youtubeUrl() {
        var config = PersonalConfig.store(); var urls = config.list("youtube", "random_urls");
        if (!Objects.equals(configuredYoutubeUrl, config.text("youtube", "url"))) {
            configuredYoutubeUrl = config.text("youtube", "url"); lastYoutubeUrl = null;
        }
        if (config.bool("youtube", "random_video") && !urls.isEmpty()) return safe(urls.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(urls.size())), "https://www.youtube.com/");
        return safe(lastYoutubeUrl == null ? config.text("youtube", "url") : lastYoutubeUrl, "https://www.youtube.com/");
    }
    private static String safe(String url, String fallback) {
        try { var uri = java.net.URI.create(url); return ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) && uri.getHost() != null ? url : fallback; }
        catch (RuntimeException exception) { return fallback; }
    }
    public static void changed(Screen next) {
        if (owner == next) return;
        if (youtube != null) {
            lastYoutubeUrl = youtube.url();
            if(!PersonalConfig.store().bool("youtube","stop_on_close")&&inventory(owner))youtube.keepPlayingOnScreenChange();
            youtube.focus(false);
            if (PersonalConfig.store().bool("youtube", "stop_on_close") || Minecraft.getInstance().level == null) { youtube.close(); youtube = null; }
        }
        if (ad != null) { ad.close(); ad = null; }
        owner = null;
    }
    public static void tick() {
        var mc = Minecraft.getInstance();
        if (owner != null && owner != mc.gui.screen()) changed(mc.gui.screen());
        if (youtube != null && (!ClientRules.enabled("youtube") || mc.level == null)) { youtube.close(); youtube = null; }
        if (ad != null && (!ClientRules.enabled("advertisements") || ShopClient.adPremium() || mc.level == null || !advertisementScreen(mc.gui.screen()))) closeAd();
        if (youtube != null) { youtube.updateVolume(PersonalConfig.store().integer("youtube", "volume")); youtube.pollPlayback(); }
        if (ad != null) ad.updateVolume(PersonalConfig.store().integer("advertisements", "volume"));
        if (worldAd != null) {
            if (mc.level == null || !ClientRules.enabled("structure_ads") || !ClientRules.enabled("advertisements") || ShopClient.adPremium()) closeWorldAd();
            else if (--worldAdTicksRemaining <= 0) closeWorldAd();
            else { worldAd.updateVolume(PersonalConfig.store().integer("advertisements", "volume")); worldAd.pollPlayback(); }
        }
        if (deathMedia != null) {
            if (mc.level == null || !ClientRules.enabled("pvp_death_media") || deathMedia.failed()) closeDeathMedia();
            else {
                deathMedia.updateVolume(PersonalConfig.store().integer("pvp_death_media", "volume"));
                deathMedia.pollPlayback();
                long now = System.nanoTime();
                if (!deathMedia.playing() && !deathMedia.playback().ended() && now - deathPlayAttemptAt >= 1_000_000_000L) {
                    deathPlayAttemptAt = now; deathMedia.playMedia();
                }
                if (deathMedia.playback().ended()) closeDeathMedia();
            }
        }
        String url = PersonalConfig.store().text("youtube", "url");
        if (configuredYoutubeUrl != null && !configuredYoutubeUrl.equals(url)) {
            if (youtube != null) youtube.close(); youtube = null; lastYoutubeUrl = null; configuredYoutubeUrl = url;
            if (owner != null && inventory(owner) && owner == mc.gui.screen() && ClientRules.enabled("youtube")) youtube = new BrowserSession(youtubeUrl());
        }
        if (youtube == null && owner != null && inventory(owner) && owner == mc.gui.screen() && ClientRules.enabled("youtube")) initialize(owner);
    }
    public static void render(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (owner != screen) initialize(screen);
        if (owner != screen) return;
        var font = Minecraft.getInstance().font;
        if (inventory(screen) && youtube != null) {
            if (!ClientRules.enabled("youtube")) { youtube.close(); youtube = null; }
            else {
                var bounds = layout(screen);
                youtubeWidth = bounds.width(); youtubeHeight = bounds.height(); youtubeX = bounds.x(); youtubeY = bounds.y();
                if (!bounds.visible()) { youtube.focus(false); return; }
                graphics.fill(youtubeX - 2, youtubeY - 18, youtubeX + youtubeWidth + 2, youtubeY + youtubeHeight + 2, 0xEE101114);
                graphics.text(font, "←  →  ↻  YouTube", youtubeX + 3, youtubeY - 13, -1);
                youtube.draw(graphics, youtubeX, youtubeY, youtubeWidth, youtubeHeight, PersonalConfig.store().integer("youtube", "volume"));
            }
        }
        long now = System.nanoTime();
        if (advertisementScreen(screen) && ClientRules.enabled("advertisements") && !ShopClient.adPremium() && now >= nextAd && ad == null) {
            var urls = ClientRules.list("advertisements", "urls");
            String url = urls.isEmpty() ? "https://www.youtube.com/" : urls.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(urls.size()));
            ad = new BrowserSession(url.equals("sum1rem0d:default_ad") ? defaultAd() : safe(url, defaultAd()));
            adOpened = now; nextAd = now + (long) ClientRules.number("advertisements", "interval") * 1_000_000_000L;
        }
        if (ad != null) {
            if (!ClientRules.enabled("advertisements") || ShopClient.adPremium()) { closeAd(); return; }
            adWidth = Math.min(500, screen.width - 24); adHeight = Math.min(300, screen.height - 70);
            adX = (screen.width - adWidth) / 2; adY = (screen.height - adHeight) / 2 + 8;
            graphics.fill(0, 0, screen.width, screen.height, 0xB8000000);
            graphics.fill(adX - 3, adY - 23, adX + adWidth + 3, adY + adHeight + 3, 0xFF202126);
            graphics.text(font, "広告", adX + 5, adY - 16, 0xFFCCCCCC);
            int wait = Math.max(0, ClientRules.integer("advertisements", "skip_after") - (int) ((now - adOpened) / 1_000_000_000L));
            graphics.text(font, wait > 0 ? wait + "秒後に閉じる" : "閉じる ×", adX + adWidth - 90, adY - 16, wait > 0 ? 0xFF909090 : -1);
            ad.draw(graphics, adX, adY, adWidth, adHeight, PersonalConfig.store().integer("advertisements", "volume"));
        }
    }
    /** Draws the persistent player while the inventory is closed. */
    public static void renderHud(GuiGraphicsExtractor graphics) {
        var mc = Minecraft.getInstance();
        if (!hudVisible() || mc.level == null || mc.gui.hud.isHidden() || !ClientRules.enabled("youtube") || mc.gui.screen()!=null) return;
        boolean paused = pausedBar();
        hudWidth = paused ? Math.max(140, Math.min(220, graphics.guiWidth() - 16)) : Math.max(1, Math.min(PersonalConfig.store().integer("youtube", "mini_width"), graphics.guiWidth() - 16));
        hudHeight = paused ? 20 : Math.max(1, Math.min(PersonalConfig.store().integer("youtube", "mini_height"), graphics.guiHeight() - 44));
        if(!hudMoved){int margin=PersonalConfig.store().integer("youtube","mini_margin");hudX=graphics.guiWidth()-hudWidth-margin;hudY=margin+18;}
        hudX = Math.max(0, Math.min(hudX, graphics.guiWidth() - hudWidth));
        hudY = Math.max(0, Math.min(hudY, graphics.guiHeight() - hudHeight - 18));
        var font = mc.font;
        if (paused) {
            graphics.fill(hudX - 2, hudY - 2, hudX + hudWidth + 2, hudY + hudHeight + 2, 0xEE101114);
            graphics.text(font, "YouTube 一時停止中 · クリックで再開", hudX + 4, hudY + 5, -1);
            return;
        }
        graphics.fill(hudX - 2, hudY - 18, hudX + hudWidth + 2, hudY + hudHeight + 2, 0xEE101114);
        graphics.text(font, "←  →  ↻  YouTube", hudX + 3, hudY - 13, -1);
        youtube.drawMiniPlayer(graphics, hudX, hudY, hudWidth, hudHeight, PersonalConfig.store().integer("youtube", "volume"));
    }

    /** Timed structure popup and full-track PvP death media driven by server state serials. */
    public static void triggerWorldAd(int durationTicks) {
        var mc = Minecraft.getInstance();
        if (!ClientRules.enabled("structure_ads") || !ClientRules.enabled("advertisements") || ShopClient.adPremium() || mc.level == null) return;
        var urls = ClientRules.list("advertisements", "urls");
        String selected = urls.isEmpty() ? "sum1rem0d:default_ad" : urls.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(urls.size()));
        closeWorldAd();
        worldAd = new BrowserSession(selected.equals("sum1rem0d:default_ad") ? defaultAd() : safe(selected, defaultAd()));
        worldAdTicksRemaining = Math.max(1, Math.min(1200, durationTicks));
    }

    public static void playDeathMedia(String serverSource) {
        var mc = Minecraft.getInstance();
        if (!ClientRules.enabled("pvp_death_media") || mc.level == null) return;
        String source = localDeathMediaSource();
        if (source.isEmpty()) source = safe(serverSource == null ? "" : serverSource.trim(), "");
        if (source.isEmpty()) { closeDeathMedia(); return; }
        closeDeathMedia(); deathMedia = new BrowserSession(source); deathPlayAttemptAt = 0;
    }

    private static String localDeathMediaSource() {
        String configured = PersonalConfig.store().text("pvp_death_media", "local_file").trim();
        if (configured.isEmpty()) return "";
        try {
            java.nio.file.Path path = configured.regionMatches(true, 0, "file:", 0, 5)
                    ? java.nio.file.Path.of(java.net.URI.create(configured)) : java.nio.file.Path.of(configured);
            if (!path.isAbsolute()) path = Minecraft.getInstance().gameDirectory.toPath().resolve(path);
            path = path.toAbsolutePath().normalize();
            return java.nio.file.Files.isRegularFile(path) ? path.toUri().toString() : "";
        } catch (RuntimeException invalidPath) { return ""; }
    }

    public static void renderWorldMedia(GuiGraphicsExtractor graphics) {
        var mc = Minecraft.getInstance();
        if (worldAd != null && mc.level != null && !ShopClient.adPremium()) {
            int width = Math.min(500, Math.max(1, graphics.guiWidth() - 24));
            int height = Math.min(300, Math.max(1, graphics.guiHeight() - 70));
            int x = (graphics.guiWidth() - width) / 2, y = (graphics.guiHeight() - height) / 2;
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0xA8000000);
            graphics.fill(x - 3, y - 23, x + width + 3, y + height + 3, 0xFF202126);
            graphics.text(mc.font, "広告", x + 5, y - 16, 0xFFCCCCCC);
            worldAd.draw(graphics, x, y, width, height, PersonalConfig.store().integer("advertisements", "volume"));
        }
    }

    public static void stopWorldMedia() { closeWorldAd(); closeDeathMedia(); }
    static BrowserSession worldAdSession() { return worldAd; }
    static BrowserSession deathMediaSession() { return deathMedia; }
    static int worldAdTicksRemaining() { return worldAdTicksRemaining; }
    private static void closeWorldAd() { if (worldAd != null) worldAd.close(); worldAd = null; worldAdTicksRemaining = 0; }
    private static void closeDeathMedia() { if (deathMedia != null) deathMedia.close(); deathMedia = null; deathPlayAttemptAt = 0; }

    /** Intercepts only clicks within the visible background player. */
    public static boolean hudClick(MouseButtonEvent event, boolean doubled) {
        if (!hudVisible() || Minecraft.getInstance().gui.screen() != null || Minecraft.getInstance().level == null) return false;
        if (event.x() < hudX - 2 || event.x() >= hudX + hudWidth + 2 || event.y() < hudY - 18 || event.y() >= hudY + hudHeight + 2) return false;
        if (event.y() < hudY) {
            if (event.x() < hudX + 54) {
                int part = Math.max(0, (int) ((event.x() - hudX) / 18));
                if (part == 0) youtube.back(); else if (part == 1) youtube.forward(); else if (part == 2) youtube.reload();
            } else {
                draggingHud = true; youtube.focus(false); dragOffsetX = event.x() - hudX; dragOffsetY = event.y() - (hudY - 18);
            }
            return true;
        }
        if (pausedBar()) youtube.playMedia(); else youtube.click(event, doubled);
        return true;
    }
    public static boolean hudRelease(MouseButtonEvent event) {
        if (draggingHud) { draggingHud = false; return true; }
        if (hudVisible() && youtube.focused() && Minecraft.getInstance().gui.screen() == null) { youtube.release(event); return true; }
        return false;
    }
    public static boolean hudDrag(double x, double y) {
        if (draggingHud) {
            var mc = Minecraft.getInstance();
            hudMoved=true;
            hudX = (int) Math.max(0, Math.min(mc.getWindow().getGuiScaledWidth() - hudWidth, x - dragOffsetX));
            hudY = (int) Math.max(18, Math.min(mc.getWindow().getGuiScaledHeight() - hudHeight - 2, y - dragOffsetY + 18));
            return true;
        }
        if (hudVisible() && youtube.focused() && Minecraft.getInstance().gui.screen() == null) { youtube.move(x, y); return true; }
        return false;
    }
    public static boolean hudScroll(double x, double y, double amount) {
        return hudVisible() && Minecraft.getInstance().gui.screen() == null && youtube.scroll(x, y, amount);
    }
    public static String defaultAd() {
        try (var stream = BrowserOverlays.class.getResourceAsStream("/assets/sum1rem0d/browser/default-ad.html")) {
            if (stream == null) throw new IllegalStateException("Missing advertisement");
            return "data:text/html;base64," + Base64.getEncoder().encodeToString(stream.readAllBytes());
        } catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
    }
    private static void closeAd() { if (ad != null) ad.close(); ad = null; }
    public static boolean click(Screen screen, MouseButtonEvent event, boolean doubled) {
        if (owner == null || owner != screen) return false;
        if (ad != null) {
            if (event.y() >= adY - 23 && event.y() < adY && event.x() >= adX + adWidth - 95 && System.nanoTime() - adOpened >= ClientRules.integer("advertisements", "skip_after") * 1_000_000_000L) closeAd();
            else ad.click(event, doubled);
            return true;
        }
        if (youtube != null && inventory(screen) && layout(screen).visible()) {
            if (event.y() >= youtubeY - 18 && event.y() < youtubeY && event.x() >= youtubeX && event.x() < youtubeX + 54) {
                int part = (int) ((event.x() - youtubeX) / 18); if (part == 0) youtube.back(); else if (part == 1) youtube.forward(); else youtube.reload(); return true;
            }
            return youtube.click(event, doubled);
        }
        return false;
    }
    public static boolean release(Screen screen, MouseButtonEvent event) { if (owner == null || owner != screen) return false; if (ad != null) { ad.release(event); return true; } if (youtube != null && youtube.focused()) { youtube.release(event); return true; } return false; }
    public static boolean drag(Screen screen, MouseButtonEvent event) {
        if (owner == null || owner != screen) return false;
        BrowserSession target = ad != null ? ad : youtube != null && youtube.focused() ? youtube : null;
        if (target == null) return false;
        target.move(event.x(), event.y()); return true;
    }
    public static boolean scroll(Screen screen, double x, double y, double amount) { if (owner == null || owner != screen) return false; if (ad != null) { ad.scroll(x, y, amount); return true; } return youtube != null && inventory(screen) && layout(screen).visible() && youtube.scroll(x, y, amount); }
    public static void moved(double x, double y) { var mc = Minecraft.getInstance(); if (owner == null || owner != mc.gui.screen()) return; if (ad != null) ad.move(x, y); else if (youtube != null) youtube.move(x, y); }
    public static boolean key(KeyEvent event, boolean released) {
        if (owner == null || owner != Minecraft.getInstance().gui.screen()) return false;
        if (ad != null) {
            if (!released && event.key() == InputConstants.KEY_ESCAPE && System.nanoTime() - adOpened >= ClientRules.integer("advertisements", "skip_after") * 1_000_000_000L) closeAd();
            else ad.key(event, released);
            return true;
        }
        if (youtube != null && youtube.focused()) {
            if (!released && event.key() == InputConstants.KEY_ESCAPE) { youtube.focus(false); return true; }
            return youtube.key(event, released);
        }
        return false;
    }
    public static boolean character(CharacterEvent event) { if (owner == null || owner != Minecraft.getInstance().gui.screen()) return false; return ad != null ? ad.character(event) : youtube != null && youtube.character(event); }
    public static void closeAll() { if (youtube != null) youtube.close(); youtube = null; closeAd(); stopWorldMedia(); owner = null; draggingHud = false; hudMoved=false; lastYoutubeUrl = null; configuredYoutubeUrl = null; }
}
