package com.sumirelabs.sum1rem0d.mixin;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Slot.class)
public abstract class FuelReservoirSlotMixin {
 @ModifyVariable(method="remove",at=@At("HEAD"),argsOnly=true)
 private int sum1rem0d$normalWithdrawal(int count){var slot=(Slot)(Object)this;return slot.container instanceof AbstractFurnaceBlockEntity&&slot.getContainerSlot()==1?Math.min(count,slot.getItem().getMaxStackSize()):count;}
}
