package com.sumirelabs.sum1rem0d.client;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Opt-in actual resource reload checks; restores snapshots and resources before exit. */
final class NetherChangesSmoke {
 private static int stage;
 private static CompletableFuture<?> reload;
 private static JsonObject rules,state;
 private static long revision;
 private static boolean editable;
 private static byte[] rack,ingot,armor;
 private static Throwable failure;
 private static final List<String> passed=new ArrayList<>();
 private NetherChangesSmoke() {}
 private static void require(boolean ok,String text) { if(!ok) throw new AssertionError(text); }
 private static byte[] texture(Minecraft mc,String path) throws Exception { try(var in=mc.getResourceManager().getResource(net.minecraft.resources.Identifier.withDefaultNamespace(path)).orElseThrow().open()) { return in.readAllBytes(); } }
 private static void daily(String id) { var data=new JsonObject();data.addProperty("day",1);var d=new JsonObject();if(!id.isEmpty())d.addProperty(id,true);data.add("daily",d);ClientGameState.accept(new GameStatePayload(data.toString())); }
 static void tick(Minecraft mc) {
  if(!Boolean.getBoolean("sum1rem0d.netherSmoke") || stage<0 || !mc.isGameLoadFinished() || mc.gui.overlay()!=null || reload!=null && !reload.isDone()) return;
  try {
   if(reload!=null) reload.join();
   switch(stage) {
    case 0 -> {
     rules=ClientRules.snapshot();state=ClientGameState.snapshot();revision=ClientRules.revision;editable=ClientRules.editable;
     var fixture=rules.deepCopy();for(String id:List.of("sideways_nylium","retro_netherrack","ardite_disguise")) fixture.getAsJsonObject("features").getAsJsonObject(id).addProperty("enabled",true);
     ClientRules.accept(new ConfigStatePayload(revision,editable,fixture.toString(),"",0,false));
     rack=texture(mc,"textures/block/netherrack.png");ingot=texture(mc,"textures/item/netherite_ingot.png");armor=texture(mc,"textures/entity/equipment/humanoid/netherite.png");
     daily("sideways_nylium");
     require(!VisualBlocks.replace(Blocks.WARPED_NYLIUM.defaultBlockState()).is(Blocks.WARPED_NYLIUM) && !VisualBlocks.replace(Blocks.CRIMSON_NYLIUM.defaultBlockState()).is(Blocks.CRIMSON_NYLIUM),"Both nylium models replaced");
     daily("");require(VisualBlocks.replace(Blocks.WARPED_NYLIUM.defaultBlockState()).is(Blocks.WARPED_NYLIUM),"Nylium restored");passed.add("Nylium model substitution and reset");
     daily("retro_netherrack");reload=DailyTextures.verifyReload(mc,1);stage=1;
    }
    case 1 -> {
     require(Arrays.equals(DailyTextures.programmerArtTexture(),texture(mc,"textures/block/netherrack.png")),"Exact built-in Programmer Art texture is used without enabling the whole pack");
     require(Arrays.equals(ingot,texture(mc,"textures/item/netherite_ingot.png")),"Other event does not leak");passed.add("Retro texture loaded without manyullyn overlap");
     daily("ardite_disguise");reload=DailyTextures.verifyReload(mc,2);stage=2;
    }
    case 2 -> {
     require(Arrays.equals(rack,texture(mc,"textures/block/netherrack.png")),"Retro texture restored");
     require(!Arrays.equals(ingot,texture(mc,"textures/item/netherite_ingot.png")) && !Arrays.equals(armor,texture(mc,"textures/entity/equipment/humanoid/netherite.png")),"Item and equipped armor palettes changed");
     require(NetherNames.name(BuiltInRegistries.ITEM.getKey(Items.ANCIENT_DEBRIS)).equals(net.minecraft.network.chat.Component.translatable("sum1rem0d.disguise.ardite_ore")),"Ore name");
     require(NetherNames.name(BuiltInRegistries.ITEM.getKey(Items.NETHERITE_SCRAP)).equals(net.minecraft.network.chat.Component.translatable("sum1rem0d.disguise.ardite_ingot")),"Scrap name");
     require(NetherNames.name(BuiltInRegistries.ITEM.getKey(Items.NETHERITE_SWORD)).equals(net.minecraft.network.chat.Component.translatable("sum1rem0d.disguise.manyullyn_sword")),"Sword name");
     passed.add("Ardite/manyullyn item names and item/equipment textures");
     daily("");reload=DailyTextures.verifyReload(mc,0);stage=3;
    }
    case 3 -> {
     require(Arrays.equals(rack,texture(mc,"textures/block/netherrack.png")) && Arrays.equals(ingot,texture(mc,"textures/item/netherite_ingot.png")) && Arrays.equals(armor,texture(mc,"textures/entity/equipment/humanoid/netherite.png")),"All original resources restored");
     require(NetherNames.name(BuiltInRegistries.ITEM.getKey(Items.NETHERITE_SWORD))==null,"Name restored");passed.add("Event end restores original resources and name");finish(mc);
    }
    case 4 -> finish(mc);
   }
  } catch(Throwable error) {
   failure=error;com.sumirelabs.sum1rem0d.Sum1reM0d.LOGGER.error("Nether visual verification failed",error);
   if(stage==4) {finish(mc);return;}
   daily("");reload=DailyTextures.verifyReload(mc,0);stage=4;
  }
 }
 private static void finish(Minecraft mc) {
  stage=-1;
  try {
   if(rules!=null)ClientRules.accept(new ConfigStatePayload(revision,editable,rules.toString(),"",0,false));
   if(state!=null)ClientGameState.accept(new GameStatePayload(state.toString()));
   var report=new JsonObject();report.addProperty("success",failure==null);report.addProperty("failure",failure==null?"":failure.toString());report.add("passed",new Gson().toJsonTree(passed));
   Files.writeString(mc.gameDirectory.toPath().resolve("sum1rem0d-nether-visual.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
  }catch(Exception e){throw new IllegalStateException(e);}mc.stop();
 }
}
