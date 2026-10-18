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
 public static ItemStack woolShape(CraftingInput input,String feature){
  if(!List.of("mixed_wool_slab","mixed_wool_stairs").contains(feature)||!Rules.on(feature)||!Rules.b(feature,"recipe_enabled"))return ItemStack.EMPTY;
  int[] slots;
  if(feature.equals("mixed_wool_slab")){
   if(input.width()!=3||input.height()<1)return ItemStack.EMPTY;slots=null;
   for(int row=0;row<input.height()&&slots==null;row++){int[] candidate=new int[]{row*input.width(),row*input.width()+1,row*input.width()+2};if(patternMatches(input,candidate))slots=candidate;}
   if(slots==null)return ItemStack.EMPTY;
  }else{
   if(input.width()!=3||input.height()!=3)return ItemStack.EMPTY;
   int[] left=new int[]{0,3,6,4,7,8},right=new int[]{2,5,8,4,7,6};
   if(patternMatches(input,left))slots=left;else if(patternMatches(input,right))slots=right;else return ItemStack.EMPTY;
  }
  int output=Math.max(1,Rules.i(feature,"recipe_output"));
  if(input.items().stream().filter(s->!s.isEmpty()).count()!=slots.length)return ItemStack.EMPTY;
  var colors=new ArrayList<Integer>();
  for(int slot:slots){var wool=input.getItem(slot);if(!kind(wool,"wool"))return ItemStack.EMPTY;colors.add(color(wool));}
  if(!Rules.b(feature,"allow_mixed_colors")&&colors.stream().distinct().count()>1)return ItemStack.EMPTY;
  boolean custom=false;for(int slot:slots)custom|=input.getItem(slot).is(ExtraContent.item("mixed_wool"));
  if(!custom && colors.stream().distinct().count()==1){
   var original=BuiltInRegistries.ITEM.getKey(input.getItem(slots[0]).getItem()).getPath();
   for(var dye:DyeColor.values())if(original.equals(dye.getName()+"_wool"))return new ItemStack(WoolShapeBackport.item(dye,feature.equals("mixed_wool_stairs")),output);
  }
  return stack(feature,blend(colors)).copyWithCount(output);
 }
 public static ItemStack concreteShape(CraftingInput input,String feature){
  if(!List.of("mixed_concrete_slab","mixed_concrete_stairs").contains(feature)||!Rules.on(feature)||!Rules.b(feature,"recipe_enabled"))return ItemStack.EMPTY;
  int[] slots;
  if(feature.equals("mixed_concrete_slab")){
   if(input.width()!=3||input.height()<1)return ItemStack.EMPTY;slots=null;
   for(int row=0;row<input.height()&&slots==null;row++){int[] candidate=new int[]{row*input.width(),row*input.width()+1,row*input.width()+2};if(patternMatches(input,candidate))slots=candidate;}
   if(slots==null)return ItemStack.EMPTY;
  }else{
   if(input.width()!=3||input.height()!=3)return ItemStack.EMPTY;
   int[] left=new int[]{0,3,6,4,7,8},right=new int[]{2,5,8,4,7,6};
   if(patternMatches(input,left))slots=left;else if(patternMatches(input,right))slots=right;else return ItemStack.EMPTY;
  }
  int output=Math.max(1,Rules.i(feature,"recipe_output"));
  if(input.items().stream().filter(s->!s.isEmpty()).count()!=slots.length)return ItemStack.EMPTY;
  var colors=new ArrayList<Integer>();
  for(int slot:slots){var concrete=input.getItem(slot);if(!MixedConcrete.isConcrete(concrete))return ItemStack.EMPTY;colors.add(MixedConcrete.color(concrete).rgb()&0xFFFFFF);}
  if(!Rules.b(feature,"allow_mixed_colors")&&colors.stream().distinct().count()>1)return ItemStack.EMPTY;
  return stack(feature,blend(colors)).copyWithCount(output);
 }
 private static boolean patternMatches(CraftingInput input,int[] slots){var occupied=new HashSet<Integer>();for(int slot:slots)occupied.add(slot);for(int i=0;i<input.items().size();i++)if(input.getItem(i).isEmpty()==occupied.contains(i))return false;return true;}
 @SuppressWarnings("unchecked") public static <T extends Recipe<?>> RecipeHolder<T> priority(CraftingInput input,Level level){
  if(!(level instanceof net.minecraft.server.level.ServerLevel server))return null;
  for(String id:List.of("mixed_bed","mixed_concrete_slab","mixed_concrete_stairs","mixed_wool_slab","mixed_wool_stairs","mixed_wool")){
   var result=id.equals("mixed_bed")?bed(input):id.equals("mixed_wool_slab")||id.equals("mixed_wool_stairs")?woolShape(input,id):id.equals("mixed_concrete_slab")||id.equals("mixed_concrete_stairs")?concreteShape(input,id):dye(input,id);if(result.isEmpty())continue;
   var key=ResourceKey.create(Registries.RECIPE,ModContent.id(id));return (RecipeHolder<T>)server.getServer().getRecipeManager().byKey(key).orElse(null);
  }return null;
 }
 public static void registerHomes(){
  var bed=ExtraContent.block("mixed_bed");var states=new HashSet<>(bed.getStateDefinition().getPossibleStates());states.removeIf(s->s.getValue(BlockStateProperties.BED_PART)!=BedPart.HEAD);
  com.sumirelabs.sum1rem0d.mixin.PathPoiAccess.sum1rem0d$register(BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(PoiTypes.HOME),states);
  ((com.sumirelabs.sum1rem0d.mixin.FireRegistrationAccess)Blocks.FIRE).sum1rem0d$flammable(ExtraContent.block("mixed_wool"),30,60);
  ((com.sumirelabs.sum1rem0d.mixin.FireRegistrationAccess)Blocks.FIRE).sum1rem0d$flammable(ExtraContent.block("mixed_wool_slab"),30,60);
  ((com.sumirelabs.sum1rem0d.mixin.FireRegistrationAccess)Blocks.FIRE).sum1rem0d$flammable(ExtraContent.block("mixed_wool_stairs"),30,60);
 }
}
