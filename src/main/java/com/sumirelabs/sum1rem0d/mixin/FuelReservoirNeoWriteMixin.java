package com.sumirelabs.sum1rem0d.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
//? if neoforge {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.At;
*///?}

/** The NeoForge transactional setter's native stack clamp must not truncate saved fuel counts. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FuelReservoirNeoWriteMixin {
    //? if neoforge {
    /*@WrapOperation(method="setItem(ILnet/minecraft/world/item/ItemStack;Z)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;limitSize(I)V"))
    private void sum1rem0d$preserveLargeFuel(ItemStack stack,int nativeLimit,Operation<Void> original,@Local(argsOnly=true) int slot){
        if(slot==1&&stack.getCount()>stack.getMaxStackSize())return;
        original.call(stack,nativeLimit);
    }
    *///?}
}
