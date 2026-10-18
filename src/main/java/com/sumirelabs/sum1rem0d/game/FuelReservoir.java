package com.sumirelabs.sum1rem0d.game;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
/** Large count lives only in the furnace; cursor/player stacks retain vanilla limits. */
public final class FuelReservoir {
 private FuelReservoir(){}
 public static int capacity(ItemStack stack){return Rules.on("fuel_stacks")&&stack.getMaxStackSize()>1?Math.max(stack.getMaxStackSize(),Rules.i("fuel_stacks","maximum")):stack.getMaxStackSize();}
 public static int insert(Container container,ItemStack source,int requested){
  if(source.isEmpty()||requested<=0)return 0;var current=container.getItem(1);
  if(!current.isEmpty()&&!ItemStack.isSameItemSameComponents(current,source))return 0;
  int count=Math.min(Math.min(requested,source.getCount()),Math.max(0,capacity(source)-current.getCount()));if(count<=0)return 0;
  if(current.isEmpty()){int first=Math.min(count,source.getMaxStackSize());container.setItem(1,source.split(first));if(count>first){container.getItem(1).grow(count-first);source.shrink(count-first);}}
  else {current.grow(count);source.shrink(count);}
  container.setChanged();return count;
 }
 public static ItemStack take(Container container,int requested){var current=container.getItem(1);return container.removeItem(1,Math.min(requested,current.getMaxStackSize()));}
 public static void restore(Container container,ItemStack remaining){
  if(remaining.isEmpty())return;var current=container.getItem(1);if(current.isEmpty()){int count=remaining.getCount();container.setItem(1,remaining.copyWithCount(Math.min(count,remaining.getMaxStackSize())));container.getItem(1).setCount(count);}else if(ItemStack.isSameItemSameComponents(current,remaining))current.grow(remaining.getCount());else throw new IllegalStateException("Fuel changed during synchronous transfer");container.setChanged();
 }
}
