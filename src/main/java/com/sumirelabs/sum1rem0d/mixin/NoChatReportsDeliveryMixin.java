package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.NoChatReports;
import net.minecraft.network.chat.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bypass native signature acknowledgements for messages that are never sent as player chat. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class NoChatReportsDeliveryMixin {
  @Inject(method = "sendPlayerChatMessage", at = @At("HEAD"), cancellable = true)
  private void sum1rem0d$deliver(PlayerChatMessage message, ChatType.Bound type, CallbackInfo ci) {
    if (!NoChatReports.enabled()) return;
    ci.cancel();
    if (!message.isFullyFiltered())
      ((ServerGamePacketListenerImpl) (Object) this)
          .send(
              NoChatReports.systemMessage(
                  message.signedContent(), message.decoratedContent(), message.filterMask(), type));
  }
}
