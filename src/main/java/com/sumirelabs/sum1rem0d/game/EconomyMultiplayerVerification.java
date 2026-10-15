package com.sumirelabs.sum1rem0d.game;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.network.ServerConfigHandler;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;

/** Opt-in two real client round trip; never enabled in ordinary gameplay. */
public final class EconomyMultiplayerVerification {
    private static final Set<String> NAMES=Set.of("Developer","SumireGuest");
    private static final Map<String,ServerPlayer> actors=new HashMap<>();
    private static final Map<String,List<ItemStack>> inventories=new HashMap<>();
    private static final Map<String,boolean[]> flight=new HashMap<>();
    private static final Map<String,String> markers=new HashMap<>();
    private static final Map<String,net.minecraft.world.phys.Vec3> positions=new HashMap<>();
    private static final Set<String> ack=new HashSet<>();
    private static final List<String> passed=new ArrayList<>();
    private static JsonObject original,checkpoint;
    private static long started, finishAt;
    private static boolean restored,finished;
    private static int phase=-1;
    private EconomyMultiplayerVerification(){}
    public static int phase(){return Boolean.getBoolean("sum1rem0d.economyVerify")?phase:-1;}
    public static boolean temporaryInventory(ServerPlayer p){return Boolean.getBoolean("sum1rem0d.economyVerify")&&original!=null&&!restored&&actors.containsKey(p.getGameProfile().name());}
    public static void acknowledge(ServerPlayer p,long value){if(Boolean.getBoolean("sum1rem0d.economyVerify")&&value==phase&&NAMES.contains(p.getGameProfile().name()))ack.add(p.getGameProfile().name());}
    private static boolean ready(){return ack.containsAll(NAMES);}
    private static void next(MinecraftServer server,int value){phase=value;Sum1reM0d.LOGGER.info("ECONOMY_MULTI phase {}",value);ack.clear();for(var p:actors.values()){ServerConfigHandler.send(p,"");Gameplay.sendState(p);}}
    public static void tick(MinecraftServer server){
        if(!Boolean.getBoolean("sum1rem0d.economyVerify")||finished)return;
        try{
            if(started==0){if(server.getPlayerList().getPlayers().isEmpty())return;started=System.nanoTime();}
            if(System.nanoTime()-started>240_000_000_000L)throw new AssertionError("Economy round trip timeout at "+phase);
            for(var p:server.getPlayerList().getPlayers())if(!NAMES.contains(p.getGameProfile().name()))throw new AssertionError("Only verification clients may join");
            if(original==null){
                if(server.getPlayerList().getPlayers().size()!=2)return;
                original=Sum1reM0d.config().snapshot();checkpoint=WorldState.get(server).checkpoint();
                for(var p:server.getPlayerList().getPlayers()){String name=p.getGameProfile().name();actors.put(name,p);var items=new ArrayList<ItemStack>();for(int i=0;i<p.getInventory().getContainerSize();i++)items.add(p.getInventory().getItem(i).copy());inventories.put(name,items);positions.put(name,p.position());markers.put(name,((CoinInventoryRevision)p).sum1rem0d$coinRevision());flight.put(name,new boolean[]{p.getAbilities().mayfly,p.getAbilities().flying});}
                next(server,0);return;
            }
            var a=actors.get("Developer");var b=actors.get("SumireGuest");var state=WorldState.get(server);
            switch(phase){
                case 0->{if(!ready())return;var config=original.deepCopy();for(var feature:config.getAsJsonObject("features").entrySet())feature.getValue().getAsJsonObject().addProperty("enabled",false);for(String id:List.of("economy","player_trading"))config.getAsJsonObject("features").getAsJsonObject(id).addProperty("enabled",true);Sum1reM0d.config().update(config,Sum1reM0d.config().revision());
                    for(var p:actors.values()){p.closeContainer();p.getInventory().clearContent();p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();p.connection.teleport(0.5,245,0.5,p.getYRot(),p.getXRot());p.setDeltaMovement(0,0,0);p.setNoGravity(true);state.player(p).walletInitialized=true;state.player(p).coins=p==a?100:20;p.getInventory().setItem(0,new ItemStack(p==a?Items.DIAMOND:Items.EMERALD,p==a?4:2));p.inventoryMenu.broadcastFullState();}next(server,1);}
                case 1->{if(Economy.state(a).has("trade")&&Economy.state(b).has("trade")){passed.add("real client wallet invitation and recipient consent open synchronized menus");next(server,2);}}
                case 2->{var t=Economy.state(a).getAsJsonObject("trade");if(t!=null&&t.get("ownCoins").getAsLong()==30&&t.get("otherCoins").getAsLong()==5&&a.containerMenu.getSlot(0).hasItem()&&a.containerMenu.getSlot(9).hasItem()){passed.add("native inventory clicks and GUI amounts synchronize exact item and currency offers");next(server,3);}}
                case 3->{if(Economy.state(a).has("trade"))return;ServerVerification.require(Economy.balance(a)==75&&Economy.balance(b)==45&&a.getInventory().getItem(0).is(Items.EMERALD)&&b.getInventory().getItem(0).is(Items.DIAMOND),"Two real client confirmations exchange exact items and coins");passed.add("two real client confirmations commit conserved items and currency");next(server,4);}
                case 4->{if(!ready())return;ServerVerification.require(Economy.balance(a)==75&&Economy.balance(b)==45,"Client wallet deposit and withdrawal conserve balances");passed.add("both real clients withdraw ten-coin rolls then redeposit through wallet GUI");restore(server);next(server,5);}
                case 5->{if(!ready())return;passed.add("both clients receive restored original inventory and balance");finishAt=server.overworld().getGameTime()+40;next(server,6);}
                case 6->{if(server.overworld().getGameTime()>=finishAt)finish(server,true,"");}
            }
        }catch(Throwable failure){Sum1reM0d.LOGGER.error("ECONOMY_MULTI failed",failure);finish(server,false,failure.toString());}
    }
    private static void restore(MinecraftServer server)throws Exception{
        if(restored||original==null)return;
        for(var p:actors.values()){Economy.leaving(p);p.closeContainer();var items=inventories.get(p.getGameProfile().name());for(int i=0;i<items.size();i++)p.getInventory().setItem(i,items.get(i).copy());((CoinInventoryRevision)p).sum1rem0d$coinRevision(markers.get(p.getGameProfile().name()));p.getAbilities().mayfly=flight.get(p.getGameProfile().name())[0];p.getAbilities().flying=flight.get(p.getGameProfile().name())[1];p.onUpdateAbilities();p.setNoGravity(false);var pos=positions.get(p.getGameProfile().name());p.connection.teleport(pos.x,pos.y,pos.z,p.getYRot(),p.getXRot());p.inventoryMenu.broadcastFullState();}
        WorldState.get(server).restore(checkpoint);WorldState.get(server).save();Sum1reM0d.config().update(original,Sum1reM0d.config().revision());restored=true;server.getPlayerList().saveAll();
    }
    public static void stopping(MinecraftServer server){if(Boolean.getBoolean("sum1rem0d.economyVerify")&&!finished)finish(server,false,"Stopped before native round trip completed");}
    private static void finish(MinecraftServer server,boolean success,String failure){if(finished)return;finished=true;try{restore(server);}catch(Exception e){success=false;failure+="; restore "+e;}var report=new JsonObject();report.addProperty("success",success);report.addProperty("failure",failure);report.add("passed",new Gson().toJsonTree(passed));try{Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("sum1rem0d-economy-multiplayer.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception e){Sum1reM0d.LOGGER.error("Cannot save economy round trip",e);}server.halt(false);}
}
