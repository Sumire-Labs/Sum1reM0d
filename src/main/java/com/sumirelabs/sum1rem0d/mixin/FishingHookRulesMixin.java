package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.FishingRodRules;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hooks only the native fishing bobber lifecycle so normal fishing remains server-owned. */
@Mixin(FishingHook.class)
public abstract class FishingHookRulesMixin {
    @Inject(method="<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;II)V",at=@At("TAIL"))
    private void sum1rem0d$longCast(Player player,Level level,int luck,int lure,CallbackInfo callback){FishingRodRules.longCast((FishingHook)(Object)this);}

    //? if >=26.3 {
    @Inject(method="getDefaultGravity",at=@At("RETURN"),cancellable=true)
    private void sum1rem0d$castGravity(CallbackInfoReturnable<Double> callback){callback.setReturnValue(FishingRodRules.gravity((FishingHook)(Object)this,callback.getReturnValue()));}
    //?} else {
    /*@ModifyConstant(method="tick",constant=@Constant(doubleValue=-0.03D))
    private double sum1rem0d$legacyCastGravity(double gravity){return -FishingRodRules.gravity((FishingHook)(Object)this,-gravity);}
    *///?}

    @Inject(method="tick",at=@At("TAIL"))
    private void sum1rem0d$lavaBite(CallbackInfo callback){FishingRodRules.tick((FishingHook)(Object)this);}

    @Inject(method="onHitBlock",at=@At("TAIL"))
    private void sum1rem0d$rememberAnchor(BlockHitResult hit,CallbackInfo callback){FishingRodRules.blockHit((FishingHook)(Object)this,hit);}

    @Inject(method="retrieve",at=@At("HEAD"),cancellable=true)
    private void sum1rem0d$grappleOrLavaLoot(ItemStack rod,CallbackInfoReturnable<Integer> callback){Integer result=FishingRodRules.retrieve((FishingHook)(Object)this,rod);if(result!=null)callback.setReturnValue(result);}

    @Inject(method="remove",at=@At("HEAD"))
    private void sum1rem0d$clearFishingState(net.minecraft.world.entity.Entity.RemovalReason reason,CallbackInfo callback){FishingRodRules.removed((FishingHook)(Object)this);}
}
