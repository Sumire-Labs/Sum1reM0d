package com.sumirelabs.sum1rem0d.game;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
public final class ShopCatalog {
 private ShopCatalog(){}
 public static boolean allowed(ItemStack stack){
  if(stack.isEmpty() || Rules.matches(stack,Rules.list("sumzon","excluded_items")))return false;
  if(!Rules.b("sumzon","allow_technical")){String path=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();if(path.endsWith("_spawn_egg") || List.of("test_block","test_instance_block","command_block","chain_command_block","repeating_command_block","structure_block","jigsaw","barrier","light","structure_void","debug_stick","bedrock","spawner","end_portal_frame").contains(path))return false;}
  return true;
 }
 public static String category(ItemStack stack){if(stack.getItem() instanceof BlockItem)return "blocks";if(stack.has(DataComponents.EQUIPPABLE) || stack.has(DataComponents.TOOL) || stack.has(DataComponents.WEAPON))return "equipment";if(stack.has(DataComponents.FOOD))return "food";return "other";}
 public static long price(ItemStack stack){
  String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();long price=Rules.i("sumzon",switch(category(stack)){case "blocks"->"block_price";case "equipment"->"equipment_price";case "food"->"food_price";default->"base_price";});
  if(stack.getRarity()!=Rarity.COMMON)price=Math.max(price,Rules.i("sumzon","rare_price"));
  for(String entry:Rules.list("sumzon","prices")){var pair=entry.split("=",2);if(pair.length==2 && pair[0].trim().equals(id))price=Long.parseLong(pair[1].trim());}
  return Math.max(price,Economy.value(stack));
 }
 public static ItemStack item(String id){var key=Identifier.tryParse(id);var item=key==null?Items.AIR:BuiltInRegistries.ITEM.getOptional(key).orElse(Items.AIR);return item==Items.AIR?ItemStack.EMPTY:new ItemStack(item);}
}
