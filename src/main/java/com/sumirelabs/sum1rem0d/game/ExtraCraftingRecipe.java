package com.sumirelabs.sum1rem0d.game;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
public final class ExtraCraftingRecipe extends CustomRecipe {
 public static final RecipeSerializer<ExtraCraftingRecipe> SERIALIZER=new RecipeSerializer<>(Codec.STRING.fieldOf("feature").xmap(ExtraCraftingRecipe::new,r->r.feature),StreamCodec.ofMember((r,b)->b.writeUtf(r.feature,64),b->new ExtraCraftingRecipe(b.readUtf(64))));
 private final String feature;
 public ExtraCraftingRecipe(String feature){if(!feature.equals("piston_base") && !feature.equals("tamed_book") && !feature.equals("mixed_wool") && !feature.equals("mixed_bed") && !feature.equals("mixed_wool_slab") && !feature.equals("mixed_wool_stairs") && !ExtraEquipment.ITEMS.contains(feature))throw new IllegalArgumentException(feature);this.feature=feature;}
 @Override public boolean matches(CraftingInput input,Level level){return !assemble(input).isEmpty();}
 @Override public ItemStack assemble(CraftingInput input){
  if(ExtraEquipment.ITEMS.contains(feature))return ExtraEquipment.craft(feature,input);
  if(!Rules.on(feature))return ItemStack.EMPTY;
  if(feature.equals("mixed_wool"))return DyeTextiles.dye(input,feature);
  if(feature.equals("mixed_bed"))return DyeTextiles.bed(input);
  if(feature.equals("mixed_wool_slab")||feature.equals("mixed_wool_stairs"))return DyeTextiles.woolShape(input,feature);
  if(feature.equals("tamed_book")){
   ItemStack book=ItemStack.EMPTY;int sugar=0;
   for(var stack:input.items())if(!stack.isEmpty()){if(stack.is(Items.WRITABLE_BOOK) && book.isEmpty())book=stack;else if(stack.is(Items.SUGAR))sugar++;else return ItemStack.EMPTY;}
   return !book.isEmpty() && sugar==1?book.transmuteCopy(ExtraContent.item("tamed_book"),1):ItemStack.EMPTY;
  }
  if(!Rules.b(feature,"recipe_enabled") || input.width()!=3 || input.height()!=2)return ItemStack.EMPTY;
  Item[] pattern={Items.COBBLESTONE,Items.IRON_INGOT,Items.COBBLESTONE,Items.COBBLESTONE,Items.REDSTONE,Items.COBBLESTONE};
  for(int i=0;i<6;i++)if(!input.getItem(i).is(pattern[i]))return ItemStack.EMPTY;
  return new ItemStack(ExtraContent.item("piston_base"));
 }
 @Override public RecipeSerializer<? extends CustomRecipe> getSerializer(){return SERIALIZER;}
}
