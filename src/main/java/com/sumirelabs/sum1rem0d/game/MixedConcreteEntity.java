package com.sumirelabs.sum1rem0d.game;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
public final class MixedConcreteEntity extends BlockEntity {
 private volatile int color=0xFFFFFF;
 public MixedConcreteEntity(BlockPos pos,BlockState state){super(MixedConcrete.type(),pos,state);}
 public int color(){return color;}
 public void color(int value){color=value & 0xFFFFFF;setChanged();refresh();}
 private void refresh(){if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
 public ItemStack item(){return MixedConcrete.stack(color);}
 @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putInt("color",color);}
 @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);color=in.getIntOr("color",0xFFFFFF)&0xFFFFFF;if(level!=null && level.isClientSide())refresh();}
 @Override protected void applyImplicitComponents(DataComponentGetter components){super.applyImplicitComponents(components);color(components.getOrDefault(DataComponents.DYED_COLOR,new DyedItemColor(0xFFFFFF)).rgb());}
 @Override protected void collectImplicitComponents(DataComponentMap.Builder components){super.collectImplicitComponents(components);components.set(DataComponents.DYED_COLOR,new DyedItemColor(color));}
 @Override public void removeComponentsFromTag(ValueOutput out){out.discard("color");}
 @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
 @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup){return saveWithoutMetadata(lookup);}
}
