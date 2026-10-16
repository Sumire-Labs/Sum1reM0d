package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.*;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(EnchantmentScreen.class)
public abstract class CoinEnchantingScreenMixin {
  @Redirect(
      method = {"extractBackground", "extractRenderState"},
      at = @At(value = "FIELD", target = "Lnet/minecraft/world/inventory/EnchantmentMenu;costs:[I"))
  private int[] sum1rem0d$costs(EnchantmentMenu menu) {
    return CoinPayments.displayCosts(menu.costs);
  }

  @Redirect(
      method = {"extractBackground", "extractRenderState"},
      at =
          @At(
              value = "FIELD",
              target = "Lnet/minecraft/client/player/LocalPlayer;experienceLevel:I"))
  private int sum1rem0d$balance(LocalPlayer player) {
    return CoinPayments.enabled("coin_enchanting")
        ? CoinPayments.displayBalance(player)
        : player.experienceLevel;
  }

  @ModifyVariable(method = "extractBackground", at = @At("STORE"), ordinal = 0)
  private String sum1rem0d$label(String label) {
    return CoinPayments.enabled("coin_enchanting") ? label + " C" : label;
  }

  @Redirect(
      method = "extractRenderState",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"))
  private MutableComponent sum1rem0d$tooltip(String key, Object[] args) {
    if (CoinPayments.enabled("coin_enchanting")) {
      if (key.equals("container.enchant.level.requirement"))
        return Component.translatable("gui.sum1rem0d.coin_requirement", args);
      if (key.equals("container.enchant.level.many"))
        return Component.translatable(
            "gui.sum1rem0d.coin_cost",
            CoinPayments.cost("coin_enchanting", ((Number) args[0]).intValue()));
    }
    return Component.translatable(key, args);
  }

  @Redirect(
      method = "extractRenderState",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"))
  private MutableComponent sum1rem0d$one(String key) {
    return CoinPayments.enabled("coin_enchanting") && key.equals("container.enchant.level.one")
        ? Component.translatable("gui.sum1rem0d.coin_cost", CoinPayments.cost("coin_enchanting", 1))
        : Component.translatable(key);
  }
}
