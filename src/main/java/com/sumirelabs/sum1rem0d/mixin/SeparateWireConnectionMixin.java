package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.GlowWireBlock;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(RedstoneWireBlock.class)
public abstract class SeparateWireConnectionMixin {
 //? if >=26.3 {
 @Inject(method="shouldConnectTo(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z",at=@At("HEAD"),cancellable=true)
 private static void sum1rem0d$separate(BlockState state,BlockGetter level,BlockPos pos,Direction side,CallbackInfoReturnable<Boolean> cir){if(state.getBlock() instanceof GlowWireBlock)cir.setReturnValue(false);}
 //?} else {
 /*@Inject(method="shouldConnectTo(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z",at=@At("HEAD"),cancellable=true)
 private static void sum1rem0d$separate(BlockState state,Direction side,CallbackInfoReturnable<Boolean> cir){if(state.getBlock() instanceof GlowWireBlock)cir.setReturnValue(false);}
 *///?}
}
