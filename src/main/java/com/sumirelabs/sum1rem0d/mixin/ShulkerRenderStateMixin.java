package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.ShulkerRenderStateAccess;

import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ShulkerBoxRenderState.class)
public abstract class ShulkerRenderStateMixin implements ShulkerRenderStateAccess {
    @Unique private int sum1rem0d$mixedRgb = -1;
    @Override public int sum1rem0d$mixedRgb() { return sum1rem0d$mixedRgb; }
    @Override public void sum1rem0d$mixedRgb(int rgb) { sum1rem0d$mixedRgb = rgb; }
}
