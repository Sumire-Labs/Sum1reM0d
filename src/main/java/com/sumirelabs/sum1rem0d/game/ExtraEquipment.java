package com.sumirelabs.sum1rem0d.game;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.tags.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
public final class ExtraEquipment {
 public static final List<String> FAMILIES=List.of("emerald","redstone","lapis");
 public static final List<String> PARTS=List.of("sword","pickaxe","axe","shovel","hoe","helmet","chestplate","leggings","boots");
 public static final List<String> ITEMS=FAMILIES.stream().flatMap(f->PARTS.stream().map(p->f+"_"+p)).toList();
 private ExtraEquipment(){}
 public static boolean isArmor(String name){return List.of("helmet","chestplate","leggings","boots").contains(name.substring(name.indexOf('_')+1));}
 public static boolean radiant(ItemStack stack){var id=BuiltInRegistries.ITEM.getKey(stack.getItem());return id.getNamespace().equals("sum1rem0d") && id.getPath().startsWith("redstone_") && ITEMS.contains(id.getPath());}
 public static boolean glowing(ItemStack stack){return radiant(stack) && Rules.on("redstone_equipment") && Rules.b("redstone_equipment","glow_enabled");}
 public static int color(String family){return switch(family){case "emerald"->0x50CF78;case "redstone"->0xE83A32;default->0x487CE8;};}
 public static Item create(String name){
  String family=name.substring(0,name.indexOf('_')),part=name.substring(name.indexOf('_')+1),feature=family+"_equipment";
  var props=new Item.Properties().setId(ResourceKey.create(Registries.ITEM,ModContent.id(name)));
  var repair=TagKey.create(Registries.ITEM,ModContent.id(family+"_repair"));
  if(isArmor(name)){
   var defense=Map.of(ArmorType.HELMET,Rules.i(feature,"helmet_defense"),ArmorType.CHESTPLATE,Rules.i(feature,"chestplate_defense"),ArmorType.LEGGINGS,Rules.i(feature,"leggings_defense"),ArmorType.BOOTS,Rules.i(feature,"boots_defense"));
   var material=new ArmorMaterial(Rules.i(feature,"armor_durability"),defense,Rules.i(feature,"enchantability"),net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON,(float)Rules.n(feature,"toughness"),0,repair,ResourceKey.create(EquipmentAssets.ROOT_ID,ModContent.id(family)));
   return new Item(props.humanoidArmor(material,ArmorType.valueOf(part.toUpperCase(Locale.ROOT))));
  }
  var material=new ToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL,Rules.i(feature,"tool_durability"),(float)Rules.n(feature,"tool_speed"),(float)Rules.n(feature,"attack_bonus"),Rules.i(feature,"enchantability"),repair);
  props=switch(part){case "sword"->props.sword(material,3,-2.4f);case "pickaxe"->props.pickaxe(material,1,-2.8f);case "axe"->props.axe(material,6,-3.1f);case "shovel"->props.shovel(material,1.5f,-3);case "hoe"->props.hoe(material,0,-2);default->throw new IllegalArgumentException(name);};
  return new Item(props);
 }
 public static ItemStack craft(String name,net.minecraft.world.item.crafting.CraftingInput input){
  String family=name.substring(0,name.indexOf('_')),part=name.substring(name.indexOf('_')+1),feature=family+"_equipment";if(!Rules.on(feature))return ItemStack.EMPTY;
  String[] shape=switch(part){case "sword","shovel"->new String[]{"M",part.equals("sword")?"M":"S","S"};case "pickaxe"->new String[]{"MMM"," S "," S "};case "axe"->new String[]{"MM","MS"," S"};case "hoe"->new String[]{"MM"," S"," S"};case "helmet"->new String[]{"MMM","M M"};case "chestplate"->new String[]{"M M","MMM","MMM"};case "leggings"->new String[]{"MMM","M M","M M"};case "boots"->new String[]{"M M","M M"};default->throw new IllegalArgumentException(part);};
  if(input.width()!=shape[0].length() || input.height()!=shape.length)return ItemStack.EMPTY;
  var id=Rules.id(Rules.s(feature,"material_item"));var material=id==null?Items.AIR:BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);if(material==Items.AIR)return ItemStack.EMPTY;
  for(boolean mirror:new boolean[]{false,true}){boolean ok=true;for(int y=0;y<shape.length;y++)for(int x=0;x<shape[0].length();x++){
   char c=shape[y].charAt(mirror?shape[0].length()-x-1:x);var stack=input.getItem(x+y*input.width());if(c==' '?!stack.isEmpty():!stack.is(c=='M'?material:Items.STICK))ok=false;
  }if(ok)return new ItemStack(ExtraContent.item(name));}return ItemStack.EMPTY;
 }
}
