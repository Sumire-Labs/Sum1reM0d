package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
import com.google.gson.*;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;

/** Native round-trip checks for box delivery, migration, full-capacity retry and safe extraction. */
final class DeliveryBoxVerification {
    private DeliveryBoxVerification() {}
    private static void rejects(Runnable action,String message) { boolean rejected=false;try{action.run();}catch(RuntimeException failure){rejected=true;}require(rejected,message); }

    static void verify(MinecraftServer server, Consumer<String> passed) throws Exception {
        var config=Sum1reM0d.config();var original=config.snapshot();var state=WorldState.get(server);var checkpoint=state.checkpoint();
        var level=server.overworld();var player=EconomyVerification.actor(server,level,"SumireBoxCheck");
        var players=((com.sumirelabs.sum1rem0d.mixin.PlayerListVerificationAccess)server.getPlayerList()).sum1rem0d$verificationPlayers();
        var pos=new BlockPos(1984,240,32);var old=level.getBlockState(pos);
        try {
            var draft=original.deepCopy();var features=draft.getAsJsonObject("features");
            for(var feature:features.entrySet())feature.getValue().getAsJsonObject().addProperty("enabled",false);
            for(String id:List.of("economy","sumzon","sumzon_delivery_box"))features.getAsJsonObject(id).addProperty("enabled",true);
            features.getAsJsonObject("sumzon_delivery_box").addProperty("require_for_claim",true);
            features.getAsJsonObject("sumzon_delivery_box").addProperty("recipe_enabled",true);
            config.update(draft,config.revision());
            var formerId=UUID.randomUUID();var former=new WorldState.PlayerData();former.lastKnownName="FormerSumzonPlayer";state.playerRecords().put(formerId.toString(),former);
            var past=ShopServices.networkSnapshot(player,0).getAsJsonArray("pastPlayers");require(past.asList().stream().anyMatch(row->row.getAsJsonObject().get("id").getAsString().equals(formerId.toString())&&row.getAsJsonObject().get("name").getAsString().equals("FormerSumzonPlayer")),"Sumzon lists player heads only from saved participation records");
            require(ShopCatalog.price(ShopCatalog.playerHead(formerId,"FormerSumzonPlayer"))>=Rules.i("sumzon","player_head_price"),"Past-participant head uses the configured high price floor");
            require(ShopCatalog.price(new ItemStack(Items.IRON_INGOT))>=40&&ShopCatalog.price(new ItemStack(Items.DIAMOND_SWORD))>=1200,"Resource and recipe-value floors price common materials and crafted tools");
            var customPrice=draft.deepCopy();customPrice.getAsJsonObject("features").getAsJsonObject("sumzon").addProperty("prices","minecraft:iron_ingot=7");config.update(customPrice,config.revision());
            require(ShopCatalog.price(new ItemStack(Items.IRON_INGOT))==7,"Explicit per-item prices override generated resource valuation");config.update(draft,config.revision());
            var recipeInput=CraftingInput.of(3,3,List.of(new ItemStack(Items.IRON_INGOT),new ItemStack(Items.IRON_INGOT),new ItemStack(Items.IRON_INGOT),new ItemStack(Items.IRON_INGOT),new ItemStack(Items.CHEST),new ItemStack(Items.IRON_INGOT),new ItemStack(Items.IRON_INGOT),new ItemStack(Items.IRON_INGOT),new ItemStack(Items.IRON_INGOT)));
            var recipe=new ConfiguredCraftingRecipe("sumzon_delivery_box");require(recipe.matches(recipeInput,level)&&recipe.assemble(recipeInput).is(DeliveryBoxContent.item()),"Configurable delivery box recipe matches exact components");
            features.getAsJsonObject("sumzon_delivery_box").addProperty("recipe_enabled",false);config.update(draft,config.revision());require(!recipe.matches(recipeInput,level),"Turning off the delivery box recipe disables new crafting");features.getAsJsonObject("sumzon_delivery_box").addProperty("recipe_enabled",true);config.update(draft,config.revision());
            ServerVerification.place(level,pos.below(),Blocks.STONE.defaultBlockState());
            ServerVerification.place(level,pos,DeliveryBoxContent.block().defaultBlockState());
            var box=(DeliveryBoxEntity)level.getBlockEntity(pos);
            require(box!=null&&box.setOwner(player.getUUID(),player.getUUID()),"Placed box assigns a server-owned owner");
            player.setPos(pos.getX()+0.5,pos.getY()+1,pos.getZ()+0.5);
            var data=state.player(player);data.walletInitialized=true;data.coins=1000;data.deliveryItems=new JsonArray();for(int i=0;i<27;i++)data.deliveryItems.add(JsonNull.INSTANCE);
            var order=new JsonObject();order.addProperty("id",1);order.addProperty("due",state.serviceTime());order.addProperty("total",100);order.addProperty("boxDelivery",false);
            var line=new JsonObject();line.addProperty("item","minecraft:stone");line.addProperty("quantity",100);line.addProperty("remaining",100);line.addProperty("price",1);var lines=new JsonArray();lines.add(line);order.add("lines",lines);
            var service=new JsonObject();service.addProperty("revision",0);service.addProperty("sequence",1);var orders=new JsonArray();orders.add(order);service.add("orders",orders);data.services=service;
            players.add(player);

            ShopServices.tick(server);
            var afterFirst=ShopServices.snapshot(player).getAsJsonArray("orders").get(0).getAsJsonObject();
            require(ShopServices.remaining(afterFirst)==36&&box.getItem(0).is(Items.STONE)&&box.getItem(0).getCount()==64,"Legacy order migrates and first delivery leaves its unfit remainder");
            ShopServices.tick(server);
            require(ShopServices.remaining(ShopServices.snapshot(player).getAsJsonArray("orders").get(0).getAsJsonObject())==0&&box.getItem(1).is(Items.STONE)&&box.getItem(1).getCount()==36,"Repeated service tick delivers only the remaining quantity");
            ShopServices.tick(server);
            require(box.getItem(0).getCount()+box.getItem(1).getCount()==100,"Completed order never duplicates on later ticks");
            var reload=new WorldState(server).player(player.getUUID());
            require(reload.deliveryItems.get(0).getAsJsonObject().get("id").getAsString().equals("minecraft:stone"),"Mailbox items persist in the same durable world-state file as orders");

            player.containerMenu=new DeliveryBoxMenu(91,player.getInventory(),box);
            for(int i=0;i<player.getInventory().getContainerSize();i++)player.getInventory().setItem(i,new ItemStack(Items.DIRT,64));
            Economy.takeDeliveryBox(player,box,0);
            require(box.getItem(0).getCount()==64,"Full player inventory leaves a box stack untouched");
            data=state.player(player);
            player.getInventory().clearContent();Economy.takeDeliveryBox(player,box,0);
            require(box.getItem(0).isEmpty()&&player.getInventory().getItem(0).is(Items.STONE)&&player.getInventory().getItem(0).getCount()==64,"Extraction removes the box stack only when the same journal stores the player inventory");

            for(int i=0;i<27;i++)box.setItem(i,new ItemStack(Items.DIRT,64));
            var fullOrder=new JsonObject();fullOrder.addProperty("id",2);fullOrder.addProperty("due",state.serviceTime());fullOrder.addProperty("boxDelivery",true);
            var diamondLine=new JsonObject();diamondLine.addProperty("item","minecraft:diamond");diamondLine.addProperty("quantity",2);diamondLine.addProperty("remaining",2);diamondLine.addProperty("price",10);var fullLines=new JsonArray();fullLines.add(diamondLine);fullOrder.add("lines",fullLines);state.player(player).services.getAsJsonArray("orders").add(fullOrder);
            ShopServices.tick(server);
            var held=ShopServices.snapshot(player).getAsJsonArray("orders").get(1).getAsJsonObject();require(ShopServices.remaining(held)==2,"Full mailbox leaves the due order unchanged");
            player.containerMenu=player.inventoryMenu;DeliveryBoxAccess.open(player,pos,box);rejects(()->ShopServices.claim(player,2),"Full-box orders cannot bypass automatic delivery through the old claim button");
            player.containerMenu=new DeliveryBoxMenu(92,player.getInventory(),box);
            var disabled=draft.deepCopy();disabled.getAsJsonObject("features").getAsJsonObject("sumzon_delivery_box").addProperty("enabled",false);config.update(disabled,config.revision());
            box.setItem(0,ItemStack.EMPTY);ShopServices.tick(server);
            held=ShopServices.snapshot(player).getAsJsonArray("orders").get(1).getAsJsonObject();require(ShopServices.remaining(held)==0&&box.getItem(0).is(Items.DIAMOND)&&box.getItem(0).getCount()==2,"Making one slot available retries the saved remainder exactly once");
            passed.accept("Sumzon mailbox migration, partial/full-box retry, replay safety, durable slots and journaled extraction");
        } finally {
            players.remove(player);DeliveryBoxAccess.clear(player.getUUID());player.discard();level.setBlock(pos,old,3);level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),3);
            config.update(original,config.revision());state.restore(checkpoint);state.saveStrict();
        }
    }
}
