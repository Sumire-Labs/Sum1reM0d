package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemStack.class)
public abstract class NetherrackMiningMixin {
 @Inject(method="getDestroySpeed", at=@At("HEAD"), cancellable=true)
 private void sum1rem0d$speed(BlockState state, CallbackInfoReturnable<Float> cir) { if(state.is(Blocks.NETHERRACK) && Rules.on("netherrack_shears")) cir.setReturnValue((float)Rules.n("netherrack_shears", ((ItemStack)(Object)this).is(Items.SHEARS)?"shears_speed":"other_speed")); }
 @Inject(method="isCorrectToolForDrops", at=@At("HEAD"), cancellable=true)
 private void sum1rem0d$tool(BlockState state, CallbackInfoReturnable<Boolean> cir) { if(state.is(Blocks.NETHERRACK) && Rules.on("netherrack_shears")) cir.setReturnValue(((ItemStack)(Object)this).is(Items.SHEARS)); }
}
