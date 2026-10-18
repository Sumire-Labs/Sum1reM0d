package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.StackableFluids;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class StackableFluidsNeoMixin {
    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true, require = 0)
    private void sum1rem0d$stackableFluids(CallbackInfoReturnable<Integer> cir) {
        int override = StackableFluids.overrideMaxStackSize((ItemInstance) (Object) this);
        if (override > 0) cir.setReturnValue(override);
    }
}
