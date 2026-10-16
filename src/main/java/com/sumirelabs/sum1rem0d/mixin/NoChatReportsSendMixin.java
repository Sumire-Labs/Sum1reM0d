package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.NoChatReports;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Converts both native send overloads without losing their completion callbacks. */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class NoChatReportsSendMixin {
  @ModifyVariable(
      method = {
        "send(Lnet/minecraft/network/protocol/Packet;)V",
        "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V"
      },
      at = @At("HEAD"),
      argsOnly = true)
  private Packet<?> sum1rem0d$unsignedChat(Packet<?> packet) {
    return (Object) this instanceof ServerGamePacketListenerImpl
        ? NoChatReports.outgoing(packet)
        : packet;
  }
}
