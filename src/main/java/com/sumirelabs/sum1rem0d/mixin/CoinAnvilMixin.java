package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.CoinPayments;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class CoinAnvilMixin {
  @Shadow
  public abstract int getCost();

  @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
  private void sum1rem0d$afford(
      Player player, boolean hasItem, CallbackInfoReturnable<Boolean> cir) {
    if (CoinPayments.enabled("coin_anvil"))
      cir.setReturnValue(CoinPayments.canAnvil(player, getCost()));
  }

  @Redirect(
      method = "onTake",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/world/entity/player/Player;giveExperienceLevels(I)V"))
  private void sum1rem0d$charge(Player player, int levels) {
    if (CoinPayments.enabled("coin_anvil")) CoinPayments.charge(player, "coin_anvil", -levels);
    else player.giveExperienceLevels(levels);
  }

  @Redirect(
      method = "onTake",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/inventory/ContainerLevelAccess;execute(Ljava/util/function/BiConsumer;)V"))
  private void sum1rem0d$worldEffects(
      ContainerLevelAccess access, BiConsumer<Level, BlockPos> action) {
    if (CoinPayments.enabled("coin_anvil")) CoinPayments.afterCommit(() -> access.execute(action));
    else access.execute(action);
  }
}
