package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ItemStack.class)
public abstract class OreItemNameMixin {
    @Inject(method = "getHoverName", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$oreName(CallbackInfoReturnable<Component> cir) {
        if (!ClientGameState.daily("diamond_disguise") || !ClientRules.bool("diamond_disguise", "items")) return;
        if (((ItemStack) (Object) this).getItem() instanceof BlockItem block && Rules.matches(block.getBlock().defaultBlockState(), ClientRules.list("diamond_disguise", "target_blocks"))) {
            var id = Rules.id(ClientRules.text("diamond_disguise", "replacement"));
            if (id != null) BuiltInRegistries.BLOCK.getOptional(id).ifPresent(replacement -> cir.setReturnValue(replacement.getName()));
        }
    }
}
