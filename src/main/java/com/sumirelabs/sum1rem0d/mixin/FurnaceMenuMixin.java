package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(AbstractFurnaceMenu.class)
public abstract class FurnaceMenuMixin extends AbstractContainerMenu implements FurnaceContainerAccess {
    @Shadow @Final private Container container;
    @Override public Container sum1rem0d$fuelContainer(){return container;}
    protected FurnaceMenuMixin(MenuType<?> type, int id) { super(type, id); }
    @Inject(method = "isFuel", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$fuel(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (Rules.on("instant_smelting")) cir.setReturnValue(Rules.matches(stack, Rules.list("instant_smelting", "fuel_items")));
    }
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$bulk(Player player, int slot, CallbackInfoReturnable<ItemStack> cir) {
        if(slot==1&&container.getItem(1).getCount()>container.getItem(1).getMaxStackSize()){
            var fuel=container.getItem(1);int capacity=0;for(var current:player.getInventory().getNonEquipmentItems()){if(current.isEmpty())capacity+=fuel.getMaxStackSize();else if(ItemStack.isSameItemSameComponents(current,fuel))capacity+=Math.max(0,current.getMaxStackSize()-current.getCount());}
            int amount=Math.min(capacity,fuel.getCount());if(amount<=0){cir.setReturnValue(ItemStack.EMPTY);return;}
            var taken=container.removeItem(1,amount);var stats=taken.copyWithCount(Math.min(amount,taken.getMaxStackSize()));player.getInventory().add(taken);FuelReservoir.restore(container,taken);container.setChanged();broadcastChanges();cir.setReturnValue(stats);return;
        }
        if(slot>=3&&slot<slots.size()&&Rules.on("fuel_stacks")){
            var source=slots.get(slot).getItem();if(!source.isEmpty()&&slots.get(1).mayPlace(source)){
                var before=source.copy();if(FuelReservoir.insert(container,source,source.getCount())>0){slots.get(slot).setChanged();broadcastChanges();cir.setReturnValue(before);return;}
            }
        }
        if (!(player instanceof ServerPlayer) || slot != 2 || !Rules.on("instant_smelting") || !(container instanceof InstantFurnace instant)) return;
        ItemStack first = ItemStack.EMPTY;
        for (int batch = 0; batch < Rules.i("instant_smelting", "batch_per_tick"); batch++) {
            instant.sum1rem0d$refresh(); ItemStack result = container.getItem(2);
            if (result.isEmpty()) break;
            int capacity = 0;
            for (ItemStack current : player.getInventory().getNonEquipmentItems()) {
                if (current.isEmpty()) capacity += result.getMaxStackSize();
                else if (ItemStack.isSameItemSameComponents(current, result)) capacity += Math.max(0, result.getMaxStackSize() - current.getCount());
            }
            if (capacity < result.getCount()) break;
            ItemStack taken = container.removeItem(2, result.getCount()); if (taken.isEmpty()) break;
            ItemStack stats = taken.copy(); if (first.isEmpty()) first = stats.copy();
            player.getInventory().add(taken); slots.get(2).onTake(player, stats);
        }
        broadcastChanges(); cir.setReturnValue(first);
    }
}
