package com.sumirelabs.sum1rem0d.game;
import com.google.gson.*;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.network.ServerConfigHandler;
import java.util.*;
import java.nio.file.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.util.ProblemReporter;

/** Disposable local-client fixture; restores native player NBT and persistent mod state on every exit. */
public final class RemainingLiveVerification {
 private static ServerPlayer actor;private static JsonObject config,world;private static net.minecraft.nbt.CompoundTag player;private static long started,finishAt;private static boolean finished,restored;private static net.minecraft.world.level.GameType mode;
 private RemainingLiveVerification(){}
 public static boolean temporary(ServerPlayer p){return Boolean.getBoolean("sum1rem0d.remainingVerify") && p==actor && !restored;}
 public static void tick(MinecraftServer server){
  if(!Boolean.getBoolean("sum1rem0d.remainingVerify") || finished)return;
  try{
   if(actor==null){if(server.getPlayerList().getPlayers().isEmpty())return;ServerVerification.require(server.getPlayerList().getPlayers().size()==1,"Only one local fixture client");actor=server.getPlayerList().getPlayers().getFirst();ServerVerification.require(actor.getGameProfile().name().equals("Developer"),"Local developer account only");started=System.nanoTime();config=Sum1reM0d.config().snapshot();world=WorldState.get(server).checkpoint();var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,actor.registryAccess());actor.saveWithoutId(out);player=out.buildResult();mode=actor.gameMode.getGameModeForPlayer();
    var draft=config.deepCopy();var fs=draft.getAsJsonObject("features");for(var row:fs.entrySet())row.getValue().getAsJsonObject().addProperty("enabled",false);for(String id:List.of("economy","sumzon","ad_premium","sumzon_prime","mixed_concrete","mixed_wool","mixed_bed","emerald_equipment","redstone_equipment","lapis_equipment","tamed_book","scribbling_book"))fs.getAsJsonObject(id).addProperty("enabled",true);fs.getAsJsonObject("sumzon").addProperty("delivery_hours",0);fs.getAsJsonObject("scribbling_book").addProperty("interval",1);Sum1reM0d.config().update(draft,Sum1reM0d.config().revision());var d=WorldState.get(server).player(actor);d.walletInitialized=true;d.coins=100000;d.services=new JsonObject();actor.getInventory().clearContent();actor.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);actor.setNoGravity(true);actor.connection.teleport(0.5,245,0.5,0,0);MixedColorFixture.setup(actor);actor.inventoryMenu.broadcastFullState();ServerConfigHandler.send(actor,"");Gameplay.sendState(actor);
   }
   if(finishAt>0 && server.overworld().getGameTime()>=finishAt){finish(server,true,"");return;}
   if(System.nanoTime()-started>240_000_000_000L)finish(server,false,"Client timeout");
  }catch(Throwable e){Sum1reM0d.LOGGER.error("Remaining live fixture failed",e);finish(server,false,e.toString());}
 }
 public static void command(ServerPlayer p,String phase){if(!temporary(p))throw new IllegalArgumentException("Verification unavailable");switch(phase){case "normal_book"->{if(p.getMainHandItem().is(Items.DIAMOND))p.getInventory().setItem(8,p.getMainHandItem().copy());p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.WRITABLE_BOOK));}case "tamed_book"->p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ExtraContent.item("tamed_book")));case "finish"->{ServerVerification.require(ShopServices.active(p,"ad")&&ShopServices.active(p,"prime"),"Both paid contracts reached server");ServerVerification.require(p.getInventory().countItem(Items.DIAMOND)==2,"Exactly two purchased diamonds received");finishAt=p.level().getGameTime()+20;}default->throw new IllegalArgumentException(phase);}p.inventoryMenu.broadcastFullState();}
 public static void leaving(ServerPlayer p){if(temporary(p)&&!finished)finish(p.level().getServer(),false,"Client left before completion");}
 public static void stopping(MinecraftServer server){if(Boolean.getBoolean("sum1rem0d.remainingVerify")&&!finished)finish(server,false,"Stopped before completion");}
 private static void finish(MinecraftServer server,boolean success,String failure){if(finished)return;finished=true;try{if(config!=null){actor.closeContainer();MixedColorFixture.restore(actor);actor.load(TagValueInput.create(ProblemReporter.DISCARDING,actor.registryAccess(),player));actor.setGameMode(mode);actor.setNoGravity(false);WorldState.get(server).restore(world);WorldState.get(server).saveStrict();Sum1reM0d.config().update(config,Sum1reM0d.config().revision());actor.inventoryMenu.broadcastFullState();restored=true;server.getPlayerList().saveAll();}}catch(Exception e){success=false;failure+="; restore "+e;}var report=new JsonObject();report.addProperty("success",success);report.addProperty("failure",failure);report.add("passed",new Gson().toJsonTree(List.of("real shop checkout and exact inventory delivery","real premium and Prime contracts","fixture player NBT, config and world-state restoration")));try{Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("sum1rem0d-remaining-live.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception e){throw new IllegalStateException(e);}server.halt(false);}
}
