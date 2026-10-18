package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.FoodContent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class CatMeatDropMixin {
    @Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
    private void sum1rem0d$dropCatMeat(ServerLevel level, DamageSource source, CallbackInfo ci) {
        if (!((Object) this instanceof Cat cat)) return;
        int count = FoodContent.catMeatDropCount(cat.getRandom());
        if (count > 0) cat.spawnAtLocation(level, new ItemStack(FoodContent.item("cat_meat"), count));
    }
}
