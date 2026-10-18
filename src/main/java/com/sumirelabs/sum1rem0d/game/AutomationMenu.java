package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Pattern slots for the auto-crafter and its single tool slot for the clicker. */
public final class AutomationMenu extends AbstractContainerMenu {
    private static ClientTargetHint clientTargetHint;
    public final AutomationMachineEntity machine;
    private int clickModeState;
    public AutomationMenu(int id,Inventory inventory){this(id,inventory,find(inventory));}
    public AutomationMenu(int id,Inventory inventory,AutomationMachineEntity machine){
        super(AutomationContent.MENU,id);this.machine=machine;
        clickModeState=machine!=null&&machine.leftClick()?1:0;
        int count=machine==null?0:machine.getContainerSize();int cols=count==1?1:3;int rows=count==1?1:3;
        for(int row=0;row<rows;row++)for(int col=0;col<cols;col++){int slot=row*cols+col;if(slot>=count)continue;addSlot(new Slot(machine,slot,62+col*18,18+row*18));}
        addStandardInventorySlots(inventory,8,count==1?62:84);
        addDataSlot(new DataSlot(){@Override public int get(){return clickModeState;}@Override public void set(int value){clickModeState=value==0?0:1;}});
    }
    public boolean leftClickMode(){return clickModeState!=0;}
    public int playerInventoryLabelY(){int firstPlayerSlot=machine==null?0:machine.getContainerSize();return slots.size()>firstPlayerSlot?slots.get(firstPlayerSlot).y-12:72;}
    public static void recordClientTarget(Level level,BlockPos pos){if(level.isClientSide())clientTargetHint=new ClientTargetHint(new java.lang.ref.WeakReference<>(level),pos.immutable(),System.nanoTime());}
    private static AutomationMachineEntity find(Inventory inventory){
        var hint=clientTargetHint;clientTargetHint=null;
        if(hint!=null&&hint.level.get()==inventory.player.level()&&System.nanoTime()-hint.recordedAtNanos<5_000_000_000L
                &&inventory.player.level().getBlockEntity(hint.pos) instanceof AutomationMachineEntity entity)return entity;
        var pos=inventory.player.blockPosition();for(int y=-2;y<=2;y++)for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)if(inventory.player.level().getBlockEntity(pos.offset(x,y,z)) instanceof AutomationMachineEntity entity)return entity;return null;
    }
    private record ClientTargetHint(java.lang.ref.WeakReference<Level> level,BlockPos pos,long recordedAtNanos){}
    @Override public boolean clickMenuButton(Player player,int button){if(button!=0||machine==null||!machine.kind().equals("auto_clicker")||!stillValid(player))return false;machine.toggleClickMode();clickModeState=machine.leftClick()?1:0;broadcastChanges();return true;}
    @Override public boolean stillValid(Player player){return machine==null?player.level().isClientSide():machine.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
}
