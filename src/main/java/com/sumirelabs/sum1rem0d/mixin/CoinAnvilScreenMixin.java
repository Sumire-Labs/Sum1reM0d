package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.CoinPayments;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(AnvilScreen.class)
public abstract class CoinAnvilScreenMixin {
  @Redirect(
      method = "extractLabels",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"))
  private MutableComponent sum1rem0d$cost(String key, Object[] args) {
    if (CoinPayments.enabled("coin_anvil") && key.equals("container.repair.cost"))
      return Component.translatable(
          "gui.sum1rem0d.coin_cost",
          CoinPayments.cost("coin_anvil", ((Number) args[0]).intValue()));
    return Component.translatable(key, args);
  }
}
