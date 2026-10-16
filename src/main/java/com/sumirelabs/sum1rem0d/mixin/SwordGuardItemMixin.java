package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Item.class)
public abstract class SwordGuardItemMixin {
 @Inject(method="use",at=@At("HEAD"),cancellable=true)
 private void sum1rem0d$guard(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> cir) {
  var stack=player.getItemInHand(hand);if(!SwordGuard.canGuard(stack) || !player.isAlive() || player.isSpectator()) return;
  if(hand==InteractionHand.MAIN_HAND && Rules.b("sword_guard","offhand_priority") && !player.getOffhandItem().isEmpty() && player.getOffhandItem().getUseDuration(player)>0) {cir.setReturnValue(InteractionResult.PASS);return;}
  player.startUsingItem(hand);cir.setReturnValue(InteractionResult.CONSUME);
 }
 @Inject(method="getUseAnimation",at=@At("HEAD"),cancellable=true)
 private void sum1rem0d$animation(ItemStack stack,CallbackInfoReturnable<ItemUseAnimation> cir) {if(SwordGuard.canGuard(stack)) cir.setReturnValue(ItemUseAnimation.BLOCK);}
 @Inject(method="getUseDuration",at=@At("HEAD"),cancellable=true)
 private void sum1rem0d$duration(ItemStack stack,LivingEntity entity,CallbackInfoReturnable<Integer> cir) {if(SwordGuard.canGuard(stack)) cir.setReturnValue(72000);}
}
