package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.*;
import com.sumirelabs.sum1rem0d.config.ClientGameState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class HardnessMixin {
    @Unique private static final ThreadLocal<Boolean> sum1rem0d$query = ThreadLocal.withInitial(() -> false);
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void sum1rem0d$hardness(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (sum1rem0d$query.get() || !Rules.on("hardness_swap")) return;
        boolean active = level instanceof ServerLevel server ? WorldState.get(server.getServer()).daily("hardness_swap") : ClientGameState.daily("hardness_swap");
        if (!active) return;
        String current = BuiltInRegistries.BLOCK.getKey(((BlockState) (Object) this).getBlock()).toString();
        String first = Rules.s("hardness_swap", "first_block"), second = Rules.s("hardness_swap", "second_block");
        var id = Rules.id(current.equals(first) ? second : current.equals(second) ? first : "");
        if (id == null) return;
        var replacement = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        if (replacement != null) { sum1rem0d$query.set(true); try { cir.setReturnValue(replacement.defaultBlockState().getDestroySpeed(level, pos)); } finally { sum1rem0d$query.set(false); } }
    }
}
