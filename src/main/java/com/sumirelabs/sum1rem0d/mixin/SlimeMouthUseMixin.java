package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.ModContent;
import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class SlimeMouthUseMixin {
    @Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$slimeBall(ItemStack stack, Player user, LivingEntity target, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (!Rules.on("slime_mouth") || !stack.is(Items.SLIME_BALL) || !(target instanceof ServerPlayer victim)) return;
        if (!user.level().isClientSide()) {
            int ticks = Math.max(20, Math.min(72000, Rules.i("slime_mouth", "duration_seconds") * 20));
            victim.addEffect(new MobEffectInstance(ModContent.effect("slime_mouth"), ticks), user);
            if (!user.getAbilities().instabuild) stack.shrink(1);
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
