package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.client.ChatPrivacy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class NoChatReportsKeysMixin {
  @Inject(method = "getProfileKeyPairManager", at = @At("HEAD"), cancellable = true)
  private void sum1rem0d$noKeys(CallbackInfoReturnable<ProfileKeyPairManager> cir) {
    if (ChatPrivacy.suppressSigning()) cir.setReturnValue(ProfileKeyPairManager.EMPTY_KEY_MANAGER);
  }
}
