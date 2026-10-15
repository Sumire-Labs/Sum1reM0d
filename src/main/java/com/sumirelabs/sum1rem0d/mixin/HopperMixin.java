package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.BlockGameplay;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(HopperBlockEntity.class)
public abstract class HopperMixin {
    @Inject(method = "pushItemsTick", at = @At("HEAD"))
    private static void sum1rem0d$break(Level level, BlockPos pos, BlockState state, HopperBlockEntity entity, CallbackInfo ci) {
        if (level instanceof ServerLevel server) BlockGameplay.hopper(server, pos, state, entity);
    }
}
