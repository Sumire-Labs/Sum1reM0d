package com.sumirelabs.sum1rem0d.client;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.sumirelabs.sum1rem0d.game.*;
import com.sumirelabs.sum1rem0d.network.EquipmentStatePayload;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.*;
import net.minecraft.world.item.*;
/** Connection-local defaults; server values never overwrite the user's config file. */
public final class EquipmentDefaultsClient {
 private static final Map<String,DataComponentMap> ORIGINAL=new HashMap<>();private static String pending;public static boolean received;
 private EquipmentDefaultsClient(){}
 public static void clear(){for(var entry:ORIGINAL.entrySet())com.sumirelabs.sum1rem0d.game.EquipmentDefaults.item(entry.getKey()).builtInRegistryHolder().bindComponents(entry.getValue());ORIGINAL.clear();pending=null;received=false;com.sumirelabs.sum1rem0d.mixin.CreativeCacheAccess.sum1rem0d$invalidate(null);}
 public static void mismatchForVerification(){if(!Boolean.getBoolean("sum1rem0d.remainingSmoke"))return;var item=ExtraContent.item("redstone_pickaxe");ORIGINAL.put("redstone_pickaxe",item.components());item.builtInRegistryHolder().bindComponents(DataComponentMap.builder().addAll(item.components()).set(DataComponents.MAX_DAMAGE,1).build());var paxel=PaxelContent.item("diamond");ORIGINAL.put("diamond_paxel",paxel.components());paxel.builtInRegistryHolder().bindComponents(DataComponentMap.builder().addAll(paxel.components()).set(DataComponents.MAX_DAMAGE,1).build());}
 public static void accept(EquipmentStatePayload p){pending=p.json();tick();}
 public static void tick(){var mc=Minecraft.getInstance();if(pending==null || mc.level==null)return;var root=JsonParser.parseString(pending).getAsJsonObject();var ops=mc.level.registryAccess().createSerializationContext(JsonOps.INSTANCE);var maps=new HashMap<String,DataComponentMap>();for(String name:com.sumirelabs.sum1rem0d.game.EquipmentDefaults.itemNames())maps.put(name,DataComponentMap.CODEC.parse(ops,root.get(name)).getOrThrow());for(var entry:maps.entrySet()){var item=com.sumirelabs.sum1rem0d.game.EquipmentDefaults.item(entry.getKey());ORIGINAL.putIfAbsent(entry.getKey(),item.components());item.builtInRegistryHolder().bindComponents(entry.getValue());}
  // Normally the packet precedes inventory sync; also handle late refreshes without losing item patches.
  if(mc.player!=null){var inventory=mc.player.getInventory();for(int i=0;i<inventory.getContainerSize();i++)inventory.setItem(i,rebase(inventory.getItem(i)));mc.player.containerMenu.setCarried(rebase(mc.player.containerMenu.getCarried()));}
  com.sumirelabs.sum1rem0d.mixin.CreativeCacheAccess.sum1rem0d$invalidate(null);pending=null;received=true;
 }
 private static ItemStack rebase(ItemStack stack){var key=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());if(stack.isEmpty() || !net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("sum1rem0d") || !EquipmentDefaults.itemNames().contains(key.getPath()))return stack;return new ItemStack(stack.typeHolder(),stack.getCount(),stack.getComponentsPatch());}
}
