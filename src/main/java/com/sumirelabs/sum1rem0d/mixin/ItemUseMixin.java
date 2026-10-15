package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.Gameplay;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ItemStack.class)
public abstract class ItemUseMixin {
    @Unique private ItemStack sum1rem0d$eaten;
    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void sum1rem0d$beforeUse(Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) { sum1rem0d$eaten = ((ItemStack) (Object) this).copy(); }
    @Inject(method = "finishUsingItem", at = @At("RETURN"))
    private void sum1rem0d$afterUse(Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        if (entity instanceof ServerPlayer player && sum1rem0d$eaten != null) Gameplay.ate(player, sum1rem0d$eaten);
        sum1rem0d$eaten = null;
    }
}
