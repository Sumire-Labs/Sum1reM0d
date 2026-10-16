package com.sumirelabs.sum1rem0d.game;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
public final class TextileEntity extends DyedBlockEntity {
 public TextileEntity(String name,BlockPos pos,BlockState state){super(ExtraContent.type(name),pos,state);}
}
