package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemStack.class)
public abstract class ElytraEnchantableItemMixin {
    @Inject(method="isEnchantable",at=@At("HEAD"),cancellable=true)
    private void sum1rem0d$elytraGate(CallbackInfoReturnable<Boolean> cir) {
        var stack=(ItemStack)(Object)this;
        if(stack.is(Items.ELYTRA)) cir.setReturnValue(Rules.on("elytra_enchanting")&&!stack.isEnchanted());
    }
}
