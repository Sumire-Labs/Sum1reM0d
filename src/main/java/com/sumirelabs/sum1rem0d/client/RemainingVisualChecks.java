package com.sumirelabs.sum1rem0d.client;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.network.ConfigStatePayload;
import com.sumirelabs.sum1rem0d.game.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import com.mojang.blaze3d.vertex.PoseStack;

final class RemainingVisualChecks {
 private RemainingVisualChecks(){}
 private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
 static void verify(Minecraft mc,Consumer<String> passed){
  for(String name:List.of("mixed_wool","mixed_bed")){
   var block=ExtraContent.block(name).defaultBlockState();var pos=new BlockPos(0,64,0);var entity=new TextileEntity(name,pos,block);entity.color(0x13579B);
   var world=(BlockAndTintGetter)Proxy.newProxyInstance(BlockAndTintGetter.class.getClassLoader(),new Class<?>[]{BlockAndTintGetter.class},(proxy,method,args)->switch(method.getName()){case "getBlockEntity"->entity;case "getBlockState"->block;case "toString"->"Textile color fixture";default->throw new UnsupportedOperationException(method.getName());});
   var source=mc.getBlockColors().getTintSource(block,0);require(source!=null && source.colorInWorld(block,world,pos)==0xFF13579B,"Exact native textile tint "+name);entity.color(0xBC6731);require(source.colorInWorld(block,world,pos)==0xFFBC6731,"Updated native textile tint "+name);
  }
  passed.accept("Native wool and bed block renderer tints read exact changing 24bit colors");
  for(String name:List.of("mixed_concrete","mixed_wool","mixed_bed")){
   var stack=new ItemStack(name.equals("mixed_concrete")?MixedConcrete.item():ExtraContent.item(name));
   stack.set(net.minecraft.core.component.DataComponents.DYED_COLOR,new net.minecraft.world.item.component.DyedItemColor(0x13579B));
   var tints=new ArrayList<Integer>();
   var tintedCollector=(SubmitNodeCollector)Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(),new Class<?>[]{SubmitNodeCollector.class},(proxy,method,args)->{if(method.getName().equals("order"))return proxy;if(method.getName().equals("submitItem"))for(int tint:(int[])args[5])tints.add(tint);return null;});
   var tintedState=new ItemStackRenderState();mc.getItemModelResolver().updateForTopItem(tintedState,stack,ItemDisplayContext.GUI,mc.level,mc.player,0);tintedState.submit(new PoseStack(),tintedCollector,0,0,0);
   require(tints.contains(0xFF13579B),"Actual item render submission preserves exact RGB "+name);
  }
  passed.accept("Concrete, wool and bed actual item render submissions preserve their dyed component RGB");
  var lights=new ArrayList<Integer>();var collector=(SubmitNodeCollector)Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(),new Class<?>[]{SubmitNodeCollector.class},(proxy,method,args)->{if(method.getName().equals("order"))return proxy;if(method.getName().equals("submitItem"))lights.add((Integer)args[2]);return null;});var state=new ItemStackRenderState();
  mc.getItemModelResolver().updateForTopItem(state,new ItemStack(ExtraContent.item("redstone_pickaxe")),ItemDisplayContext.GUI,mc.level,mc.player,0);state.submit(new PoseStack(),collector,0,0,0);require(!lights.isEmpty() && lights.stream().allMatch(i->i==0xF000F0),"Native redstone item submits full-bright quads");
  lights.clear();mc.getItemModelResolver().updateForTopItem(state,new ItemStack(Items.IRON_PICKAXE),ItemDisplayContext.GUI,mc.level,mc.player,0);state.submit(new PoseStack(),collector,0,0,0);require(!lights.isEmpty() && lights.stream().allMatch(i->i==0),"Reused item state clears glow for ordinary items");
  var before=ClientRules.snapshot();try{var off=before.deepCopy();off.getAsJsonObject("features").getAsJsonObject("redstone_equipment").addProperty("glow_enabled",false);ClientRules.accept(new ConfigStatePayload(ClientRules.revision,ClientRules.editable,off.toString(),"",0,false));lights.clear();mc.getItemModelResolver().updateForTopItem(state,new ItemStack(ExtraContent.item("redstone_pickaxe")),ItemDisplayContext.GUI,mc.level,mc.player,0);state.submit(new PoseStack(),collector,0,0,0);require(!lights.isEmpty() && lights.stream().allMatch(i->i==0),"Config OFF restores ordinary native item lighting");}finally{ClientRules.accept(new ConfigStatePayload(ClientRules.revision,ClientRules.editable,before.toString(),"",0,false));}
  passed.accept("Native equipment resolver and render submission full-bright, reused-state cleanup and live glow OFF");
 }
}
