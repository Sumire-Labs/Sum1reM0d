package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.WorldSurvivalRules;
import org.spongepowered.asm.mixin.*;
//? if >=26.2 {
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
//?}
/** Hooks actual noxious-water/jet contact rather than nearby decorative sulfur. */
@Pseudo
@Mixin(targets="net.minecraft.world.level.block.entity.PotentSulfurBlockEntity")
public abstract class SulfurContactMixin {
 //? if >=26.2 {
 @Inject(method="applyNauseaEffect",at=@At("HEAD"))
 private static void sum1rem0d$water(LivingEntity entity,CallbackInfo ci){WorldSurvivalRules.sulfurContact(entity,false);}
 @WrapOperation(method="lambda$static$5",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;addDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
 private static void sum1rem0d$jet(Entity entity,Vec3 delta,Operation<Void> original){WorldSurvivalRules.sulfurContact(entity,true);original.call(entity,delta);}
 //?}
}
