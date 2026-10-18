package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.CrossbowVolley;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public abstract class CrossbowVolleyMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$recoverQueueOnUse(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack crossbow = player.getItemInHand(hand);
        if (CrossbowVolley.releaseOnShiftUse(level, player, crossbow)) {
            if (level.isClientSide()) cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        CrossbowVolley.prepareUse(level, crossbow);
    }

    @Inject(method = "performShooting", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$queueArrowProjectiles(Level level, LivingEntity shooter, InteractionHand hand,
            ItemStack crossbow, float velocity, float inaccuracy, LivingEntity target, CallbackInfo ci) {
        if (CrossbowVolley.intercept(level, shooter, crossbow)) ci.cancel();
    }
}
