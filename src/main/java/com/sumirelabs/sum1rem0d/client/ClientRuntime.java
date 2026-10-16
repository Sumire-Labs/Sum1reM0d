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
        ChatPrivacySmoke.rules(payload);
        var before = ClientRules.snapshot().getAsJsonObject("features");
        ClientRules.accept(payload);
        var after = ClientRules.snapshot().getAsJsonObject("features");
        var mc = Minecraft.getInstance();
        if (mc.level != null && java.util.List.of("upside_down_grass", "diamond_disguise", "furnace_textures", "sideways_nylium").stream().anyMatch(id -> !before.get(id).equals(after.get(id))))
            mc.levelRenderer.invalidateCompiledGeometry(mc.level, mc.options, mc.gameRenderer.mainCamera(), mc.getBlockColors());
    }
    public static void accept(GameStatePayload payload) {
        if (VisualVerification.defer(payload)) return;
        applyState(payload);
    }
    static void applyState(GameStatePayload payload) {
        boolean nylium = ClientGameState.daily("sideways_nylium");
        boolean grass = ClientGameState.daily("upside_down_grass"), ore = ClientGameState.daily("diamond_disguise");
        int permutation = (int) ClientGameState.number("furnacePermutation", -1);
        ClientGameState.accept(payload);
        TradeInvitationToast.receive(ClientGameState.snapshot());
        cameraTicks = (int) ClientGameState.number("cameraTicks", 0); redTicks = (int) ClientGameState.number("redTicks", 0);
        if (cameraTicks > 0) {
            originalYaw = (float) ClientGameState.number("originalYaw", 0); originalPitch = (float) ClientGameState.number("originalPitch", 0);
            lockedYaw = (float) ClientGameState.number("cameraYaw", 0); lockedPitch = (float) ClientGameState.number("cameraPitch", 0);
        }
        var mc = Minecraft.getInstance();
        if (mc.level != null && (nylium != ClientGameState.daily("sideways_nylium") || grass != ClientGameState.daily("upside_down_grass") || ore != ClientGameState.daily("diamond_disguise") || permutation != (int) ClientGameState.number("furnacePermutation", -1))) mc.levelRenderer.invalidateCompiledGeometry(mc.level, mc.options, mc.gameRenderer.mainCamera(), mc.getBlockColors());
    }
    public static void tick() {
        var mc = Minecraft.getInstance();
        if (!Boolean.getBoolean("sum1rem0d.netherSmoke")) DailyTextures.tick(mc);
        NetherChangesSmoke.tick(mc);
        CombatConcreteSmoke.tick(mc);
        ClientSmoke.tick(mc);
        NetworkSmoke.tick(mc);
        BrowserSmoke.tick(mc);
        ToastSmoke.tick(mc);
        ViewerSmoke.tick(mc);
        EmiSmoke.tick(mc);
        MultiplayerSmoke.tick(mc);
        TradeToastSmoke.tick(mc); SearchSuggestionSmoke.tick(mc); ChatPrivacySmoke.tick(mc); CoinServicesSmoke.tick(mc); EconomySmoke.tick(mc); WikiSmoke.tick(mc); WikiRuntime.tick(mc);
        ClientKeys.tick(mc);
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
        CustomHud.render(graphics);
    }
}
