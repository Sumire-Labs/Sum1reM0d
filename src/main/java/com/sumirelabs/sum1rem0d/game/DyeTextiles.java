package com.sumirelabs.sum1rem0d.game;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
public final class DyeTextiles {
 private DyeTextiles(){}
 public static boolean kind(ItemStack stack,String kind){if(stack.is(ExtraContent.item("mixed_"+kind)))return true;var id=BuiltInRegistries.ITEM.getKey(stack.getItem());return id.getNamespace().equals("minecraft") && Arrays.stream(DyeColor.values()).anyMatch(c->id.getPath().equals(c.getName()+"_"+kind));}
 public static int color(ItemStack stack){var explicit=stack.get(DataComponents.DYED_COLOR);if(explicit!=null)return explicit.rgb()&0xFFFFFF;String path=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();for(var c:DyeColor.values())if(path.equals(c.getName()+"_wool") || path.equals(c.getName()+"_bed"))return c.getTextureDiffuseColor()&0xFFFFFF;return 0xFFFFFF;}
 public static ItemStack stack(String name,int rgb){var s=new ItemStack(ExtraContent.item(name));s.set(DataComponents.DYED_COLOR,new DyedItemColor(rgb&0xFFFFFF));return s;}
 public static int blend(List<Integer> colors){long r=0,g=0,b=0,m=0;for(int c:colors){int rr=c>>16&255,gg=c>>8&255,bb=c&255;r+=rr;g+=gg;b+=bb;m+=Math.max(rr,Math.max(gg,bb));}int n=colors.size();int ar=(int)(r/n),ag=(int)(g/n),ab=(int)(b/n),top=Math.max(ar,Math.max(ag,ab));if(top==0)return 0;float brightness=(float)m/n;return (int)(ar*brightness/top)<<16 | (int)(ag*brightness/top)<<8 | (int)(ab*brightness/top);}
 public static ItemStack dye(CraftingInput input,String feature){
  if(!Rules.on(feature))return ItemStack.EMPTY;String kind=feature.equals("mixed_wool")?"wool":"bed";ItemStack base=ItemStack.EMPTY;var dyes=new ArrayList<DyeColor>();
  for(var s:input.items())if(!s.isEmpty()){if(kind(s,kind) && base.isEmpty())base=s;else if(s.get(DataComponents.DYE)!=null)dyes.add(s.get(DataComponents.DYE));else return ItemStack.EMPTY;}
  if(base.isEmpty() || dyes.isEmpty() || dyes.size()>Rules.i(feature,"maximum_dyes"))return ItemStack.EMPTY;
  return stack(feature,DyedItemColor.applyDyes(new DyedItemColor(color(base)),dyes).rgb());
 }
 public static ItemStack bed(CraftingInput input){
  if(!Rules.on("mixed_bed"))return ItemStack.EMPTY;
  var dyed=dye(input,"mixed_bed");if(!dyed.isEmpty())return dyed;
  if(input.width()!=3 || input.height()!=2)return ItemStack.EMPTY;var colors=new ArrayList<Integer>();boolean custom=false;
  for(int i=0;i<3;i++){var s=input.getItem(i);if(!kind(s,"wool") || !input.getItem(i+3).is(net.minecraft.tags.ItemTags.PLANKS))return ItemStack.EMPTY;colors.add(color(s));custom |= s.is(ExtraContent.item("mixed_wool"));}
  boolean same=colors.stream().distinct().count()==1;if(!Rules.b("mixed_bed","allow_mixed_wool") && !same)return ItemStack.EMPTY;
  if(same && !custom)return ItemStack.EMPTY;return stack("mixed_bed",blend(colors));
 }
 @SuppressWarnings("unchecked") public static <T extends Recipe<?>> RecipeHolder<T> priority(CraftingInput input,Level level){
  if(!(level instanceof net.minecraft.server.level.ServerLevel server))return null;
  for(String id:List.of("mixed_bed","mixed_wool")){
   var result=id.equals("mixed_bed")?bed(input):dye(input,id);if(result.isEmpty())continue;
   var key=ResourceKey.create(Registries.RECIPE,ModContent.id(id));return (RecipeHolder<T>)server.getServer().getRecipeManager().byKey(key).orElse(null);
  }return null;
 }
 public static void registerHomes(){
  var bed=ExtraContent.block("mixed_bed");var states=new HashSet<>(bed.getStateDefinition().getPossibleStates());states.removeIf(s->s.getValue(BlockStateProperties.BED_PART)!=BedPart.HEAD);
  com.sumirelabs.sum1rem0d.mixin.PathPoiAccess.sum1rem0d$register(BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(PoiTypes.HOME),states);
  ((com.sumirelabs.sum1rem0d.mixin.FireRegistrationAccess)Blocks.FIRE).sum1rem0d$flammable(ExtraContent.block("mixed_wool"),30,60);
 }
}
