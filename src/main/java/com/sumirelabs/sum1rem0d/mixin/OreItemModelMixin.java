package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ItemModelResolver.class)
public abstract class OreItemModelMixin {
    @ModifyVariable(method = "appendItemLayers", at = @At("HEAD"), argsOnly = true)
    private ItemStack sum1rem0d$oreModel(ItemStack stack) {
        if (!ClientGameState.daily("diamond_disguise") || !ClientRules.bool("diamond_disguise", "items")) return stack;
        if (stack.getItem() instanceof BlockItem block && Rules.matches(block.getBlock().defaultBlockState(), ClientRules.list("diamond_disguise", "target_blocks"))) {
            var id = Rules.id(ClientRules.text("diamond_disguise", "replacement"));
            if (id != null) return BuiltInRegistries.BLOCK.getOptional(id).orElse(block.getBlock()).asItem().getDefaultInstance().copyWithCount(stack.getCount());
        }
        return stack;
    }
}
