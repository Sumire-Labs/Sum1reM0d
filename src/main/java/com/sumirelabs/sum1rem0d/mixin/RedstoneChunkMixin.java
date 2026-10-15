package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.RedstoneRefresh;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LevelChunk.class)
public abstract class RedstoneChunkMixin {
    @Shadow public abstract Level getLevel();
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void sum1rem0d$source(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        if (getLevel() instanceof ServerLevel level && cir.getReturnValue() != null) RedstoneRefresh.changed(level, pos, cir.getReturnValue(), state);
    }
}
