package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.RedstoneGameplay;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin({RedstoneTorchBlock.class, RedstoneWallTorchBlock.class})
public abstract class TorchMixin {
    @Inject(method = "hasNeighborSignal", at = @At("RETURN"), cancellable = true)
    private void sum1rem0d$torch(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (RedstoneGameplay.part("torches")) cir.setReturnValue(!cir.getReturnValueZ());
    }
}
