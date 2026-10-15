package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(BoneMealItem.class)
public abstract class BonemealMixin {
    @Inject(method = "growCrop", at = @At("HEAD"), cancellable = true)
    private static void sum1rem0d$bonemeal(ItemStack stack, Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof ServerLevel server && Rules.b("growth_sleep", "bonemeal") && BlockGameplay.growthSleeps(server, level.getBlockState(pos))) cir.setReturnValue(false);
    }
}
