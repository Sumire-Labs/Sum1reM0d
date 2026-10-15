package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.network.GameStatePayload;
import com.sumirelabs.sum1rem0d.network.ConfigStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ClientRuntime {
    public static int cameraTicks, redTicks;
    private static boolean cameraActive;
    private static float originalYaw, originalPitch, lockedYaw, lockedPitch;
    private static boolean inputActivity;
    private static int activityCooldown;
    private ClientRuntime() {}
    public static void acceptRules(ConfigStatePayload payload) {
        var before = ClientRules.snapshot().getAsJsonObject("features");
        ClientRules.accept(payload);
        var after = ClientRules.snapshot().getAsJsonObject("features");
        var mc = Minecraft.getInstance();
        if (mc.level != null && java.util.List.of("upside_down_grass", "diamond_disguise", "furnace_textures").stream().anyMatch(id -> !before.get(id).equals(after.get(id))))
            mc.levelRenderer.invalidateCompiledGeometry(mc.level, mc.options, mc.gameRenderer.mainCamera(), mc.getBlockColors());
    }
    public static void accept(GameStatePayload payload) {
        if (VisualVerification.defer(payload)) return;
        applyState(payload);
    }
    static void applyState(GameStatePayload payload) {
        boolean grass = ClientGameState.daily("upside_down_grass"), ore = ClientGameState.daily("diamond_disguise");
        int permutation = (int) ClientGameState.number("furnacePermutation", -1);
        ClientGameState.accept(payload);
        cameraTicks = (int) ClientGameState.number("cameraTicks", 0); redTicks = (int) ClientGameState.number("redTicks", 0);
        if (cameraTicks > 0) {
            originalYaw = (float) ClientGameState.number("originalYaw", 0); originalPitch = (float) ClientGameState.number("originalPitch", 0);
            lockedYaw = (float) ClientGameState.number("cameraYaw", 0); lockedPitch = (float) ClientGameState.number("cameraPitch", 0);
        }
        var mc = Minecraft.getInstance();
        if (mc.level != null && (grass != ClientGameState.daily("upside_down_grass") || ore != ClientGameState.daily("diamond_disguise") || permutation != (int) ClientGameState.number("furnacePermutation", -1))) mc.levelRenderer.invalidateCompiledGeometry(mc.level, mc.options, mc.gameRenderer.mainCamera(), mc.getBlockColors());
    }
    public static void tick() {
        var mc = Minecraft.getInstance();
        ClientSmoke.tick(mc);
        NetworkSmoke.tick(mc);
        BrowserSmoke.tick(mc);
        ToastSmoke.tick(mc);
        ViewerSmoke.tick(mc);
        EmiSmoke.tick(mc);
        MultiplayerSmoke.tick(mc);
        EconomySmoke.tick(mc); WikiSmoke.tick(mc); WikiRuntime.tick(mc);
        BrowserOverlays.tick();
        AfkLights.tick();
        if (mc.player == null) { cameraTicks = 0; redTicks = 0; cameraActive = false; return; }
        if (--activityCooldown <= 0 && inputActivity) { PlatformClientNetwork.activity(); inputActivity = false; activityCooldown = 20; }
        tickCamera(mc);
        redTicks = Math.max(0, redTicks - 1);
    }
    public static boolean cameraLocked() { return cameraTicks > 0 && ClientRules.enabled("camera_lock"); }
    static void tickCamera(Minecraft mc) {
        if (cameraLocked()) {
            mc.player.setYRot(lockedYaw); mc.player.setXRot(lockedPitch); cameraActive = true; cameraTicks--;
        } else if (cameraActive) {
            mc.player.setYRot(originalYaw); mc.player.setXRot(originalPitch); cameraActive = false;
        }
    }
    public static void input() { inputActivity = true; }
    public static void overlay(GuiGraphicsExtractor graphics) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !ClientGameState.received()) return;
        if (redTicks > 0 && ClientRules.enabled("sheep_red")) {
            int alpha = (int) (ClientRules.number("sheep_red", "opacity") * 255);
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24 | 0xFF0000);
        }
        if (ClientRules.enabled("headache") && ClientRules.bool("headache", "visual") && ClientGameState.bool("headache")) {
            int alpha = (int) (ClientRules.number("headache", "visual_opacity") * (0.85 + 0.15 * Math.sin(System.nanoTime() / 1.0E9)) * 255);
            int color = alpha << 24;
            graphics.fill(0, 0, graphics.guiWidth(), 16, color); graphics.fill(0, graphics.guiHeight() - 16, graphics.guiWidth(), graphics.guiHeight(), color);
            graphics.fill(0, 16, 16, graphics.guiHeight() - 16, color); graphics.fill(graphics.guiWidth() - 16, 16, graphics.guiWidth(), graphics.guiHeight() - 16, color);
        }
        if (ClientRules.enabled("economy") && PersonalConfig.store().bool("economy","hud")) {
            String label = "Sum1reコ↑イン↓ " + WalletScreen.number(WalletScreen.state(),"balance");
            int x=8,y=28; graphics.fill(x,y,x+mc.font.width(label)+6,y+13,0xA0000000); graphics.text(mc.font,label,x+3,y+2,0xFFFFD060);
        }
        if (ClientRules.enabled("cleanliness") && PersonalConfig.store().bool("cleanliness", "hud")) {
            int x = 8, y = 8;
            double max = ClientRules.number("cleanliness", "maximum"), clean = ClientGameState.number("cleanliness", max);
            String label = "清潔度 " + (int) clean + " / " + (int) max;
            graphics.fill(x, y, x + Math.max(92, mc.font.width(label) + 6), y + 15, 0xA0000000);
            graphics.fill(x + 2, y + 11, x + 2 + (int) (Math.max(0, Math.min(1, clean / max)) * 88), y + 13, clean < max / 2 ? 0xFFE89964 : 0xFF69CDB8);
            graphics.text(mc.font, label, x + 3, y + 1, 0xFFFFFFFF);
        }
    }
}
