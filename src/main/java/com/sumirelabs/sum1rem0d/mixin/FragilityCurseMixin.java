package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.FragilityCurse;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemStack.class)
public abstract class FragilityCurseMixin {
    //? if neoforge {
    /*@Inject(method="processDurabilityChange(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)I",at=@At("RETURN"),cancellable=true)
    private void sum1rem0d$tripleNeoWear(int amount, ServerLevel level, net.minecraft.world.entity.LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(FragilityCurse.apply((ItemStack)(Object)this, level, cir.getReturnValueI()));
    }
    *///?} else {
    @Inject(method="processDurabilityChange(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;)I",at=@At("RETURN"),cancellable=true)
    private void sum1rem0d$tripleWear(int amount, ServerLevel level, ServerPlayer player, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(FragilityCurse.apply((ItemStack)(Object)this, level, cir.getReturnValueI()));
    }
    //?}
}
