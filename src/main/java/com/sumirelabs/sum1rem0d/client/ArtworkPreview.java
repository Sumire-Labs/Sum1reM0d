package com.sumirelabs.sum1rem0d.client;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.network.ConfigStatePayload;
import com.sumirelabs.sum1rem0d.game.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
/** Actual vanilla inventory/player renderer previews, only in the opt-in disposable client fixture. */
final class ArtworkPreview {
 private static int phase,ticks;private static List<ItemStack> saved;private static com.google.gson.JsonObject rules;
 private ArtworkPreview(){}
 static boolean tick(Minecraft mc,Consumer<String> passed){
  if(!Boolean.getBoolean("sum1rem0d.artworkPreview"))return true;
  if(phase==0){saved=new ArrayList<>();for(int i=0;i<mc.player.getInventory().getContainerSize();i++)saved.add(mc.player.getInventory().getItem(i).copy());rules=ClientRules.snapshot();var draft=rules.deepCopy();draft.getAsJsonObject("features").getAsJsonObject("ardite_disguise").addProperty("enabled",true);ClientRules.accept(new ConfigStatePayload(ClientRules.revision,ClientRules.editable,draft.toString(),"",0,false));phase=1;return false;}
  if(phase>=7)return true;
  if(mc.gui.overlay()!=null)return false;
  if(ticks==0){
   for(int i=0;i<mc.player.getInventory().getContainerSize();i++)mc.player.getInventory().setItem(i,ItemStack.EMPTY);
   if(phase<=4){String family=List.of("emerald","redstone","lapis","manyullyn").get(phase-1);String base=family.equals("manyullyn")?"netherite":family;
    for(int i=0;i<ExtraEquipment.PARTS.size();i++){String part=ExtraEquipment.PARTS.get(i);var item=family.equals("manyullyn")?net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(base+"_"+part)):ExtraContent.item(base+"_"+part);mc.player.getInventory().setItem(9+i,new ItemStack(item));}
    String[] parts={"helmet","chestplate","leggings","boots"};EquipmentSlot[] slots={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};for(int i=0;i<4;i++){var item=family.equals("manyullyn")?net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(base+"_"+parts[i])):ExtraContent.item(base+"_"+parts[i]);mc.player.setItemSlot(slots[i],new ItemStack(item));}
   }else if(phase==5){var items=List.of(new ItemStack(ExtraContent.item("tamed_book")),new ItemStack(ExtraContent.item("piston_base")),new ItemStack(Items.NETHERITE_SCRAP),new ItemStack(Items.NETHERITE_INGOT),new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),new ItemStack(Items.ANCIENT_DEBRIS),DyeTextiles.stack("mixed_wool",0x8D65BC),DyeTextiles.stack("mixed_bed",0x8D65BC),new ItemStack(MixedConcrete.item()),new ItemStack(ModContent.coin()),new ItemStack(ModContent.coinRoll()),new ItemStack(ModContent.soap()),new ItemStack(ModContent.mudBall()));for(int i=0;i<items.size();i++)mc.player.getInventory().setItem(9+i,items.get(i));}
   else{for(int i=0;i<saved.size();i++)mc.player.getInventory().setItem(i,saved.get(i));ClientRules.accept(new ConfigStatePayload(ClientRules.revision,ClientRules.editable,rules.toString(),"",0,false));mc.gui.setScreen(null);phase=7;passed.accept("Native inventory renderer previews all four equipped armor families and dedicated special sprites");return true;}
   mc.gui.toastManager().clear();mc.gui.setScreen(new InventoryScreen(mc.player));
   double windowX=0, windowY=0;
   mc.mouseHandler.onMove(mc.getWindow().handle(), windowX, windowY, 0, 0);ticks=1;return false;
  }
  if(++ticks<12)return false;
  String name=phase<=4?List.of("emerald","redstone","lapis","manyullyn").get(phase-1):"special-items";Screenshot.grab(mc.gameDirectory,"sumire-artwork-"+name+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});phase++;ticks=0;return false;
 }
}
