package com.sumirelabs.sum1rem0d.game;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
final class MachineMenuVerification {
 static void verify(net.minecraft.server.MinecraftServer server,Consumer<String> passed){
  var level=server.overworld();var pos=new BlockPos(2220,245,90);ServerVerification.place(level,pos,AutomationContent.block("auto_clicker").defaultBlockState());
  var machine=(AutomationMachineEntity)level.getBlockEntity(pos);var actor=EconomyVerification.actor(server,level,"ModeSync");actor.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+1.5);
  var menu=new AutomationMenu(41,actor.getInventory(),machine);var client=new AutomationMenu(41,actor.getInventory(),machine);boolean start=machine.leftClick();
  require(menu.slots.size()==37 && menu.playerInventoryLabelY()==50,"Clicker has one tool slot and inventory label above its actual first inventory slot");
  for(int i=0;i<2;i++){
   require(menu.clickMenuButton(actor,0),"Valid nearby player toggles clicker mode");require(menu.leftClickMode()==machine.leftClick() && machine.leftClick()!=(i==0?start:!start),"Mode changes in both menu and actual machine");
   client.setData(0,menu.leftClickMode()?1:0);require(client.leftClickMode()==menu.leftClickMode(),"Native menu data update synchronizes the client mode without block entity updates");
  }
  passed.accept("Clicker left/right toggles and native DataSlot sync");
 }
}
