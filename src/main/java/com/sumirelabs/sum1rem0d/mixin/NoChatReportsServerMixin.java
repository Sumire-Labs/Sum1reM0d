package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.NoChatReports;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedServer.class)
public abstract class NoChatReportsServerMixin {
  @Inject(method = "enforceSecureProfile", at = @At("HEAD"), cancellable = true)
  private void sum1rem0d$allowUnsigned(CallbackInfoReturnable<Boolean> cir) {
    if (NoChatReports.enabled()) cir.setReturnValue(false);
  }
}
