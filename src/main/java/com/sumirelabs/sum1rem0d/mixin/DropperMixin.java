package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(DropperBlock.class)
public abstract class DropperMixin {
    @Inject(method = "dispenseFrom", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$break(ServerLevel level, BlockState state, BlockPos pos, CallbackInfo ci) {
        if (Rules.on("dropper_breaker") && BlockGameplay.dropper(level, pos, state)) ci.cancel();
    }
}
