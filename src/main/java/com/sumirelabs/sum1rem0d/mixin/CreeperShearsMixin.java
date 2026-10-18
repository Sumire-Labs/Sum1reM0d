package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.WorldEntityRules;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class CreeperShearsMixin {
    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$collectCreeperGunpowder(Entity target, InteractionHand hand, Vec3 hit,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (WorldEntityRules.shearCreeper((Player) (Object) this, target)) cir.setReturnValue(InteractionResult.CONSUME);
    }
}
