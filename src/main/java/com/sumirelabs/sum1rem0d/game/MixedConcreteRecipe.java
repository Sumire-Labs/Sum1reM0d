package com.sumirelabs.sum1rem0d.game;
import com.mojang.serialization.MapCodec;
import java.util.*;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
public final class MixedConcreteRecipe extends CustomRecipe {
 public static final RecipeSerializer<MixedConcreteRecipe> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(MixedConcreteRecipe::new),StreamCodec.unit(new MixedConcreteRecipe()));
 @Override public boolean matches(CraftingInput input,Level level){return !assemble(input).isEmpty();}
 @Override public ItemStack assemble(CraftingInput input){
  if(!Rules.on("mixed_concrete"))return ItemStack.EMPTY;
  ItemStack concrete=ItemStack.EMPTY;var dyes=new ArrayList<DyeColor>();
  for(var stack:input.items())if(!stack.isEmpty()){
   if(MixedConcrete.isConcrete(stack)){if(!concrete.isEmpty())return ItemStack.EMPTY;concrete=stack;}
   else if(stack.get(net.minecraft.core.component.DataComponents.DYE)!=null)dyes.add(stack.get(net.minecraft.core.component.DataComponents.DYE));else return ItemStack.EMPTY;
  }
  if(concrete.isEmpty() || dyes.isEmpty() || dyes.size()>Rules.i("mixed_concrete","maximum_dyes"))return ItemStack.EMPTY;
  return MixedConcrete.stack(DyedItemColor.applyDyes(MixedConcrete.color(concrete),dyes).rgb());
 }
 @Override public RecipeSerializer<? extends CustomRecipe> getSerializer(){return SERIALIZER;}
}
