package com.sumirelabs.sum1rem0d.game;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.network.chat.Component;
import java.util.UUID;
public final class ShopCatalog {
 private ShopCatalog(){}
 private static final Map<String,Long> RESOURCE_VALUES=Map.ofEntries(
  Map.entry("minecraft:coal",20L),Map.entry("minecraft:charcoal",20L),Map.entry("minecraft:raw_iron",30L),Map.entry("minecraft:iron_ingot",40L),Map.entry("minecraft:iron_block",320L),
  Map.entry("minecraft:raw_copper",18L),Map.entry("minecraft:copper_ingot",25L),Map.entry("minecraft:copper_block",225L),Map.entry("minecraft:raw_gold",60L),Map.entry("minecraft:gold_ingot",80L),Map.entry("minecraft:gold_block",640L),
  Map.entry("minecraft:redstone",8L),Map.entry("minecraft:lapis_lazuli",12L),Map.entry("minecraft:emerald",50L),Map.entry("minecraft:diamond",500L),Map.entry("minecraft:diamond_block",4000L),
  Map.entry("minecraft:ancient_debris",400L),Map.entry("minecraft:netherite_scrap",500L),Map.entry("minecraft:netherite_ingot",1800L),Map.entry("minecraft:netherite_block",14400L),
  Map.entry("minecraft:iron_sword",120L),Map.entry("minecraft:diamond_sword",1200L),Map.entry("minecraft:netherite_sword",4000L),Map.entry("minecraft:iron_pickaxe",160L),Map.entry("minecraft:diamond_pickaxe",1500L),Map.entry("minecraft:netherite_pickaxe",5000L),
  Map.entry("minecraft:enchanted_golden_apple",5000L),Map.entry("minecraft:elytra",12000L),Map.entry("minecraft:totem_of_undying",4000L));
 public static boolean allowed(ItemStack stack){
  if(stack.isEmpty() || BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ModContent.id("automated_crafting_table")) || Rules.matches(stack,Rules.list("sumzon","excluded_items")))return false;
  if(!Rules.b("sumzon","allow_technical")){String path=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();if(path.endsWith("_spawn_egg") || List.of("test_block","test_instance_block","command_block","chain_command_block","repeating_command_block","structure_block","jigsaw","barrier","light","structure_void","debug_stick","bedrock","spawner","end_portal_frame").contains(path))return false;}
  return true;
 }
 public static String category(ItemStack stack){if(stack.getItem() instanceof BlockItem)return "blocks";if(stack.has(DataComponents.EQUIPPABLE) || stack.has(DataComponents.TOOL) || stack.has(DataComponents.WEAPON))return "equipment";if(stack.has(DataComponents.FOOD))return "food";return "other";}
 public static long price(ItemStack stack){
  String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();long price=Rules.i("sumzon",switch(category(stack)){case "blocks"->"block_price";case "equipment"->"equipment_price";case "food"->"food_price";default->"base_price";});
  if(stack.is(Items.PLAYER_HEAD)&&stack.has(DataComponents.PROFILE))price=Math.max(price,Rules.i("sumzon","player_head_price"));
  if(stack.getRarity()!=Rarity.COMMON)price=Math.max(price,Rules.i("sumzon","rare_price"));
  boolean custom=false;for(String entry:Rules.list("sumzon","prices")){var pair=entry.split("=",2);if(pair.length==2 && pair[0].trim().equals(id)){price=Long.parseLong(pair[1].trim());custom=true;}}
  if(!custom)price=Math.max(price,RESOURCE_VALUES.getOrDefault(id,0L));
  return Math.max(price,Economy.value(stack));
 }
 public static String key(ItemStack stack){if(stack.is(Items.PLAYER_HEAD)&&stack.has(DataComponents.PROFILE)){var uuid=stack.get(DataComponents.PROFILE).partialProfile().id();if(uuid!=null)return "sum1rem0d:player_head/"+uuid;}return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();}
 public static UUID playerFaceId(String id){if(!id.startsWith("sum1rem0d:player_head/"))return null;try{return UUID.fromString(id.substring("sum1rem0d:player_head/".length()));}catch(IllegalArgumentException ignored){return null;}}
 public static ItemStack playerHead(UUID id,String name){var head=new ItemStack(Items.PLAYER_HEAD);head.set(DataComponents.PROFILE,ResolvableProfile.createUnresolved(id));head.set(DataComponents.CUSTOM_NAME,Component.literal(name));return head;}
 public static ItemStack item(String id){if(id.startsWith("sum1rem0d:player_head/")){var uuid=playerFaceId(id);return uuid==null?ItemStack.EMPTY:playerHead(uuid,"参加プレイヤー");}var key=Identifier.tryParse(id);var item=key==null?Items.AIR:BuiltInRegistries.ITEM.getOptional(key).orElse(Items.AIR);return item==Items.AIR?ItemStack.EMPTY:new ItemStack(item);}
}
