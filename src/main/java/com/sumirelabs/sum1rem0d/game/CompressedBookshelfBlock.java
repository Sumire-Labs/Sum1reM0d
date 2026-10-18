package com.sumirelabs.sum1rem0d.game;
import net.minecraft.world.level.block.Block;
public final class CompressedBookshelfBlock extends Block {
    private final int tier;
    //? if <26.3 {
    @Override public com.mojang.serialization.MapCodec<CompressedBookshelfBlock> codec(){return simpleCodec(p->new CompressedBookshelfBlock(tier,p));}
    //?}
    public CompressedBookshelfBlock(int tier,Properties props){super(props);this.tier=tier;}
    public int tier(){return tier;}
    //? if neoforge {
    /*@Override public float getEnchantPowerBonus(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.LevelReader level,net.minecraft.core.BlockPos pos){return BookshelfContent.power(tier);}
    *///?}
}
