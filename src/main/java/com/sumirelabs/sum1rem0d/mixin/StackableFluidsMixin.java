package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.StackableFluids;
import net.minecraft.world.item.ItemInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInstance.class)
public interface StackableFluidsMixin {
    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void sum1rem0d$stackableFluids(CallbackInfoReturnable<Integer> cir) {
        ItemInstance instance = (ItemInstance) (Object) this;
        int override = StackableFluids.overrideMaxStackSize(instance);
        if (override > 0) cir.setReturnValue(override);
    }
}
