package com.sumirelabs.sum1rem0d.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.sumirelabs.sum1rem0d.game.CoinPayments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractContainerMenu.class)
public abstract class CoinAnvilClickMixin {
  @WrapMethod(method = "clicked")
  private void sum1rem0d$payment(
      int slot, int button, ContainerInput input, Player player, Operation<Void> original) {
    var menu = (AbstractContainerMenu) (Object) this;
    var transaction =
        menu instanceof AnvilMenu ? CoinPayments.begin(menu, player, "coin_anvil") : null;
    if (transaction == null) {
      original.call(slot, button, input, player);
      return;
    }
    try (transaction) {
      try {
        original.call(slot, button, input, player);
        transaction.finish();
      } catch (CoinPayments.PaymentFailure failure) {
        transaction.rollback();
        transaction.rejected(failure);
      } catch (RuntimeException failure) {
        transaction.rollback();
        throw failure;
      }
    }
  }
}
