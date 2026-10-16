package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;

import com.mojang.serialization.JsonOps;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.*;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.*;

final class NoChatReportsVerification {
  private NoChatReportsVerification() {}

  static void verify(MinecraftServer server, Consumer<String> passed) throws Exception {
    var config = Sum1reM0d.config();
    var before = config.snapshot();
    var received = new ArrayList<Packet<?>>();
    EmbeddedChannel[] transport = {null};
    var connection =
        new Connection(PacketFlow.SERVERBOUND) {
          @Override
          public void send(Packet<?> packet, ChannelFutureListener listener, boolean flush) {
            received.add(packet);
            if (listener != null)
              try {
                listener.operationComplete(transport[0].newSucceededFuture());
              } catch (Exception failure) {
                throw new IllegalStateException(failure);
              }
          }
        };
    var channel = new EmbeddedChannel(connection);
    transport[0] = channel;
    var p = new PlayerVerification.TestPlayer(server, server.overworld(), "SumireChatVerify");
    var listener =
        new ServerGamePacketListenerImpl(
            server, connection, p, CommonListenerCookie.createInitial(p.getGameProfile(), false));
    p.connection = listener;
    try {
      var enabled = before.deepCopy();
      enabled
          .getAsJsonObject("features")
          .getAsJsonObject("no_chat_reports")
          .addProperty("enabled", true);
      config.update(enabled, config.revision());
      require(
          !server.enforceSecureProfile(), "Enabled report prevention accepts unsigned profiles");
      var type =
          ChatType.bind(ChatType.CHAT, server.registryAccess(), Component.literal("ChatTester"));
      var raw = PlayerChatMessage.unsigned(p.getUUID(), "private chat");
      var signed =
          new PlayerChatMessage(
              raw.link(),
              new MessageSignature(new byte[256]),
              raw.signedBody(),
              Component.literal("styled content").withStyle(net.minecraft.ChatFormatting.AQUA),
              FilterMask.PASS_THROUGH);
      listener.sendPlayerChatMessage(signed, type);
      require(
          received.getLast() instanceof ClientboundSystemChatPacket system
              && system.content().getString().contains("styled content")
              && system.content().getString().contains("ChatTester"),
          "Signed native chat becomes decorated system chat");
      var cacheField = ServerGamePacketListenerImpl.class.getDeclaredField("lastSeenMessages");
      cacheField.setAccessible(true);
      var acknowledgements = (LastSeenMessagesValidator) cacheField.get(listener);
      int tracked = acknowledgements.trackedMessagesCount();
      for (int i = 0; i < 4100; i++) listener.sendPlayerChatMessage(signed, type);
      require(
          acknowledgements.trackedMessagesCount() == tracked,
          "Converted messages must never accumulate signature acknowledgements");
      passed.accept(
          "No Chat Reports signed chat conversion, decoration and 4100-message acknowledgement"
              + " regression");
      var filter = new FilterMask(12);
      for (int i = 0; i < 7; i++) filter.setFiltered(i);
      listener.sendPlayerChatMessage(raw.filter(filter), type);
      require(
          received.getLast() instanceof ClientboundSystemChatPacket filtered
              && !filtered.content().getString().contains("private")
              && filtered.content().getString().contains("chat"),
          "Partial native filter is preserved");
      int count = received.size();
      listener.sendPlayerChatMessage(raw.filter(FilterMask.FULLY_FILTERED), type);
      require(received.size() == count, "Fully filtered chat is not broadcast");
      passed.accept("No Chat Reports preserves partial and full Minecraft content filters");
      var packet =
          packet(p.getUUID(), type, FilterMask.PASS_THROUGH, Component.literal("direct packet"));
      listener.send(packet);
      require(
          received.getLast() instanceof ClientboundSystemChatPacket,
          "Single send overload converts direct packets");
      boolean[] callback = {false};
      listener.send(packet, future -> callback[0] = true);
      require(
          callback[0] && received.getLast() instanceof ClientboundSystemChatPacket,
          "Callback send overload converts and preserves callback");
      var system = new ClientboundSystemChatPacket(Component.literal("unrelated system"), true);
      listener.send(system);
      require(received.getLast() == system, "Existing system and actionbar packets pass through");
      passed.accept(
          "No Chat Reports converts both native send overloads and preserves callbacks and"
              + " actionbar");
      var status =
          new ServerStatus(
              Component.literal("Fixture"),
              Optional.empty(),
              Optional.of(ServerStatus.Version.current()),
              Optional.empty(),
              false);
      var encoded = ServerStatus.CODEC.encodeStart(JsonOps.INSTANCE, status).getOrThrow();
      require(
          encoded.getAsJsonObject().get("preventsChatReports").getAsBoolean(),
          "NCR server discovery field is encoded");
      require(
          ServerStatus.CODEC
              .parse(JsonOps.INSTANCE, encoded)
              .getOrThrow()
              .description()
              .equals(status.description()),
          "Server status codec remains round-trippable");
      passed.accept("No Chat Reports NCR-compatible server discovery status round-trip");
      var off = config.snapshot();
      off.getAsJsonObject("features")
          .getAsJsonObject("no_chat_reports")
          .addProperty("enabled", false);
      config.update(off, config.revision());
      listener.send(packet);
      require(received.getLast() == packet, "OFF preserves original player packet");
      require(
          !ServerStatus.CODEC
              .encodeStart(JsonOps.INSTANCE, status)
              .getOrThrow()
              .getAsJsonObject()
              .has("preventsChatReports"),
          "OFF must not advertise report prevention");
      passed.accept("No Chat Reports OFF restores native chat and removes discovery flag");
    } finally {
      config.update(before, config.revision());
      channel.finishAndReleaseAll();
    }
  }

  private static ClientboundPlayerChatPacket packet(
      UUID sender, ChatType.Bound type, FilterMask filter, Component content) {
    var signature = new MessageSignature(new byte[256]);
    var body =
        new SignedMessageBody.Packed(
            "private chat", Instant.now(), 0, LastSeenMessages.Packed.EMPTY);
    //? if >=26.3 {
    return new ClientboundPlayerChatPacket(
        0, sender, 0, Optional.of(signature), body, Optional.ofNullable(content), filter, type);
    //?} else {
    /*return new ClientboundPlayerChatPacket(0, sender, 0, signature, body, content, filter, type);*/
    //?}
  }
}
