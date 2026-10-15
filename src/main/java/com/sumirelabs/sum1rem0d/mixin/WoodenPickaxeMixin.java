package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.config.ClientRules;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class WoodenPickaxeMixin {
    @Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$durability(CallbackInfoReturnable<Integer> callback) {
        if (Rules.on("wooden_pickaxe") && Rules.matches((ItemStack) (Object) this, Rules.list("wooden_pickaxe", "target_items")))
            callback.setReturnValue(Rules.i("wooden_pickaxe", "durability"));
    }
}
