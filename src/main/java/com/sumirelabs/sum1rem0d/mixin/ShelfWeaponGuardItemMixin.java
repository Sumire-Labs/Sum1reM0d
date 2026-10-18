package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.CombatItemRules;
import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ShelfWeaponGuardItemMixin {
    @Unique private static boolean sum1rem0d$canGuard(ItemStack stack) {
        return Rules.on("shelf_weapon") && Rules.b("shelf_weapon", "guard_enabled") && CombatItemRules.isShelf(stack);
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$startGuard(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (sum1rem0d$canGuard(player.getItemInHand(hand)) && player.isAlive() && !player.isSpectator()) {
            player.startUsingItem(hand);
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$guardAnimation(ItemStack stack, CallbackInfoReturnable<ItemUseAnimation> cir) {
        if (sum1rem0d$canGuard(stack)) cir.setReturnValue(ItemUseAnimation.BLOCK);
    }

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$guardDuration(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (sum1rem0d$canGuard(stack)) cir.setReturnValue(72000);
    }
}
