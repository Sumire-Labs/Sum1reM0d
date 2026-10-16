package com.sumirelabs.sum1rem0d.client;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.network.*;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.player.ProfileKeyPair;

public final class ChatPrivacySmoke {
  private static boolean done, sent, ordinary, command, off, reenabled;
  private static int phase;
  private static long started;
  private static JsonObject original;
  private static final List<String> passed = new ArrayList<>();

  private ChatPrivacySmoke() {}

  public static void rules(ConfigStatePayload state) {
    if (Boolean.getBoolean("sum1rem0d.chatSmoke") && state.message().startsWith("SUMIRE_CHAT:"))
      phase = Integer.parseInt(state.message().substring(12));
  }

  public static void system(ClientboundSystemChatPacket packet) {
    if (!Boolean.getBoolean("sum1rem0d.chatSmoke")) return;
    var text = packet.content().getString();
    ordinary |= text.contains("SUMIRE_CHAT_NORMAL");
    command |= text.contains("SUMIRE_CHAT_COMMAND");
    reenabled |= text.contains("SUMIRE_CHAT_REENABLED");
  }

  public static void player(ClientboundPlayerChatPacket packet) {
    if (!Boolean.getBoolean("sum1rem0d.chatSmoke")) return;
    var text = packet.body().content();
    if (text.contains("SUMIRE_CHAT_NORMAL")
        || text.contains("SUMIRE_CHAT_COMMAND")
        || text.contains("SUMIRE_CHAT_REENABLED"))
      throw new AssertionError("Protected chat arrived as player chat");
    off |= text.contains("SUMIRE_CHAT_OFF");
  }

  private static void ack(int phase) {
    PlatformClientNetwork.send(new ConfigRequestPayload(false, -1, "", -7700 - phase));
  }

  public static void tick(Minecraft mc) {
    if (!Boolean.getBoolean("sum1rem0d.chatSmoke") || done || !mc.isGameLoadFinished()) return;
    try {
      if (started == 0) {
        if (mc.gui.overlay() != null) return;
        original = PersonalConfig.store().snapshot();
        var next = original.deepCopy();
        next.getAsJsonObject("features")
            .getAsJsonObject("no_chat_reports")
            .addProperty("suppress_signing", true);
        PersonalConfig.store().update(next, PersonalConfig.store().revision());
        started = System.nanoTime();
        String address = System.getProperty("sum1rem0d.networkAddress", "127.0.0.1:25589");
        ConnectScreen.startConnecting(
            new TitleScreen(),
            mc,
            ServerAddress.parseString(address),
            new ServerData("Chat privacy verification", address, ServerData.Type.OTHER),
            false,
            null);
        return;
      }
      if (System.nanoTime() - started > 180_000_000_000L
          || mc.gui.screen() instanceof DisconnectedScreen)
        throw new AssertionError("Chat client disconnected/timeout at " + phase);
      if (mc.player == null || !ClientRules.received()) return;
      if (phase == 1 && !sent) {
        if (mc.getProfileKeyPairManager() != ProfileKeyPairManager.EMPTY_KEY_MANAGER)
          throw new AssertionError("Signing manager is active");
        var setter =
            ClientPacketListener.class.getDeclaredMethod("setKeyPair", ProfileKeyPair.class);
        setter.setAccessible(true);
        setter.invoke(mc.player.connection, new Object[] {null});
        var session = ClientPacketListener.class.getDeclaredField("chatSession");
        session.setAccessible(true);
        if (session.get(mc.player.connection) != null)
          throw new AssertionError("Chat session published despite suppression");
        passed.add(
            "native key manager and setKeyPair suppression prevent chat session publication");
        mc.player.connection.sendChat("SUMIRE_CHAT_NORMAL");
        mc.player.connection.sendCommand("msg Developer SUMIRE_CHAT_COMMAND");
        sent = true;
      }
      if (phase == 1 && ordinary && command) {
        passed.add("real chat and private-message command arrive as decorated system chat");
        ack(1);
        phase = -1;
      } else if (phase == 2 && off) {
        passed.add("server OFF receives vanilla player chat");
        ack(2);
        phase = -1;
      } else if (phase == 3 && reenabled) {
        passed.add("server reenable resumes system chat without message ordering failure");
        ack(3);
        phase = -1;
      } else if (phase == 4) {
        passed.add("server restores original configuration");
        ack(4);
        phase = -1;
      } else if (phase == 5) finish(mc, true, "");
    } catch (Throwable error) {
      Sum1reM0d.LOGGER.error("Chat smoke failed", error);
      finish(mc, false, error.toString());
    }
  }

  private static void finish(Minecraft mc, boolean success, String failure) {
    done = true;
    try {
      if (original != null)
        PersonalConfig.store().update(original, PersonalConfig.store().revision());
      var report = new JsonObject();
      report.addProperty("success", success);
      report.addProperty("failure", failure);
      report.add("passed", new Gson().toJsonTree(passed));
      Files.writeString(
          mc.gameDirectory.toPath().resolve("sum1rem0d-chat-client.json"),
          new GsonBuilder().setPrettyPrinting().create().toJson(report));
    } catch (Exception e) {
      Sum1reM0d.LOGGER.error("Cannot save chat client report", e);
    }
    mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
    mc.stop();
  }
}
