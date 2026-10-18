package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.ShulkerBoxColorAccess;

import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerBoxBlockEntity.class)
public abstract class ShulkerBoxColorMixin implements ShulkerBoxColorAccess {
    @Unique private int sum1rem0d$mixedRgb = -1;

    @Override public int sum1rem0d$mixedRgb() { return sum1rem0d$mixedRgb; }
    @Override public void sum1rem0d$mixedRgb(int rgb) {
        int next = rgb < 0 ? -1 : rgb & 0xFFFFFF;
        if (sum1rem0d$mixedRgb == next) return;
        sum1rem0d$mixedRgb = next;
        var blockEntity = (ShulkerBoxBlockEntity) (Object) this;
        blockEntity.setChanged();
        if (blockEntity.getLevel() != null)
            blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void sum1rem0d$saveColor(ValueOutput output, CallbackInfo ci) {
        if (sum1rem0d$mixedRgb >= 0) output.putInt("sum1rem0d_mixed_rgb", sum1rem0d$mixedRgb);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void sum1rem0d$loadColor(ValueInput input, CallbackInfo ci) {
        sum1rem0d$mixedRgb = input.getIntOr("sum1rem0d_mixed_rgb", -1);
        if (sum1rem0d$mixedRgb > 0xFFFFFF) sum1rem0d$mixedRgb = -1;
    }

}
