package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Pattern slots for the auto-crafter and its single tool slot for the clicker. */
public final class AutomationMenu extends AbstractContainerMenu {
    public final AutomationMachineEntity machine;
    public AutomationMenu(int id,Inventory inventory){this(id,inventory,find(inventory));}
    public AutomationMenu(int id,Inventory inventory,AutomationMachineEntity machine){
        super(AutomationContent.MENU,id);this.machine=machine;
        int count=machine==null?0:machine.getContainerSize();int cols=count==1?1:3;int rows=count==1?1:3;
        for(int row=0;row<rows;row++)for(int col=0;col<cols;col++){int slot=row*cols+col;if(slot>=count)continue;addSlot(new Slot(machine,slot,62+col*18,18+row*18));}
        addStandardInventorySlots(inventory,8,count==1?62:84);
    }
    private static AutomationMachineEntity find(Inventory inventory){var pos=inventory.player.blockPosition();for(int y=-2;y<=2;y++)for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)if(inventory.player.level().getBlockEntity(pos.offset(x,y,z)) instanceof AutomationMachineEntity entity)return entity;return null;}
    @Override public boolean clickMenuButton(Player player,int button){if(button!=0||machine==null||!machine.kind().equals("auto_clicker"))return false;machine.toggleClickMode();broadcastChanges();return true;}
    @Override public boolean stillValid(Player player){return machine==null?player.level().isClientSide():machine.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
}
