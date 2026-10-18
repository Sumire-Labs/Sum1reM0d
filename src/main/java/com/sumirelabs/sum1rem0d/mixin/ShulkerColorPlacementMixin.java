package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.ShulkerBoxColorAccess;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class ShulkerColorPlacementMixin {
    @Inject(method = "applyComponentsFromItemStack", at = @At("TAIL"))
    private void sum1rem0d$applyShulkerRgb(ItemStack stack, CallbackInfo ci) {
        if (!((Object) this instanceof ShulkerBoxBlockEntity)) return;
        var access = (ShulkerBoxColorAccess) this;
        DyedItemColor color = stack.get(DataComponents.DYED_COLOR);
        access.sum1rem0d$mixedRgb(color == null ? -1 : color.rgb());
        ((BlockEntity) (Object) this).setChanged();
    }

    @Inject(method = "collectImplicitComponents", at = @At("TAIL"))
    private void sum1rem0d$collectShulkerRgb(DataComponentMap.Builder builder, CallbackInfo ci) {
        if (!((Object) this instanceof ShulkerBoxColorAccess access)) return;
        int color = access.sum1rem0d$mixedRgb();
        if (color >= 0) builder.set(DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(color));
    }
}
