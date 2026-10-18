package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** One server-authoritative utility block with per-machine processing behavior. */
public final class AutomationMachineBlock extends BaseEntityBlock {
    public static final IntegerProperty WATER_LEVEL=IntegerProperty.create("washing_level",0,3);
    private final String kind;
    //? if <26.3 {
    private final com.mojang.serialization.MapCodec<AutomationMachineBlock> CODEC;
    @Override public com.mojang.serialization.MapCodec<AutomationMachineBlock> codec(){return CODEC;}
    //?}
    public AutomationMachineBlock(String kind,Properties props){super(props);this.kind=kind;
        //? if <26.3 {
        this.CODEC=simpleCodec(p->new AutomationMachineBlock(kind,p));
        //?}
        var state=stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH).setValue(WATER_LEVEL,0);registerDefaultState(state);}
    public String kind(){return kind;}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
        super.setPlacedBy(level,pos,state,placer,stack);
        if(kind.equals("auto_clicker")&&placer instanceof Player player&&level.getBlockEntity(pos) instanceof AutomationMachineEntity machine)machine.actorOwner(player.getUUID());
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> builder){builder.add(BlockStateProperties.HORIZONTAL_FACING,WATER_LEVEL);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,context.getHorizontalDirection().getOpposite());}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){var machine=new AutomationMachineEntity(pos,state);AutomationProcessing.loaded(machine);return machine;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(level.isClientSide()){AutomationMenu.recordClientTarget(level,pos);return InteractionResult.SUCCESS;}
        if(player instanceof ServerPlayer server)server.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inventory,who)->new AutomationMenu(id,inventory,(AutomationMachineEntity)level.getBlockEntity(pos)),Component.translatable("block.sum1rem0d."+kind)));
        return InteractionResult.CONSUME;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,net.minecraft.world.InteractionHand hand,BlockHitResult hit){
        if(level.isClientSide()&&(!kind.equals("washing_cauldron")||!stack.is(Items.WATER_BUCKET)||state.getValue(WATER_LEVEL)>=3))AutomationMenu.recordClientTarget(level,pos);
        if(!kind.equals("washing_cauldron")||!stack.is(Items.WATER_BUCKET)||state.getValue(WATER_LEVEL)>=3)return super.useItemOn(stack,state,level,pos,player,hand,hit);
        if(!level.isClientSide()){level.setBlock(pos,state.setValue(WATER_LEVEL,3),3);if(!player.getAbilities().instabuild)player.setItemInHand(hand,new ItemStack(Items.BUCKET));level.playSound(null,pos,net.minecraft.sounds.SoundEvents.BUCKET_EMPTY,net.minecraft.sounds.SoundSource.BLOCKS,1,1);}
        return InteractionResult.SUCCESS;
    }
}
