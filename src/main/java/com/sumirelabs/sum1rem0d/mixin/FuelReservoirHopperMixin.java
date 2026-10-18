package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.core.Direction;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(HopperBlockEntity.class)
public abstract class FuelReservoirHopperMixin {
 @Inject(method="tryMoveInItem",at=@At("HEAD"),cancellable=true)
 private static void sum1rem0d$insert(Container from,Container to,ItemStack stack,int slot,Direction side,CallbackInfoReturnable<ItemStack> cir){
  if(!(to instanceof AbstractFurnaceBlockEntity)||slot!=1||!Rules.on("fuel_stacks")||!to.canPlaceItem(slot,stack))return;
  if(side!=null&&to instanceof WorldlyContainer sided&&!sided.canPlaceItemThroughFace(slot,stack,side))return;
  FuelReservoir.insert(to,stack,stack.getCount());cir.setReturnValue(stack);
 }
}
