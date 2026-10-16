package com.sumirelabs.sum1rem0d.game;

import com.sumirelabs.sum1rem0d.Sum1reM0d;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

/** Core report prevention, adapted from No Chat Reports (WTFPL); see THIRD_PARTY_NOTICES.md. */
public final class NoChatReports {
  private NoChatReports() {}

  // This also runs on the network thread: never consult another server's client snapshot.
  public static boolean enabled() {
    return Sum1reM0d.initialized() && Sum1reM0d.config().enabled("no_chat_reports");
  }

  public static Packet<?> outgoing(Packet<?> packet) {
    if (!enabled() || !(packet instanceof ClientboundPlayerChatPacket chat)) return packet;
    return systemMessage(
        chat.body().content(),
        chat.unsignedContent().orElseGet(() -> Component.literal(chat.body().content())),
        chat.filterMask(),
        chat.chatType());
  }

  public static ClientboundSystemChatPacket systemMessage(
      String raw,
      Component decorated,
      net.minecraft.network.chat.FilterMask filter,
      net.minecraft.network.chat.ChatType.Bound type) {
    if (filter.isFullyFiltered()) return new ClientboundSystemChatPacket(Component.empty(), false);
    Component content = filter.isEmpty() ? decorated : filter.applyWithFormatting(raw);
    return new ClientboundSystemChatPacket(type.decorate(content), false);
  }
}
