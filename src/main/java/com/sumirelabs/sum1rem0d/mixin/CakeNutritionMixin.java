package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.FoodNutritionRules;
import net.minecraft.world.level.block.CakeBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(CakeBlock.class)
public abstract class CakeNutritionMixin {
 @ModifyArg(method="eat",at=@At(value="INVOKE",target="Lnet/minecraft/world/food/FoodData;eat(IF)V"),index=0)
 private static int sum1rem0d$cake(int original){return FoodNutritionRules.cake(original);}
}
