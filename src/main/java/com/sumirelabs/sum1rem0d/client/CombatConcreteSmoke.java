package com.sumirelabs.sum1rem0d.client;
import com.google.gson.*;
import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import java.nio.file.Files;
import java.lang.reflect.Proxy;
import java.util.*;
final class CombatConcreteSmoke {
 private static boolean done;
 private CombatConcreteSmoke() {}
 private static void require(boolean ok,String text){if(!ok)throw new AssertionError(text);}
 static void tick(Minecraft mc){
  if(done || !Boolean.getBoolean("sum1rem0d.combatConcreteSmoke") || !mc.isGameLoadFinished() || mc.gui.overlay()!=null)return;done=true;
  var report=new JsonObject();var passed=new ArrayList<String>();
  try{
   var state=MixedConcrete.block().defaultBlockState();var pos=new BlockPos(0,64,0);var entity=new MixedConcreteEntity(pos,state);entity.color(0x8436BC);
   var world=(BlockAndTintGetter)Proxy.newProxyInstance(BlockAndTintGetter.class.getClassLoader(),new Class<?>[]{BlockAndTintGetter.class},(proxy,method,args)->{
    if(method.getName().equals("getBlockEntity"))return pos.equals(args[0])?entity:null;
    if(method.getName().equals("getBlockState"))return state;
    if(method.getName().equals("toString"))return "Mixed concrete tint fixture";
    throw new UnsupportedOperationException(method.getName());
   });
   var source=mc.getBlockColors().getTintSource(state,0);require(source!=null && mc.getBlockColors().getTintSources(state).size()==1,"Registered tint source");
   require(source.colorInWorld(state,world,pos)==0xFF8436BC,"Exact RGB from block entity reaches renderer tint");
   require(source.colorInWorld(state,world,pos.above())==0xFFFFFFFF,"Missing entity has safe white fallback");
   entity.color(0x13579B);require(source.colorInWorld(state,world,pos)==0xFF13579B,"Updated RGB is read without stale cache");passed.add("Native block color registration, exact RGB, updates and fallback");
   var model=mc.getModelManager().getItemModel(ModContent.id("mixed_concrete"));require(model!=null && !model.getClass().getSimpleName().contains("Missing"),"Mixed concrete item model baked");
   try(var resource=mc.getResourceManager().getResource(ModContent.id("items/mixed_concrete.json")).orElseThrow().open()){
    var json=JsonParser.parseReader(new java.io.InputStreamReader(resource,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();require(json.getAsJsonObject("model").getAsJsonArray("tints").get(0).getAsJsonObject().get("type").getAsString().equals("minecraft:dye"),"Native dyed item tint source");require(json.getAsJsonObject("model").getAsJsonArray("tints").get(0).getAsJsonObject().get("default").getAsInt()==-1,"Undyed creative/command item stays opaque white");
   }
   passed.add("Baked mixed concrete item model and native dye tint");
   require(com.sumirelabs.sum1rem0d.wiki.WikiContent.article("sword_guard")!=null && com.sumirelabs.sum1rem0d.wiki.WikiContent.article("mixed_concrete")!=null,"Wiki covers both features");passed.add("Config and Wiki feature coverage");
   report.addProperty("success",true);
  }catch(Throwable error){report.addProperty("success",false);report.addProperty("failure",error.toString());com.sumirelabs.sum1rem0d.Sum1reM0d.LOGGER.error("Combat/concrete client verification failed",error);}
  report.add("passed",new Gson().toJsonTree(passed));
  try{Files.writeString(mc.gameDirectory.toPath().resolve("sum1rem0d-combat-concrete-client.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception e){throw new IllegalStateException(e);}mc.stop();
 }
}
