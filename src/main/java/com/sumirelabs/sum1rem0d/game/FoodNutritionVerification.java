package com.sumirelabs.sum1rem0d.game;

import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;

final class FoodNutritionVerification {
 static void verify(MinecraftServer server,Consumer<String> passed)throws Exception{
  var config=Sum1reM0d.config();var before=config.snapshot();var player=EconomyVerification.actor(server,server.overworld(),"SumireNutrition");
  try{
   var draft=before.deepCopy();var features=draft.getAsJsonObject("features");features.getAsJsonObject("vegan").addProperty("enabled",false);
   for(String id:List.of("beetroot_soup_boost","cooked_meat_penalty","staple_food_penalty","sweet_food_boost"))features.getAsJsonObject(id).addProperty("enabled",true);
   config.update(draft,config.revision());
   String[] ids={"beetroot_soup","cooked_beef","carrot","bread","cookie","rabbit_stew"};double[] factors={10,.5,.5,.5,1.5,1.5};
   for(int i=0;i<ids.length;i++){
    var stack=new ItemStack(BuiltInRegistries.ITEM.getValue(Rules.id("minecraft:"+ids[i])));var food=stack.get(DataComponents.FOOD);var original=food;
    player.getFoodData().setFoodLevel(0);player.getFoodData().setSaturation(0);var remainder=stack.finishUsingItem(player.level(),player);
    int expected=(int)Math.min(20,Math.floor(food.nutrition()*factors[i]));require(player.getFoodData().getFoodLevel()==expected,"Native consumption nutrition "+ids[i]);
    require(Math.abs(player.getFoodData().getSaturationLevel()-Math.min(expected,food.saturation()*factors[i]))<0.001,"Native saturation "+ids[i]);
    require(BuiltInRegistries.ITEM.getValue(Rules.id("minecraft:"+ids[i])).getDefaultInstance().get(DataComponents.FOOD).equals(original),"Food definition remains immutable "+ids[i]);
    if(ids[i].endsWith("soup")||ids[i].endsWith("stew"))require(remainder.is(Items.BOWL),"Native soup bowl remains "+ids[i]);
   }
   var pos=new BlockPos(1930,240,40);ServerVerification.place(player.level(),pos,Blocks.CAKE.defaultBlockState());player.getFoodData().setFoodLevel(0);player.getFoodData().setSaturation(0);
   var eat=CakeBlock.class.getDeclaredMethod("eat",net.minecraft.world.level.LevelAccessor.class,BlockPos.class,net.minecraft.world.level.block.state.BlockState.class,net.minecraft.world.entity.player.Player.class);eat.setAccessible(true);eat.invoke(null,player.level(),pos,player.level().getBlockState(pos),player);
   require(player.getFoodData().getFoodLevel()==3,"Native cake slice applies 1.5x nutrition");
   features.getAsJsonObject("beetroot_soup_boost").addProperty("enabled",false);config.update(draft,config.revision());player.getFoodData().setFoodLevel(0);new ItemStack(BuiltInRegistries.ITEM.getValue(Rules.id("minecraft:beetroot_soup"))).finishUsingItem(player.level(),player);
   require(player.getFoodData().getFoodLevel()==6,"Live Config OFF restores vanilla beetroot soup");
   passed.accept("Native food and cake consumption applies independent nutrition/saturation multipliers, preserves bowls/default components and live OFF");
  }finally{config.update(before,config.revision());player.discard();}
 }
}
