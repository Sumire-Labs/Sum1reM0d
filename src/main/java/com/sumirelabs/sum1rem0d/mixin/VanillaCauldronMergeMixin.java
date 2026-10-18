package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.LargeCauldronBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Joins ordinary vanilla cauldrons as soon as a complete 2x1/2x2 footprint is placed. */
@Mixin(BlockBehaviour.class)
public abstract class VanillaCauldronMergeMixin {
    @Inject(method = "onPlace", at = @At("TAIL"))
    private void sum1rem0d$mergeCauldrons(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved, CallbackInfo ci) {
        if (!moved && state.getBlock() instanceof AbstractCauldronBlock) LargeCauldronBlock.tryMergeVanilla(level, pos, state);
    }
}
