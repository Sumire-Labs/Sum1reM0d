package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;

/** Narrow regressions for native item placement into water and vertical cable connectivity. */
final class PlacementRepairsVerification {
    static void verify(net.minecraft.server.MinecraftServer server,Consumer<String> passed)throws Exception{
        var config=Sum1reM0d.config();var saved=config.snapshot();var draft=saved.deepCopy();
        for(var entry:draft.getAsJsonObject("features").entrySet())entry.getValue().getAsJsonObject().addProperty("enabled",false);
        for(var id:List.of("glowstone_torch","optical_fiber"))draft.getAsJsonObject("features").getAsJsonObject(id).addProperty("enabled",true);
        config.update(draft,config.revision());
        var level=server.overworld();var actor=EconomyVerification.actor(server,level,"PlacementRepairs");
        try{
            int index=0;
            for(boolean wall:List.of(false,true))for(int waterLevel:new int[]{0,3,8}){
                var pos=new BlockPos(3540+index++*5,245,40);var support=wall?pos.south():pos.below();
                ServerVerification.place(level,support,Blocks.STONE.defaultBlockState());
                ServerVerification.place(level,pos,Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,waterLevel));
                var stack=new ItemStack(UtilityContent.item("glowstone_torch"));
                var hit=new BlockHitResult(Vec3.atCenterOf(support),wall?Direction.NORTH:Direction.UP,support,false);
                ((BlockItem)stack.getItem()).place(new BlockPlaceContext(actor,InteractionHand.MAIN_HAND,stack,hit));
                var state=level.getBlockState(pos);
                require(state.is(UtilityContent.block(wall?"wall_glowstone_torch":"glowstone_torch"))&&state.getValue(GlowstoneTorchBlock.WATERLOGGED)&&state.getFluidState().is(FluidTags.WATER)&&state.getLightEmission()==15,"Real torch item placement retains source/flow/falling water: wall="+wall+", level="+waterLevel);
            }
            passed.accept("Native floor/wall torch item placement keeps water and light in source, flowing and falling water");
            var pos=new BlockPos(3580,245,40);var source=pos.east();var torch=(GlowstoneTorchBlock)UtilityContent.block("glowstone_torch");
            ServerVerification.place(level,pos.below(),Blocks.STONE.defaultBlockState());ServerVerification.place(level,source.below(),Blocks.STONE.defaultBlockState());
            ServerVerification.place(level,pos,torch.defaultBlockState());ServerVerification.place(level,source,Blocks.WATER.defaultBlockState());
            for(var neighbor:List.of(source.north(),source.south(),source.east()))ServerVerification.place(level,neighbor,Blocks.STONE.defaultBlockState());
            torch.tick(level.getBlockState(pos),level,pos,level.getRandom());
            Fluids.WATER.tick(level,source,level.getBlockState(source),level.getFluidState(source));
            require(level.getBlockState(pos).is(torch)&&level.getBlockState(pos).getValue(GlowstoneTorchBlock.WATERLOGGED),"Settled neighboring water flows into an old dry torch without destroying it");
            var bucket=torch.pickupBlock(actor,level,pos,level.getBlockState(pos));
            require(bucket.is(Items.WATER_BUCKET)&&!level.getBlockState(pos).getValue(GlowstoneTorchBlock.WATERLOGGED)&&level.getBlockState(pos).getLightEmission()==15,"Water can still be removed with a bucket");
            require(!torch.canPlaceLiquid(actor,level,pos,level.getBlockState(pos),Fluids.LAVA)&&!torch.placeLiquid(level,pos,level.getBlockState(pos),Fluids.LAVA.defaultFluidState()),"Lava is not accepted as water");
            passed.accept("Existing dry torches accept natural water flow, preserve bucket behavior, and reject lava");
            var root=new BlockPos(3600,250,40);var fiber=(OpticalFiberBlock)UtilityContent.block("optical_fiber");
            ServerVerification.place(level,root.below(),Blocks.STONE.defaultBlockState());
            for(int i=0;i<3;i++)ServerVerification.place(level,root.above(i),fiber.defaultBlockState());
            ServerVerification.place(level,root.west(),Blocks.REDSTONE_BLOCK.defaultBlockState());
            OpticalFiberNetwork.invalidate(level);OpticalFiberNetwork.update(level,root);
            require(!level.getBlockState(root).getValue(OpticalFiberBlock.CONNECTIONS.get(Direction.DOWN))&&level.getBlockState(root).getValue(OpticalFiberBlock.CONNECTIONS.get(Direction.UP)),"Bottom cable connects up, not into plain ground");
            require(level.getBlockState(root.above()).getValue(OpticalFiberBlock.CONNECTIONS.get(Direction.UP))&&level.getBlockState(root.above()).getValue(OpticalFiberBlock.CONNECTIONS.get(Direction.DOWN)),"Middle cable connects in both directions");
            require(!level.getBlockState(root.above(2)).getValue(OpticalFiberBlock.CONNECTIONS.get(Direction.UP))&&level.getBlockState(root.above(2)).getValue(OpticalFiberBlock.CONNECTIONS.get(Direction.DOWN)),"Top cable connects down only");
            for(int i=0;i<3;i++)require(level.getBlockState(root.above(i)).getValue(OpticalFiberBlock.POWER)==15,"Vertical cable transmits full signal");
            level.setBlock(root.west(),Blocks.AIR.defaultBlockState(),3);OpticalFiberNetwork.invalidate(level);OpticalFiberNetwork.update(level,root);
            for(int i=0;i<3;i++)require(level.getBlockState(root.above(i)).getValue(OpticalFiberBlock.POWER)==0,"Vertical cable turns off after input removal");
            passed.accept("Vertical fiber state/collision connections and full-strength signal follow the actual neighboring cables");
        }finally{actor.discard();config.update(saved,config.revision());OpticalFiberNetwork.invalidate(level);}
    }
}
