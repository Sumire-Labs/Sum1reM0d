package com.sumirelabs.sum1rem0d.game;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;

/** Consumption-time changes preserve vanilla effects, sounds and containers. */
public final class FoodNutritionRules {
 private static final String[] FEATURES={"beetroot_soup_boost","cooked_meat_penalty","staple_food_penalty","sweet_food_boost"};
 private FoodNutritionRules(){}
 public static double multiplier(ItemStack stack){
  for(String feature:FEATURES)if(Rules.on(feature)&&Rules.matches(stack,Rules.list(feature,"target_items")))return Rules.n(feature,"multiplier");
  return 1;
 }
 public static FoodProperties modify(ItemStack stack,FoodProperties original){
  double scale=multiplier(stack);if(scale==1)return original;
  return new FoodProperties((int)Math.clamp(Math.floor(original.nutrition()*scale),0,Integer.MAX_VALUE),Math.max(0,(float)(original.saturation()*scale)),original.canAlwaysEat());
 }
 public static int cake(int original){return (int)Math.max(0,Math.floor(original*multiplier(new ItemStack(Items.CAKE))));}
}
