package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.AutomationProcessing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tracks the native rod so the server can test the heavy-core pedestal and redstone signal. */
@Mixin(LightningRodBlock.class)
public abstract class LightningRodTeslaMixin {
    @Inject(method="onPlace",at=@At("TAIL"))
    private void sum1rem0d$placed(BlockState state,Level level,BlockPos pos,BlockState oldState,boolean moved,CallbackInfo callback){if(level instanceof ServerLevel server)AutomationProcessing.trackTeslaRod(server,pos);}
    @Inject(method="updateShape",at=@At("RETURN"))
    private void sum1rem0d$track(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,Direction direction,BlockPos neighborPos,BlockState neighborState,RandomSource random,CallbackInfoReturnable<BlockState> callback){if(level instanceof ServerLevel server)AutomationProcessing.trackTeslaRod(server,pos);}
}
