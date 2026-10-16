package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.CoinServicesMultiplayerVerification;
import net.minecraft.advancements.triggers.EnchantedItemTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantedItemTrigger.class)
public abstract class VerificationEnchantmentCriteriaMixin {
  @Inject(method = "trigger", at = @At("HEAD"), cancellable = true)
  private void sum1rem0d$temporary(
      ServerPlayer player, ItemStack stack, int levels, CallbackInfo ci) {
    if (CoinServicesMultiplayerVerification.temporaryInventory(player)) ci.cancel();
  }
}
