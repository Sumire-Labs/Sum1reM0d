package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ComparatorBlock.class)
public abstract class ComparatorMixin extends DiodeBlock {
    protected ComparatorMixin(net.minecraft.world.level.block.state.BlockBehaviour.Properties p) { super(p); }
    @Shadow protected abstract int getInputSignal(Level level, BlockPos pos, BlockState state);
    @Unique private int sum1rem0d$divide(Level level, BlockPos pos, BlockState state) {
        int numerator = getInputSignal(level, pos, state), denominator = getAlternateSignal(level, pos, state);
        if (denominator == 0) return Rules.b("comparator_division", "zero_passthrough") ? numerator : 0;
        return Math.min(15, Rules.b("comparator_division", "round_up") ? (int) Math.ceil((double) numerator / denominator) : numerator / denominator);
    }
    @Inject(method = "calculateOutputSignal", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$output(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<Integer> cir) {
        if (Rules.on("comparator_division") && state.getValue(ComparatorBlock.MODE) == ComparatorMode.SUBTRACT) cir.setReturnValue(sum1rem0d$divide(level, pos, state));
    }
    @Inject(method = "shouldTurnOn", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$powered(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (Rules.on("comparator_division") && state.getValue(ComparatorBlock.MODE) == ComparatorMode.SUBTRACT) cir.setReturnValue(sum1rem0d$divide(level, pos, state) > 0);
    }
}
