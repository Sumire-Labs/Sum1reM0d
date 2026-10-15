package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.Economy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Runtime placements (including pistons/commands); terrain generation uses ProtoChunk. */
@Mixin(LevelChunk.class)
public abstract class CoinPlacedBlockMixin {
    @Inject(method="setBlockState",at=@At("RETURN"))
    private void sum1rem0d$placed(BlockPos pos,BlockState state,int flags,CallbackInfoReturnable<BlockState> cir) {
        var chunk=(LevelChunk)(Object)this;
        if(cir.getReturnValue()!=null && !cir.getReturnValue().is(state.getBlock()) && chunk.getLevel() instanceof ServerLevel level && com.sumirelabs.sum1rem0d.Sum1reM0d.initialized()) Economy.placed(level,pos,state);
    }
}
