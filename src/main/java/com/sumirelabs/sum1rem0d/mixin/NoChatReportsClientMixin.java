package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.client.ChatPrivacy;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.player.ProfileKeyPair;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class NoChatReportsClientMixin {
  @Unique private boolean sum1rem0d$suppressSigning;

  @Inject(method = "<init>", at = @At("TAIL"))
  private void sum1rem0d$capture(CallbackInfo ci) {
    sum1rem0d$suppressSigning = ChatPrivacy.suppressSigning();
  }

  @Inject(method = "setKeyPair", at = @At("HEAD"), cancellable = true)
  private void sum1rem0d$noSession(ProfileKeyPair pair, CallbackInfo ci) {
    if (sum1rem0d$suppressSigning) ci.cancel();
  }

  @Inject(method = "handleSystemChat", at = @At("HEAD"))
  private void sum1rem0d$verifySystem(
      net.minecraft.network.protocol.game.ClientboundSystemChatPacket packet, CallbackInfo ci) {
    com.sumirelabs.sum1rem0d.client.ChatPrivacySmoke.system(packet);
  }

  @Inject(method = "handlePlayerChat", at = @At("HEAD"))
  private void sum1rem0d$verifyPlayer(
      net.minecraft.network.protocol.game.ClientboundPlayerChatPacket packet, CallbackInfo ci) {
    com.sumirelabs.sum1rem0d.client.ChatPrivacySmoke.player(packet);
  }
}
