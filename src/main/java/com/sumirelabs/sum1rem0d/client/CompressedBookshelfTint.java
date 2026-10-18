package com.sumirelabs.sum1rem0d.client;
import com.mojang.serialization.MapCodec;
import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
public final class CompressedBookshelfTint implements BlockTintSource,ItemTintSource {
    public static final CompressedBookshelfTint INSTANCE=new CompressedBookshelfTint();
    public static final MapCodec<CompressedBookshelfTint> CODEC=MapCodec.unit(INSTANCE);
    private CompressedBookshelfTint(){}
    @Override public int color(BlockState state){return BookshelfContent.color(((CompressedBookshelfBlock)state.getBlock()).tier());}
    @Override public int calculate(ItemStack stack,ClientLevel level,LivingEntity entity){return BookshelfContent.color(Math.max(0,BookshelfContent.tier(stack)));}
    @Override public MapCodec<? extends ItemTintSource> type(){return CODEC;}
}
