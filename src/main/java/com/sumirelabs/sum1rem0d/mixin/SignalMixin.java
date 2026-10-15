package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.RedstoneGameplay;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class SignalMixin {
    @Inject(method = "getSignal", at = @At("RETURN"), cancellable = true)
    private void sum1rem0d$signal(BlockGetter level, BlockPos pos, Direction side, CallbackInfoReturnable<Integer> cir) { cir.setReturnValue(RedstoneGameplay.signal((BlockState) (Object) this, level, pos, side, cir.getReturnValueI(), false)); }
    @Inject(method = "getDirectSignal", at = @At("RETURN"), cancellable = true)
    private void sum1rem0d$direct(BlockGetter level, BlockPos pos, Direction side, CallbackInfoReturnable<Integer> cir) { cir.setReturnValue(RedstoneGameplay.signal((BlockState) (Object) this, level, pos, side, cir.getReturnValueI(), true)); }
}
