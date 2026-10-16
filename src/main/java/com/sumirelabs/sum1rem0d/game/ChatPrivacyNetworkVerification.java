package com.sumirelabs.sum1rem0d.game;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.network.*;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/** Opt-in local fixture; restores Config before normal server shutdown. */
public final class ChatPrivacyNetworkVerification {
  private static JsonObject original;
  private static ServerPlayer player;
  private static int phase = 1, delay;
  private static long started;
  private static boolean done;
  private static final List<String> passed = new ArrayList<>();

  private ChatPrivacyNetworkVerification() {}

  public static boolean request(ConfigRequestPayload request, ServerPlayer sender) {
    if (!Boolean.getBoolean("sum1rem0d.chatVerify")
        || request.update()
        || done
        || sender != player
        || request.requestId() != -7700 - phase) return false;
    try {
      switch (phase) {
        case 1 -> {
          passed.add("real unsigned client chat and message command delivered as system chat");
          var next = Sum1reM0d.config().snapshot();
          next.getAsJsonObject("features")
              .getAsJsonObject("no_chat_reports")
              .addProperty("enabled", false);
          Sum1reM0d.config().update(next, Sum1reM0d.config().revision());
          player.connection.sendPlayerChatMessage(
              PlayerChatMessage.unsigned(player.getUUID(), "SUMIRE_CHAT_OFF"),
              ChatType.bind(ChatType.CHAT, player));
          phase = 2;
        }
        case 2 -> {
          passed.add("live server OFF restores native player chat delivery");
          var next = Sum1reM0d.config().snapshot();
          next.getAsJsonObject("features")
              .getAsJsonObject("no_chat_reports")
              .addProperty("enabled", true);
          Sum1reM0d.config().update(next, Sum1reM0d.config().revision());
          player.connection.sendPlayerChatMessage(
              PlayerChatMessage.unsigned(player.getUUID(), "SUMIRE_CHAT_REENABLED"),
              ChatType.bind(ChatType.CHAT, player));
          phase = 3;
        }
        case 3 -> {
          passed.add("OFF then ON preserves chat ordering and system conversion");
          restore();
          phase = 4;
        }
        case 4 -> {
          delay = 40;
          phase = 5;
        }
        default -> {}
      }
      ServerConfigHandler.send(player, "SUMIRE_CHAT:" + phase);
    } catch (Throwable error) {
      finish(sender.level().getServer(), false, error.toString());
    }
    return true;
  }

  public static void tick(MinecraftServer server) {
    if (!Boolean.getBoolean("sum1rem0d.chatVerify") || done) return;
    try {
      if (original == null) {
        original = Sum1reM0d.config().snapshot();
        var next = original.deepCopy();
        for (var feature : next.getAsJsonObject("features").entrySet())
          feature.getValue().getAsJsonObject().addProperty("enabled", false);
        next.getAsJsonObject("features")
            .getAsJsonObject("no_chat_reports")
            .addProperty("enabled", true);
        Sum1reM0d.config().update(next, Sum1reM0d.config().revision());
      }
      if (player == null) {
        if (server.getPlayerList().getPlayers().isEmpty()) return;
        ServerVerification.require(
            server.getPlayerList().getPlayerCount() == 1, "One local test player required");
        player = server.getPlayerList().getPlayers().getFirst();
        ServerVerification.require(
            player.getGameProfile().name().equals("Developer"), "Local Developer account required");
        started = System.nanoTime();
      }
      if (System.nanoTime() - started > 180_000_000_000L)
        throw new AssertionError("Chat fixture timeout " + phase);
      if (phase == 5) {
        if (--delay <= 0) finish(server, true, "");
      } else if (server.overworld().getGameTime() % 20 == 0)
        ServerConfigHandler.send(player, "SUMIRE_CHAT:" + phase);
    } catch (Throwable error) {
      finish(server, false, error.toString());
    }
  }

  public static void stopping(MinecraftServer server) {
    if (Boolean.getBoolean("sum1rem0d.chatVerify")) restore();
  }

  private static void restore() {
    if (original != null)
      try {
        Sum1reM0d.config().update(original, Sum1reM0d.config().revision());
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
  }

  private static void finish(MinecraftServer server, boolean success, String failure) {
    done = true;
    try {
      restore();
      var report = new JsonObject();
      report.addProperty("success", success);
      report.addProperty("failure", failure);
      report.add("passed", new Gson().toJsonTree(passed));
      Files.writeString(
          server.getWorldPath(LevelResource.ROOT).resolve("sum1rem0d-chat-network.json"),
          new GsonBuilder().setPrettyPrinting().create().toJson(report));
    } catch (Exception error) {
      Sum1reM0d.LOGGER.error("Chat fixture failed", error);
    }
    server.halt(false);
  }
}
