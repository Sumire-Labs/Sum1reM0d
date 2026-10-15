package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.client.VisualBlocks;
import net.minecraft.client.renderer.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin({BlockStateModelSet.class, BlockModelSet.class})
public abstract class BlockModelsMixin {
    @ModifyVariable(method = "get", at = @At("HEAD"), argsOnly = true)
    private BlockState sum1rem0d$model(BlockState state) { return VisualBlocks.replace(state); }
}
