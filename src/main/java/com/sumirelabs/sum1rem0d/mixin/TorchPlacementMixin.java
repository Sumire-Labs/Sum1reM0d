package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.RedstoneGameplay;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(RedstoneTorchBlock.class)
public abstract class TorchPlacementMixin {
    @Inject(method = "onPlace", at = @At("TAIL"))
    private void sum1rem0d$placed(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved, CallbackInfo ci) {
        if (!level.isClientSide() && RedstoneGameplay.part("torches")) level.scheduleTick(pos, state.getBlock(), 2);
    }
}
