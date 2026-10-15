package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.RedstoneGameplay;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(RedstoneWireBlock.class)
public abstract class WireMixin {
    @Inject(method = "updatePowerStrength", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$wire(Level level, BlockPos pos, BlockState state, Orientation orientation, boolean shape, CallbackInfo ci) {
        if (RedstoneGameplay.part("wire")) { RedstoneGameplay.wire(level, pos); ci.cancel(); }
    }
}
