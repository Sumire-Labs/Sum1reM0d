package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.FoodNutritionRules;
import net.minecraft.world.food.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(FoodProperties.class)
public abstract class FoodNutritionMixin {
 @Redirect(method="onConsume",at=@At(value="INVOKE",target="Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V"))
 private void sum1rem0d$nutrition(FoodData data,FoodProperties food,Level level,LivingEntity consumer,ItemStack stack,Consumable consumable){data.eat(FoodNutritionRules.modify(stack,food));}
}
