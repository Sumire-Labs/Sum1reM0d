package com.sumirelabs.sum1rem0d.client;

import com.google.gson.JsonObject;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.network.ConfigRequestPayload;
import java.nio.file.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/** Explicit development-only native multiplayer check, against a local disposable server.
 * Start as a non-OP; grant Developer OP from that server's console after the denial check. */
public final class NetworkSmoke {
    private static int stage;
    private static long changedAt, request, initialRevision;
    private static long lastConnect;
    private static JsonObject original, modified;
    private static final java.util.List<String> passed = new java.util.ArrayList<>();
    private NetworkSmoke() {}
    public static void tick(Minecraft mc) {
        if (!Boolean.getBoolean("sum1rem0d.networkSmoke") || stage == -1 || !mc.isGameLoadFinished()) return;
        try {
            long now = System.nanoTime();
            if (changedAt != 0 && now - changedAt > 180_000_000_000L) throw new AssertionError("Timeout at stage " + stage);
            switch (stage) {
                case 0 -> { if (mc.gui.overlay() != null) return; connect(mc); advance(now); }
                case 1 -> {
                    if (mc.player == null && mc.getConnection() == null && now - lastConnect > 20_000_000_000L) connect(mc);
                    if (mc.player == null || !ClientRules.received() || !ClientGameState.received()) return;
                    require(!ClientRules.editable, "Start with a non-OP Developer account");
                    original = ClientRules.snapshot(); initialRevision = ClientRules.revision;
                    modified = original.deepCopy(); var f = modified.getAsJsonObject("features").getAsJsonObject("early_fall");
                    f.addProperty("enabled", !f.get("enabled").getAsBoolean());
                    pass("join receives config and player runtime state"); send(modified, initialRevision); advance(now);
                }
                case 2 -> {
                    if (ClientRules.responseId != request) return;
                    require(!ClientRules.responseApplied && ClientRules.revision == initialRevision && ClientRules.snapshot().equals(original), "Non-OP update must be rejected without mutation");
                    pass("non-OP update denied without mutation; grant Developer OP now"); PlatformClientNetwork.refresh(); advance(now);
                }
                case 3 -> {
                    if (!ClientRules.editable) { if (now - changedAt > 1_000_000_000L) PlatformClientNetwork.refresh(); return; }
                    send(modified, initialRevision); advance(now);
                }
                case 4 -> {
                    if (ClientRules.responseId != request) return;
                    require(ClientRules.responseApplied && ClientRules.revision > initialRevision && ClientRules.snapshot().equals(modified), "OP update must persist and acknowledge its request");
                    pass("OP update succeeds with matching acknowledgement"); send(original, initialRevision); advance(now);
                }
                case 5 -> {
                    if (ClientRules.responseId != request) return;
                    require(!ClientRules.responseApplied && ClientRules.snapshot().equals(modified), "Stale revision must be rejected");
                    pass("stale revision rejected without overwriting newer rules"); send(original, ClientRules.revision); advance(now);
                }
                case 6 -> {
                    if (ClientRules.responseId != request) return;
                    require(ClientRules.responseApplied && ClientRules.snapshot().equals(original), "Original config restored");
                    pass("original config restored"); mc.disconnectFromWorld(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE); advance(now);
                }
                case 7 -> {
                    if (mc.player != null || mc.getConnection() != null) return;
                    require(!ClientRules.received() && !ClientGameState.received(), "Disconnect clears synchronized rules and state");
                    pass("disconnect clears synchronized state"); connect(mc); advance(now);
                }
                case 8 -> {
                    if (mc.player == null || !ClientRules.received() || !ClientGameState.received()) return;
                    require(ClientRules.snapshot().equals(original), "Reconnect receives restored configuration"); pass("reconnect receives restored config and runtime state");
                    finish(mc, true, "");
                }
            }
        } catch (Throwable failure) { Sum1reM0d.LOGGER.error("NETWORK_SMOKE failed", failure); finish(mc, false, failure.toString()); }
    }
    private static void connect(Minecraft mc) {
        lastConnect = System.nanoTime();
        String address = System.getProperty("sum1rem0d.networkAddress", "127.0.0.1:25565");
        ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(address), new ServerData("Sum1reM0d development verification", address, ServerData.Type.OTHER), false, null);
    }
    private static void send(JsonObject json, long revision) {
        request = ClientRules.nextRequestId();
        require(PlatformClientNetwork.send(new ConfigRequestPayload(true, revision, json.toString(), request)), "Configuration channel available");
    }
    private static void advance(long now) { stage++; changedAt = now; }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void pass(String message) { passed.add(message); Sum1reM0d.LOGGER.info("NETWORK_SMOKE: {}", message); }
    private static void finish(Minecraft mc, boolean success, String failure) {
        stage = -1; var result = new JsonObject(); result.addProperty("success", success); result.addProperty("failure", failure); result.add("passed", new com.google.gson.Gson().toJsonTree(passed));
        try { Files.writeString(mc.gameDirectory.toPath().resolve("sum1rem0d-network-verification.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result)); }
        catch (java.io.IOException exception) { Sum1reM0d.LOGGER.error("Cannot save network verification report", exception); }
        Sum1reM0d.LOGGER.info("NETWORK_SMOKE: completed success={}", success); mc.disconnectFromWorld(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE); mc.stop();
    }
}
