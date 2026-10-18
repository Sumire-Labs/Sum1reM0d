package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.FuelReservoir;
import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.world.Container;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if neoforge {
/*import net.neoforged.neoforge.transfer.item.ItemResource;
*///?}

/** NeoForge transfer bridge: the handler capacity drives menu slots, transfers and hopper wrappers. */
@Pseudo
@Mixin(targets="net.neoforged.neoforge.transfer.item.VanillaContainerWrapper$SlotWrapper",remap=false)
public abstract class FuelReservoirNeoCapacityMixin {
    @Shadow @Final private int index;
    //? if neoforge {
    /*@Shadow @Final private net.neoforged.neoforge.transfer.item.VanillaContainerWrapper this$0;
    @Inject(method="getCapacity",at=@At("RETURN"),cancellable=true,remap=false)
    private void sum1rem0d$fuelCapacity(ItemResource resource,CallbackInfoReturnable<Integer> callback){
        Container container=((NeoVanillaContainerAccess)(Object)this$0).sum1rem0d$getContainer();
        if(index!=1||!(container instanceof AbstractFurnaceBlockEntity)||!Rules.on("fuel_stacks"))return;
        if(!resource.isEmpty()&&resource.is(Items.BUCKET))return;
        callback.setReturnValue(resource.isEmpty()?Rules.i("fuel_stacks","maximum"):FuelReservoir.capacity(resource.toStack()));
    }
    *///?}
}
