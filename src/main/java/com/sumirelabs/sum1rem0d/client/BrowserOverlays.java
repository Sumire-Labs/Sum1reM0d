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
    private static BrowserSession youtube, ad;
    private static String lastYoutubeUrl;
    private static String configuredYoutubeUrl;
    private static long nextAd, adOpened;
    private static int youtubeX, youtubeY, youtubeWidth, youtubeHeight, adX, adY, adWidth, adHeight;
    private BrowserOverlays() {}
    static BrowserSession youtubeSession() { return youtube; }
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
            lastYoutubeUrl = youtube.url(); youtube.focus(false);
            if (PersonalConfig.store().bool("youtube", "stop_on_close") || Minecraft.getInstance().level == null) { youtube.close(); youtube = null; }
        }
        if (ad != null) { ad.close(); ad = null; }
        owner = null;
    }
    public static void tick() {
        var mc = Minecraft.getInstance();
        if (owner != null && owner != mc.gui.screen()) changed(mc.gui.screen());
        if (youtube != null && (!ClientRules.enabled("youtube") || mc.level == null)) { youtube.close(); youtube = null; }
        if (ad != null && (!ClientRules.enabled("advertisements") || mc.level == null || !advertisementScreen(mc.gui.screen()))) closeAd();
        if (youtube != null) youtube.updateVolume(PersonalConfig.store().integer("youtube", "volume"));
        if (ad != null) ad.updateVolume(PersonalConfig.store().integer("advertisements", "volume"));
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
        if (advertisementScreen(screen) && ClientRules.enabled("advertisements") && now >= nextAd && ad == null) {
            var urls = ClientRules.list("advertisements", "urls");
            String url = urls.isEmpty() ? "https://www.youtube.com/" : urls.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(urls.size()));
            ad = new BrowserSession(url.equals("sum1rem0d:default_ad") ? defaultAd() : safe(url, defaultAd()));
            adOpened = now; nextAd = now + (long) ClientRules.number("advertisements", "interval") * 1_000_000_000L;
        }
        if (ad != null) {
            if (!ClientRules.enabled("advertisements")) { closeAd(); return; }
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
    public static void closeAll() { if (youtube != null) youtube.close(); youtube = null; closeAd(); owner = null; lastYoutubeUrl = null; configuredYoutubeUrl = null; }
}
