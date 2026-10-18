package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractContainerMenu.class)
public abstract class FuelReservoirMenuMixin {
 @Inject(method="clicked",at=@At("HEAD"),cancellable=true)
 private void sum1rem0d$fuelClick(int index,int button,ContainerInput input,Player player,CallbackInfo ci){
  var menu=(AbstractContainerMenu)(Object)this;if(!(menu instanceof AbstractFurnaceMenu)||index!=1)return;
  var slot=menu.getSlot(1);var storage=((FurnaceContainerAccess)menu).sum1rem0d$fuelContainer();var fuel=storage.getItem(1);var carried=menu.getCarried();
  if(!Rules.on("fuel_stacks")&&fuel.getCount()<=fuel.getMaxStackSize())return;
  if(input==ContainerInput.PICKUP){
   if(carried.isEmpty()){if(!fuel.isEmpty())menu.setCarried(FuelReservoir.take(storage,button==1?(fuel.getCount()+1)/2:fuel.getCount()));}
   else if(slot.mayPlace(carried)&&(fuel.isEmpty()||ItemStack.isSameItemSameComponents(fuel,carried)))FuelReservoir.insert(storage,carried,button==1?1:carried.getCount());
   else if(fuel.getCount()<=fuel.getMaxStackSize())return;
   menu.broadcastChanges();ci.cancel();
  }else if(input==ContainerInput.SWAP&&fuel.getCount()>fuel.getMaxStackSize()){
   if(button>=0&&button<9&&player.getInventory().getItem(button).isEmpty())player.getInventory().setItem(button,FuelReservoir.take(storage,fuel.getMaxStackSize()));
   menu.broadcastChanges();ci.cancel();
  }else if(input==ContainerInput.THROW&&fuel.getCount()>fuel.getMaxStackSize()){
   var taken=FuelReservoir.take(storage,button==0?1:fuel.getMaxStackSize());if(!taken.isEmpty()&&player instanceof net.minecraft.server.level.ServerPlayer server)server.drop(taken,true,net.minecraft.util.Prediction.SERVER_ONLY);
   menu.broadcastChanges();ci.cancel();
  }
 }
}
