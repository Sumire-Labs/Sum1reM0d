package com.sumirelabs.sum1rem0d.game;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Durable machine settings and the auto-crafter/clicker one-use slots. */
public final class AutomationMachineEntity extends BlockEntity implements Container {
    private final List<ItemStack> items=new ArrayList<>(Collections.nCopies(9,ItemStack.EMPTY));
    private int cooldown;
    private boolean leftClick=true,lastPowered;
    private UUID actorOwner;
    private String lastClickResult="not-run";
    public AutomationMachineEntity(BlockPos pos,BlockState state){super(AutomationContent.type(),pos,state);}
    public String kind(){return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath();}
    public int cooldown(){return cooldown;}public void cooldown(int value){cooldown=Math.max(0,value);}
    public boolean leftClick(){return leftClick;}public void toggleClickMode(){leftClick=!leftClick;setChanged();}
    public boolean lastPowered(){return lastPowered;}public void lastPowered(boolean powered){lastPowered=powered;setChanged();}
    public String lastClickResult(){return lastClickResult;}public void lastClickResult(String value){lastClickResult=value==null?"null":value;}
    public UUID actorOwner(){return actorOwner;}public void actorOwner(UUID owner){if(!Objects.equals(actorOwner,owner)){actorOwner=owner;setChanged();}}
    @Override public void setRemoved(){AutomationProcessing.unloaded(this);super.setRemoved();}
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){super.preRemoveSideEffects(pos,state);if(level!=null)net.minecraft.world.Containers.dropContents(level,pos,this);}
    @Override public int getContainerSize(){return kind().equals("automated_crafting_table")?9:kind().equals("auto_clicker")?1:0;}
    @Override public boolean isEmpty(){for(int i=0;i<getContainerSize();i++)if(!items.get(i).isEmpty())return false;return true;}
    @Override public ItemStack getItem(int slot){return slot>=0&&slot<getContainerSize()?items.get(slot):ItemStack.EMPTY;}
    @Override public ItemStack removeItem(int slot,int count){if(slot<0||slot>=getContainerSize()||count<=0)return ItemStack.EMPTY;var current=items.get(slot);if(current.isEmpty())return ItemStack.EMPTY;var taken=current.split(count);setChanged();return taken;}
    @Override public ItemStack removeItemNoUpdate(int slot){if(slot<0||slot>=getContainerSize())return ItemStack.EMPTY;var result=items.get(slot);items.set(slot,ItemStack.EMPTY);return result;}
    @Override public void setItem(int slot,ItemStack stack){if(slot<0||slot>=getContainerSize())return;items.set(slot,stack.isEmpty()?ItemStack.EMPTY:stack.copyWithCount(Math.min(stack.getMaxStackSize(),stack.getCount())));setChanged();}
    @Override public boolean stillValid(Player player){return level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))<=64;}
    @Override public void clearContent(){for(int i=0;i<getContainerSize();i++)items.set(i,ItemStack.EMPTY);setChanged();}
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);out.putInt("cooldown",cooldown);out.putBoolean("leftClick",leftClick);out.putBoolean("lastPowered",lastPowered);
        out.storeNullable("actorOwner",UUIDUtil.CODEC,actorOwner);
        out.store("items",ItemStack.OPTIONAL_CODEC.listOf(),items.subList(0,getContainerSize()));
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);cooldown=Math.max(0,in.getIntOr("cooldown",0));leftClick=in.getBooleanOr("leftClick",true);lastPowered=in.getBooleanOr("lastPowered",false);
        actorOwner=in.read("actorOwner",UUIDUtil.CODEC).orElse(null);
        var stored=in.read("items",ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());for(int i=0;i<Math.min(stored.size(),9);i++)items.set(i,stored.get(i));
    }
    public List<ItemStack> pattern(){var result=new ArrayList<ItemStack>(9);for(int i=0;i<9;i++)result.add(items.get(i).copyWithCount(items.get(i).isEmpty()?0:1));return result;}
}
