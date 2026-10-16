package com.sumirelabs.sum1rem0d.game;

import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.network.EconomyRequestPayload;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;

/** Development-only checks using native item, merchant and server menu classes. */
final class EconomyVerification {
    private static void request(ServerPlayer p,String action,long amount,long revision){Economy.handle(new EconomyRequestPayload(action,"",amount,revision),p);}
    private static long revision(ServerPlayer p){return Economy.state(p).getAsJsonObject("trade").get("revision").getAsLong();}
    static ServerPlayer actor(MinecraftServer server,ServerLevel level,String name){
        var p=new PlayerVerification.TestPlayer(server,level,name);
        var connection=new Connection(PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(connection);
        p.connection=new ServerGamePacketListenerImpl(server,connection,p,CommonListenerCookie.createInitial(p.getGameProfile(),false)){
            @Override public boolean isAcceptingMessages(){return true;}
            @Override public void send(Packet<?> packet){}
            @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener){}
        };p.setPos(0.5,245,0.5);return p;
    }
    static void verify(MinecraftServer server,Consumer<String> passed)throws Exception{
        var config=Sum1reM0d.config();var original=config.snapshot();var state=WorldState.get(server);var checkpoint=state.checkpoint();
        var a=actor(server,server.overworld(),"SumireCoinA");var b=actor(server,server.overworld(),"SumireCoinB");
        try{
            var candidate=config.snapshot();for(String id:List.of("economy","coin_rewards","coin_trading","player_trading"))candidate.getAsJsonObject("features").getAsJsonObject(id).addProperty("enabled",true);config.update(candidate,config.revision());
            state.player(a).walletInitialized=true;state.player(a).coins=100;state.player(b).walletInitialized=true;state.player(b).coins=20;
            a.getInventory().setItem(0,new ItemStack(ModContent.coin(),3));a.getInventory().setItem(1,new ItemStack(ModContent.coinRoll(),2));request(a,"deposit",0,0);
            require(Economy.balance(a)==123&&a.getInventory().getItem(0).isEmpty()&&a.getInventory().getItem(1).isEmpty(),"Native deposit accounts for both denominations exactly");
            Economy.handle(new EconomyRequestPayload("withdraw","roll",20,0),a);require(Economy.balance(a)==103&&a.getInventory().getItem(0).is(ModContent.coinRoll())&&a.getInventory().getItem(0).getCount()==2,"Native withdrawal creates two ten-coin rolls");
            for(int i=0;i<36;i++)a.getInventory().setItem(i,new ItemStack(Items.STONE,64));request(a,"withdraw",1,0);require(Economy.balance(a)==103&&a.getInventory().getItem(0).is(Items.STONE),"Full inventory withdrawal is atomic");
            a.getInventory().clearContent();state.player(a).coins=Rules.i("economy","maximum_balance");a.getInventory().setItem(0,new ItemStack(ModContent.coin(),1));request(a,"deposit",0,0);require(a.getInventory().getItem(0).getCount()==1&&Economy.balance(a)==Rules.i("economy","maximum_balance"),"Rejected deposit retains physical coin");
            passed.accept("economy native deposits, ten-coin withdrawals, inventory-full and cap rollback");
            var offer=new MerchantOffer(new ItemCost(Items.EMERALD,3),Optional.of(new ItemCost(Items.EMERALD,2)),new ItemStack(Items.EMERALD,4),2,12,5,0.05f,1);offer.setSpecialPriceDiff(-1);
            require(offer.getBaseCostA().is(ModContent.coinRoll())&&offer.getCostB().is(ModContent.coinRoll())&&offer.getResult().is(ModContent.coinRoll()),"Both merchant inputs and output become ten-coin rolls");
            require(offer.getUses()==2&&offer.getMaxUses()==12&&offer.getXp()==5&&offer.getDemand()==1&&offer.getSpecialPriceDiff()==-1,"Merchant demand, stock, XP and discounts retained");
            require(offer.satisfiedBy(new ItemStack(ModContent.coinRoll(),64),new ItemStack(ModContent.coinRoll(),2))&&!offer.satisfiedBy(new ItemStack(Items.EMERALD,64),new ItemStack(Items.EMERALD,2)),"Merchant payment matching uses coins");
            var off=config.snapshot();off.getAsJsonObject("features").getAsJsonObject("coin_trading").addProperty("enabled",false);config.update(off,config.revision());((CoinMerchantConversion)offer).sum1rem0d$prepareCurrency(false);require(offer.getCostA().is(Items.EMERALD)&&offer.getCostB().is(Items.EMERALD)&&offer.getResult().is(Items.EMERALD),"Disabling conversion restores emeralds");config.update(candidate,config.revision());
            passed.accept("native merchant inputs, secondary cost, results, stock, discounts and reversible Config");
            state.player(a).coins=0;var pos=new BlockPos(400,240,0);var tool=new ItemStack(Items.DIAMOND_PICKAXE);Economy.mined(a,pos,Blocks.DIAMOND_ORE.defaultBlockState(),tool);require(Economy.balance(a)==2,"Natural ore pays configured reward");Economy.mined(a,pos,Blocks.DIAMOND_ORE.defaultBlockState(),tool);require(Economy.balance(a)==2,"Claimed ore cannot pay twice");
            var silk=tool.copy();silk.enchant(a.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);Economy.mined(a,pos.east(),Blocks.DIAMOND_ORE.defaultBlockState(),silk);Economy.mined(a,pos.east(),Blocks.DIAMOND_ORE.defaultBlockState(),tool);require(Economy.balance(a)==2,"Silk recovery and subsequent re-mining never pay");
            ServerVerification.place(a.level(),pos.south(4),Blocks.DIAMOND_ORE.defaultBlockState());Economy.mined(a,pos.south(4),Blocks.DIAMOND_ORE.defaultBlockState(),tool);require(Economy.balance(a)==2,"Runtime placed ore is excluded by chunk hook");Economy.mined(a,pos.east(2),Blocks.SPAWNER.defaultBlockState(),tool);require(Economy.balance(a)==102,"Spawner reward pays once");
            var zombie=net.minecraft.world.entity.EntityTypes.ZOMBIE.create(a.level(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);require(zombie!=null,"Zombie exists");zombie.setHealth(0);Economy.killed(zombie,a.damageSources().playerAttack(a));Economy.killed(zombie,a.damageSources().playerAttack(a));require(Economy.balance(a)==112,"Hostile kill cannot pay twice");zombie.discard();
            state.save();require(new WorldState(server).player(a).coins==112,"Balance reloads from actual world file");passed.accept("native ore, placed/silk/duplicate exclusion, spawner and hostile rewards, durable balance reload");
            a.getInventory().clearContent();b.getInventory().clearContent();state.player(a).coins=100;state.player(b).coins=20;
            a.getInventory().setItem(0,new ItemStack(Items.DIAMOND,4));b.getInventory().setItem(0,new ItemStack(Items.EMERALD,2));Economy.startSession(a,b);Economy.select(a,0,false);Economy.select(b,0,false);
            long concurrent=revision(a);request(a,"offer",30,concurrent);request(b,"offer",5,concurrent);require(Economy.state(a).getAsJsonObject("trade").get("ownCoins").getAsLong()==30&&Economy.state(a).getAsJsonObject("trade").get("otherCoins").getAsLong()==5,"Simultaneous partner edits both survive while invalidating consent");long old=revision(a);request(a,"confirm",0,old);request(b,"offer",5,old);long rev=revision(a);require(!Economy.state(a).getAsJsonObject("trade").get("accepted").getAsBoolean(),"Offer changes revoke both confirmations");request(a,"confirm",0,old);require(!Economy.state(a).getAsJsonObject("trade").get("accepted").getAsBoolean(),"Stale confirmation is ignored");
            request(a,"confirm",0,rev);require(Economy.balance(a)==100&&a.getInventory().getItem(0).is(Items.DIAMOND),"One consent cannot transfer items or money");request(b,"confirm",0,rev);require(Economy.balance(a)==75&&Economy.balance(b)==45&&a.getInventory().getItem(0).is(Items.EMERALD)&&b.getInventory().getItem(0).is(Items.DIAMOND),"Both consents atomically exchange exact coins and item stacks");request(b,"confirm",0,rev);require(Economy.balance(a)==75&&Economy.balance(b)==45,"Replay cannot repeat committed transfer");
            passed.accept("native trade item and coin conservation, consent invalidation, stale revision and replay rejection");
            String durableRevision=((CoinInventoryRevision)a).sum1rem0d$coinRevision();
            a.getInventory().setItem(0,new ItemStack(Items.DIAMOND,4));((CoinInventoryRevision)a).sum1rem0d$coinRevision("");Economy.recover(a);
            require(a.getInventory().getItem(0).is(Items.EMERALD)&&a.getInventory().getItem(0).getCount()==2&&Economy.balance(a)==75,"Durable post-transaction journal recovers old player inventory without minting currency");
            a.getInventory().setItem(1,new ItemStack(Items.STONE,3));Economy.recover(a);require(a.getInventory().getItem(1).getCount()==3,"Completed journal is idempotent and preserves later items");
            var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,a.registryAccess());a.saveWithoutId(output);
            var reloaded=actor(server,a.level(),"SumireCoinReload");reloaded.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,a.registryAccess(),output.buildResult()));
            require(((CoinInventoryRevision)reloaded).sum1rem0d$coinRevision().equals(durableRevision),"Vanilla player NBT persists transaction revision");
            var respawn=actor(server,a.level(),"SumireCoinRespawn");respawn.restoreFrom(a,false);require(((CoinInventoryRevision)respawn).sum1rem0d$coinRevision().equals(durableRevision),"Death respawn retains revision so old traded items never resurrect");reloaded.discard();respawn.discard();
            passed.accept("durable transaction recovery, idempotent replay, native player NBT and respawn revision preservation");
            var savedPath=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/sum1rem0d-state.json");var backup=savedPath.resolveSibling("coin-verification-"+UUID.randomUUID()+".json");
            java.nio.file.Files.move(savedPath,backup);
            try {java.nio.file.Files.createDirectory(savedPath);a.getInventory().setItem(2,new ItemStack(ModContent.coin(),1));request(a,"deposit",0,0);require(Economy.balance(a)==75&&a.getInventory().getItem(2).is(ModContent.coin()),"Failed durable save must retain both money and physical coin");}
            finally {java.nio.file.Files.delete(savedPath);java.nio.file.Files.move(backup,savedPath);a.getInventory().setItem(2,ItemStack.EMPTY);}
            passed.accept("actual filesystem write failure rolls back wallet balance, journal and inventory");

            Economy.startSession(a,b);Economy.select(a,0,false);a.closeContainer();require(!Economy.state(b).has("trade")&&a.getInventory().getItem(0).is(Items.EMERALD)&&Economy.balance(a)==75,"Closing either menu cancels without escrow loss");
            Economy.startSession(a,b);Economy.select(a,0,false);Economy.leaving(b);require(a.getInventory().getItem(0).is(Items.EMERALD)&&Economy.balance(a)==75,"Disconnect returns no escrow because items never left inventory");
            for(int i=0;i<36;i++)a.getInventory().setItem(i,new ItemStack(Items.STONE,64));Economy.startSession(a,b);Economy.select(b,0,false);request(a,"offer",10,revision(a));rev=revision(a);request(a,"confirm",0,rev);request(b,"confirm",0,rev);require(Economy.balance(a)==75&&Economy.balance(b)==45&&b.getInventory().getItem(0).is(Items.DIAMOND)&&a.getInventory().getItem(0).is(Items.STONE),"Full recipient refuses entire trade including currency");Economy.cancel(a,"fixture");
            passed.accept("native trade close, disconnect and full-recipient rollback preserve every item and coin");
            a.getInventory().clearContent();state.player(a).coins=0;a.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            var creative=config.snapshot();creative.getAsJsonObject("features").getAsJsonObject("economy").addProperty("allow_creative_wallet",false);config.update(creative,config.revision());
            a.getInventory().setItem(0,new ItemStack(ModContent.coin(),3));request(a,"deposit",0,0);require(Economy.balance(a)==0&&a.getInventory().getItem(0).getCount()==3,"Creative wallet is disabled by default without consuming coins");
            creative.getAsJsonObject("features").getAsJsonObject("economy").addProperty("allow_creative_wallet",true);config.update(creative,config.revision());request(a,"deposit",0,0);require(Economy.balance(a)==3&&a.getInventory().getItem(0).isEmpty(),"Configured creative deposit consumes physical coins");
            request(a,"withdraw",1,0);require(Economy.balance(a)==2&&a.getInventory().getItem(0).is(ModContent.coin()),"Configured creative withdrawal creates one physical coin");
            Economy.depositHand(a,net.minecraft.world.InteractionHand.MAIN_HAND);require(Economy.balance(a)==3&&a.getInventory().getItem(0).isEmpty(),"Configured creative right-click deposit conserves value");
            a.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);creative.getAsJsonObject("features").getAsJsonObject("economy").addProperty("allow_creative_wallet",false);config.update(creative,config.revision());a.getInventory().setItem(0,new ItemStack(ModContent.coin(),2));request(a,"deposit",0,0);require(Economy.balance(a)==5&&a.getInventory().getItem(0).isEmpty(),"Coins created as items are valid in survival regardless of provenance");
            passed.accept("creative wallet default denial, configured deposit/withdraw/right-click and survival item provenance");
        }finally{Economy.leaving(a);Economy.leaving(b);config.update(original,config.revision());state.restore(checkpoint);state.save();a.discard();b.discard();}
    }
}
