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
  var displayKeys=new HashSet<String>();
  for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM){var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);if(!id.getNamespace().equals("sum1rem0d"))continue;
   displayKeys.add((item instanceof BlockItem?"block.":"item.")+id.getNamespace()+"."+id.getPath());
   require(!mc.getModelManager().getItemModel(id).getClass().getSimpleName().contains("Missing"),"Registered item model resolves: "+id);
  }
  for(var block:net.minecraft.core.registries.BuiltInRegistries.BLOCK){var id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);if(id.getNamespace().equals("sum1rem0d"))displayKeys.add("block.sum1rem0d."+id.getPath());}
  for(var effect:net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT){var id=net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect);if(id.getNamespace().equals("sum1rem0d"))displayKeys.add("effect.sum1rem0d."+id.getPath());}
  for(String locale:List.of("ja_jp","en_us")){
   try(var reader=new java.io.InputStreamReader(mc.getResourceManager().getResource(ModContent.id("lang/"+locale+".json")).orElseThrow().open(),java.nio.charset.StandardCharsets.UTF_8)){
    var translations=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();for(String key:displayKeys)require(translations.has(key),"Registered display name has "+locale+" translation: "+key);
   }catch(java.io.IOException error){throw new IllegalStateException(error);}
  }
  passed.accept("All registered Mod item models and Japanese/English item, block and effect names resolve");
  for(String material:List.of("wooden","stone","copper","iron","golden","diamond","netherite","emerald","redstone","lapis","manyullyn")){
   try(var stream=mc.getResourceManager().getResource(ModContent.id("textures/item/"+material+"_paxel.png")).orElseThrow().open()){
    var sprite=javax.imageio.ImageIO.read(stream);require(sprite!=null && sprite.getWidth()==16 && sprite.getHeight()==16,"Native 16x16 paxel sprite: "+material);int pixels=0;
    for(int y=0;y<16;y++)for(int x=0;x<16;x++){int alpha=sprite.getRGB(x,y)>>>24;require(alpha==0||alpha==255,"Crisp paxel alpha: "+material);if(alpha==255)pixels++;}
    require(pixels>=35 && pixels<196,"Readable transparent paxel silhouette: "+material);
   }catch(java.io.IOException error){throw new IllegalStateException(error);}
  }
  passed.accept("All 11 rebuilt paxel sprites load as crisp native 16x16 transparent images");

  if(ExtraContent.MIXED_SHAPES_AVAILABLE){
   for(var dye:net.minecraft.world.item.DyeColor.values())for(boolean stairs:new boolean[]{false,true}){
    var item=VanillaWoolShapes.item(dye,stairs);var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
    require(item!=Items.AIR && !mc.getModelManager().getItemModel(id).getClass().getSimpleName().contains("Missing"),"Native wool shape item model resolves: "+id);
   }
   passed.accept("All 32 native wool shape item models resolve");
  }

  var savedRules=ClientRules.snapshot();var tintDraft=savedRules.deepCopy();var glowOptions=tintDraft.getAsJsonObject("features").getAsJsonObject("glow_wire");
  try{
   glowOptions.addProperty("signal_tint",true);glowOptions.addProperty("minimum_brightness",20);ClientRules.accept(new ConfigStatePayload(ClientRules.revision,ClientRules.editable,tintDraft.toString(),"",0,false));
   var wire=ExtraContent.block("glow_wire");var source=mc.getBlockColors().getTintSource(wire.defaultBlockState(),0);require(source!=null,"Glow wire has native tint provider");int previous=-1;
   for(int power=0;power<=15;power++){var state=wire.defaultBlockState().setValue(GlowWireBlock.POWER,power);int color=source.color(state);require(color==GlowWireTint.signalColor(power,20),"Native glow tint at signal "+power);int red=(color>>16)&255;require(red>previous,"Strictly increasing glow brightness "+power);previous=red;}
   require(source.color(wire.defaultBlockState())==0xFF332A12,"Unpowered wire is dark gold");require(source.color(wire.defaultBlockState().setValue(GlowWireBlock.POWER,15))==0xFFFFD45A,"Full power keeps original gold");
   glowOptions.addProperty("signal_tint",false);ClientRules.accept(new ConfigStatePayload(ClientRules.revision,ClientRules.editable,tintDraft.toString(),"",0,false));require(source.color(wire.defaultBlockState())==0xFFFFD45A,"Tint OFF restores constant gold");
   glowOptions.addProperty("signal_tint",true);glowOptions.addProperty("minimum_brightness",0);ClientRules.accept(new ConfigStatePayload(ClientRules.revision,ClientRules.editable,tintDraft.toString(),"",0,false));require(source.color(wire.defaultBlockState())==0xFF000000,"Config minimum brightness applies");
   for(String name:List.of("sugar_wire","gunpowder_wire")){var options=tintDraft.getAsJsonObject("features").getAsJsonObject(name);options.addProperty("signal_tint",true);options.addProperty("minimum_brightness",20);ClientRules.accept(new ConfigStatePayload(ClientRules.revision,ClientRules.editable,tintDraft.toString(),"",0,false));var wireState=ExtraContent.block(name).defaultBlockState();var other=mc.getBlockColors().getTintSource(wireState,0);int tint=name.equals("sugar_wire")?0xFFF4EFE6:0xFF62666B;int prior=-1;for(int power=0;power<=15;power++){var powered=wireState.setValue(GlowWireBlock.POWER,power);int color=other.color(powered);require(color==GlowWireTint.signalColor(power,20,tint),name+" tint follows signal "+power);int brightness=(color>>16)&255;require(brightness>prior,name+" brightness rises with signal "+power);prior=brightness;}options.addProperty("signal_tint",false);ClientRules.accept(new ConfigStatePayload(ClientRules.revision,ClientRules.editable,tintDraft.toString(),"",0,false));require(other.color(wireState)==tint,name+" tint Config OFF restores its original constant color");}
   try{var ext=Class.forName("net.caffeinemc.mods.sodium.client.model.color.interop.BlockColorsExtension");var providers=(Map<?,?>)ext.getMethod("sodium$getProviders").invoke(mc.getBlockColors());require(providers.containsKey(wire),"Sodium glow color provider registered");}catch(ClassNotFoundException absent){}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
   passed.accept("Native/Sodium glow tint: all 16 signal levels, Config OFF and minimum brightness, independent dust colors");
  }finally{ClientRules.accept(new ConfigStatePayload(ClientRules.revision,ClientRules.editable,savedRules.toString(),"",0,false));}

  for(String name:List.of("mixed_wool","mixed_bed")){
   var block=ExtraContent.block(name).defaultBlockState();var pos=new BlockPos(0,64,0);var entity=new TextileEntity(name,pos,block);entity.color(0x13579B);
   var world=(BlockAndTintGetter)Proxy.newProxyInstance(BlockAndTintGetter.class.getClassLoader(),new Class<?>[]{BlockAndTintGetter.class},(proxy,method,args)->switch(method.getName()){case "getBlockEntity"->entity;case "getBlockState"->block;case "toString"->"Textile color fixture";default->throw new UnsupportedOperationException(method.getName());});
   var source=mc.getBlockColors().getTintSource(block,0);require(source!=null && source.colorInWorld(block,world,pos)==0xFF13579B,"Exact native textile tint "+name);entity.color(0xBC6731);require(source.colorInWorld(block,world,pos)==0xFFBC6731,"Updated native textile tint "+name);
   if(name.equals("mixed_bed")){var white=mc.getBlockColors().getTintSource(block,1);require(white!=null && white.colorInWorld(block,world,pos)==0xFFFFFFFF,"Pillow and wooden parts remain white after cloth color changes");}
  }
  passed.accept("Native wool and bed block renderer tints read exact changing 24bit colors");
  for(String name:List.of("mixed_concrete","mixed_wool","mixed_bed")){
   var stack=new ItemStack(name.equals("mixed_concrete")?MixedConcrete.item():ExtraContent.item(name));
   stack.set(net.minecraft.core.component.DataComponents.DYED_COLOR,new net.minecraft.world.item.component.DyedItemColor(0x13579B));
   var tints=new ArrayList<Integer>();
   var tintedCollector=(SubmitNodeCollector)Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(),new Class<?>[]{SubmitNodeCollector.class},(proxy,method,args)->{if(method.getName().equals("order"))return proxy;if(method.getName().equals("submitItem"))for(int tint:(int[])args[5])tints.add(tint);return null;});
   var tintedState=new ItemStackRenderState();mc.getItemModelResolver().updateForTopItem(tintedState,stack,ItemDisplayContext.GUI,mc.level,mc.player,0);tintedState.submit(new PoseStack(),tintedCollector,0,0,0);
   require(tints.contains(0xFF13579B),"Actual item render submission preserves exact RGB "+name);
   if(name.equals("mixed_bed"))require(tints.size()==2 && tints.get(1)==0xFFFFFFFF,"Actual bed item submits independent white pillow layer");
  }
  passed.accept("Concrete, wool and bed actual item render submissions preserve their dyed component RGB");
  passed.accept("Bed pillow and wooden parts use an independent constant-white layer in world and item render submissions");
  var lights=new ArrayList<Integer>();var collector=(SubmitNodeCollector)Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(),new Class<?>[]{SubmitNodeCollector.class},(proxy,method,args)->{if(method.getName().equals("order"))return proxy;if(method.getName().equals("submitItem"))lights.add((Integer)args[2]);return null;});var state=new ItemStackRenderState();
  mc.getItemModelResolver().updateForTopItem(state,new ItemStack(ExtraContent.item("redstone_pickaxe")),ItemDisplayContext.GUI,mc.level,mc.player,0);state.submit(new PoseStack(),collector,0,0,0);require(!lights.isEmpty() && lights.stream().allMatch(i->i==0xF000F0),"Native redstone item submits full-bright quads");
  lights.clear();mc.getItemModelResolver().updateForTopItem(state,new ItemStack(Items.IRON_PICKAXE),ItemDisplayContext.GUI,mc.level,mc.player,0);state.submit(new PoseStack(),collector,0,0,0);require(!lights.isEmpty() && lights.stream().allMatch(i->i==0),"Reused item state clears glow for ordinary items");
  var before=ClientRules.snapshot();try{var off=before.deepCopy();off.getAsJsonObject("features").getAsJsonObject("redstone_equipment").addProperty("glow_enabled",false);ClientRules.accept(new ConfigStatePayload(ClientRules.revision,ClientRules.editable,off.toString(),"",0,false));lights.clear();mc.getItemModelResolver().updateForTopItem(state,new ItemStack(ExtraContent.item("redstone_pickaxe")),ItemDisplayContext.GUI,mc.level,mc.player,0);state.submit(new PoseStack(),collector,0,0,0);require(!lights.isEmpty() && lights.stream().allMatch(i->i==0),"Config OFF restores ordinary native item lighting");}finally{ClientRules.accept(new ConfigStatePayload(ClientRules.revision,ClientRules.editable,before.toString(),"",0,false));}
  passed.accept("Native equipment resolver and render submission full-bright, reused-state cleanup and live glow OFF");
 }
}
