package com.sumirelabs.sum1rem0d.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.advancements.triggers.EnchantedItemTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(EnchantmentMenu.class)
public abstract class CoinEnchantingMixin {
  @WrapMethod(method = "clickMenuButton")
  private boolean sum1rem0d$payment(Player player, int button, Operation<Boolean> original) {
    var menu = (EnchantmentMenu) (Object) this;
    if (CoinPayments.enabled("coin_enchanting") && (!player.isAlive() || player.isSpectator()))
      return false;
    if (CoinPayments.enabled("coin_enchanting")
        && !player.hasInfiniteMaterials()
        && button >= 0
        && button < 3
        && CoinPayments.balance(player) < CoinPayments.cost("coin_enchanting", button + 1))
      return false;
    var transaction = CoinPayments.begin(menu, player, "coin_enchanting");
    if (transaction == null) return original.call(player, button);
    try (transaction) {
      try {
        boolean result = original.call(player, button);
        transaction.finish();
        return result;
      } catch (CoinPayments.PaymentFailure failure) {
        transaction.rollback();
        transaction.rejected(failure);
        return false;
      } catch (RuntimeException failure) {
        transaction.rollback();
        throw failure;
      }
    }
  }

  @Redirect(
      method = "clickMenuButton",
      at =
          @At(
              value = "FIELD",
              target = "Lnet/minecraft/world/entity/player/Player;experienceLevel:I"))
  private int sum1rem0d$level(Player player) {
    if (!CoinPayments.enabled("coin_enchanting")) return player.experienceLevel;
    return Rules.b("coin_enchanting", "require_balance")
        ? CoinPayments.levels(player, "coin_enchanting")
        : Integer.MAX_VALUE;
  }

  @Redirect(
      method = "lambda$clickMenuButton$0",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/entity/player/Player;onEnchantmentPerformed(Lnet/minecraft/world/item/ItemStack;I)V"))
  private void sum1rem0d$charge(Player player, ItemStack stack, int levels) {
    if (CoinPayments.enabled("coin_enchanting") && !player.hasInfiniteMaterials()) {
      CoinPayments.charge(player, "coin_enchanting", levels);
      player.onEnchantmentPerformed(stack, 0);
    } else player.onEnchantmentPerformed(stack, levels);
  }

  @Redirect(
      method = "lambda$clickMenuButton$0",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/advancements/triggers/EnchantedItemTrigger;trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;I)V"))
  private void sum1rem0d$advancement(
      EnchantedItemTrigger trigger, ServerPlayer player, ItemStack item, int levels) {
    if (CoinPayments.enabled("coin_enchanting")) {
      var copy = item.copy();
      CoinPayments.afterCommit(() -> trigger.trigger(player, copy, levels));
    } else trigger.trigger(player, item, levels);
  }
}
