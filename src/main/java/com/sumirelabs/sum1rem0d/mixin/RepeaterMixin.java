package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(RepeaterBlock.class)
public abstract class RepeaterMixin {
    @Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
    private void sum1rem0d$locked(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighbor, BlockState neighborState, RandomSource random, CallbackInfoReturnable<BlockState> cir) {
        BlockState next = cir.getReturnValue();
        if (!level.isClientSide() && Rules.on("repeater_bedrock") && !state.getValue(RepeaterBlock.LOCKED) && next.getBlock() instanceof RepeaterBlock && next.getValue(RepeaterBlock.LOCKED)) {
            var id = Rules.id(Rules.s("repeater_bedrock", "replacement"));
            if (id != null) BuiltInRegistries.BLOCK.getOptional(id).ifPresent(block -> cir.setReturnValue(block.defaultBlockState()));
        }
    }
}
