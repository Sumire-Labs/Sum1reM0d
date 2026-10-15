package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.BlockGameplay;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class GrowthMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$growth(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (BlockGameplay.growthSleeps(level, (BlockState) (Object) this)) ci.cancel();
    }
}
