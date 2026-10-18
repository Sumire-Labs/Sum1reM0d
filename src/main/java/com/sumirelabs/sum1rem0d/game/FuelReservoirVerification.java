package com.sumirelabs.sum1rem0d.game;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.storage.TagValueInput;
import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
final class FuelReservoirVerification {
 static void verify(MinecraftServer server,Consumer<String> passed)throws Exception{
  var config=Sum1reM0d.config();var before=config.snapshot();var player=EconomyVerification.actor(server,server.overworld(),"SumireFuel512");
  try{
   var draft=before.deepCopy();var opt=draft.getAsJsonObject("features").getAsJsonObject("fuel_stacks");opt.addProperty("enabled",true);opt.addProperty("maximum",512);config.update(draft,config.revision());
   var pos=new BlockPos(2060,240,40);ServerVerification.place(server.overworld(),pos,Blocks.FURNACE.defaultBlockState());var furnace=(FurnaceBlockEntity)server.overworld().getBlockEntity(pos);var menu=new FurnaceMenu(1,player.getInventory(),furnace,new SimpleContainerData(4));
   for(int i=0;i<8;i++){menu.setCarried(new ItemStack(Items.COAL,64));menu.clicked(1,0,ContainerInput.PICKUP,player);require(menu.getCarried().isEmpty(),"Fuel click accepts whole stack "+i);}
   require(furnace.getItem(1).getCount()==512,"Actual furnace slot holds 512 without global item stack change");menu.setCarried(new ItemStack(Items.COAL,64));menu.clicked(1,0,ContainerInput.PICKUP,player);require(menu.getCarried().getCount()==64&&furnace.getItem(1).getCount()==512,"Full reservoir consumes no cursor fuel");menu.setCarried(ItemStack.EMPTY);
   var packet=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),server.registryAccess());try{ItemStack.STREAM_CODEC.encode(packet,furnace.getItem(1));var received=ItemStack.STREAM_CODEC.decode(packet);require(received.getCount()==512&&received.getMaxStackSize()==64,"Native slot packet roundtrip preserves large count with ordinary item components");}finally{packet.release();}
   var saved=furnace.saveWithFullMetadata(server.registryAccess());var loaded=(FurnaceBlockEntity)BlockEntity.loadStatic(pos,Blocks.FURNACE.defaultBlockState(),saved,server.registryAccess());require(loaded!=null&&loaded.getItem(1).getCount()==512&&furnace.getItem(1).getCount()==512,"Native codec save/load retains all 512 and does not mutate live slot");
   menu.clicked(1,0,ContainerInput.PICKUP,player);require(menu.getCarried().getCount()==64&&furnace.getItem(1).getCount()==448,"Cursor withdrawal is bounded to native maximum");menu.setCarried(ItemStack.EMPTY);
   var hopperFuel=new ItemStack(Items.COAL,64);var left=HopperBlockEntity.addItem(null,furnace,hopperFuel,Direction.EAST);require(left.isEmpty()&&furnace.getItem(1).getCount()==512,"Native hopper reinserts into expanded fuel slot");
   opt.addProperty("enabled",false);config.update(draft,config.revision());require(furnace.getItem(1).getCount()==512,"Config OFF preserves saved over-capacity fuel");
   player.getInventory().clearContent();menu.quickMoveStack(player,1);int total=0;for(var item:player.getInventory().getNonEquipmentItems()){require(item.getCount()<=item.getMaxStackSize(),"Withdrawal never creates oversized inventory stacks");if(item.is(Items.COAL))total+=item.getCount();}require(total==512&&furnace.getItem(1).isEmpty(),"Shift withdrawal after OFF returns all fuel exactly once");
   passed.accept("Actual 512-fuel slot, click/full rollback, canonical save/load, native hopper input and vanilla-sized withdrawal after Config OFF");
  }finally{config.update(before,config.revision());player.discard();}
 }
}
