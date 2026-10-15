package com.sumirelabs.sum1rem0d.game;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.network.ServerConfigHandler;
import java.nio.file.Files;
import java.util.ArrayList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Opt-in development-only sender, for a single local Developer client. */
public final class ToastVerification {
    private static boolean finished, sent;
    private static long started;
    private static JsonObject original;
    private static final java.util.List<String> passed = new ArrayList<>();
    private ToastVerification() {}
    public static void tick(MinecraftServer server) {
        if (!Boolean.getBoolean("sum1rem0d.toastVerify") || finished) return;
        try {
            var players = server.getPlayerList().getPlayers();
            if (!sent && players.isEmpty()) return;
            if (started == 0) started = System.nanoTime();
            if (System.nanoTime() - started > 180_000_000_000L) throw new AssertionError("Toast verification client timeout");
            if (sent) { if (players.isEmpty()) finish(server, true, ""); return; }
            if (players.isEmpty()) return;
            ServerVerification.require(players.size() == 1 && players.getFirst().getGameProfile().name().equals("Developer"), "Toast verification requires only the local Developer account");
            var player = players.getFirst(); original = Sum1reM0d.config().snapshot(); var draft = original.deepCopy();
            var options = draft.getAsJsonObject("features").getAsJsonObject("sleep_advancement");
            options.addProperty("enabled", true); options.addProperty("fake_only", true); options.addProperty("chance", 100); options.addProperty("fake_seconds", 3);
            Sum1reM0d.config().update(draft, Sum1reM0d.config().revision()); ServerConfigHandler.send(player, "");
            var selected = Gameplay.sleepAdvancement(player);
            ServerVerification.require(selected != null && !player.getAdvancements().getOrStartProgress(selected).isDone(), "Actual connected player's progress is unchanged by fake toast");
            passed.add("connected player receives fake display payload without awarding progress"); sent = true;
            Sum1reM0d.LOGGER.info("TOAST_VERIFY: fake toast sent for {}", selected.id());
        } catch (Throwable failure) { Sum1reM0d.LOGGER.error("TOAST_VERIFY failed", failure); finish(server, false, failure.toString()); }
    }
    private static void finish(MinecraftServer server, boolean success, String failure) {
        finished = true;
        if (original != null) try { Sum1reM0d.config().update(original, Sum1reM0d.config().revision()); passed.add("server configuration restored"); }
        catch (Exception error) { success = false; failure += "; restore: " + error; }
        var result = new JsonObject(); result.addProperty("success", success); result.addProperty("failure", failure); result.add("passed", new Gson().toJsonTree(passed));
        try { Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("sum1rem0d-toast-verification.json"), new GsonBuilder().setPrettyPrinting().create().toJson(result)); }
        catch (Exception error) { Sum1reM0d.LOGGER.error("Cannot save toast server report", error); }
        server.halt(false);
    }
}
