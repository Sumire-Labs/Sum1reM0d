package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntityRenderer.class)
public abstract class NaturalFlipRendererMixin {
 @Inject(method="isEntityUpsideDown",at=@At("RETURN"),cancellable=true)
 private void sum1rem0d$flip(LivingEntity entity,CallbackInfoReturnable<Boolean> cir) { if(Rules.on("dinnerbone_mobs") && !entity.hasCustomName() && entity instanceof NaturalFlip flag && flag.sum1rem0d$isFlipped()) cir.setReturnValue(true); }
}
