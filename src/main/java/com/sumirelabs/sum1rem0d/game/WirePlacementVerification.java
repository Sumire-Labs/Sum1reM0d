package com.sumirelabs.sum1rem0d.game;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
final class WirePlacementVerification {
 static void verify(net.minecraft.server.MinecraftServer server,Consumer<String> passed)throws Exception{
  var store=Sum1reM0d.config();var saved=store.snapshot();var draft=saved.deepCopy();var level=server.overworld();var actor=EconomyVerification.actor(server,level,"WirePlacement");
  try{
   for(String name:List.of("glow_wire","sugar_wire","gunpowder_wire")){var feature=draft.getAsJsonObject("features").getAsJsonObject(name);feature.addProperty("enabled",true);feature.addProperty("wall_ceiling",true);}store.update(draft,store.revision());
   int index=0;
   for(String name:List.of("glow_wire","sugar_wire","gunpowder_wire"))for(Direction face:List.of(Direction.UP,Direction.NORTH,Direction.DOWN)){
    var block=(GlowWireBlock)ExtraContent.block(name);var pos=new BlockPos(2180+(index++)*4,245,90);var support=pos.relative(face.getOpposite());
    ServerVerification.place(level,support,Blocks.STONE.defaultBlockState());ServerVerification.place(level,pos,Blocks.AIR.defaultBlockState());
    actor.setPos(pos.getX()+.5,pos.getY()+2,pos.getZ()+.5);actor.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(block.dust(),3));
    var hit=new BlockHitResult(Vec3.atCenterOf(support).add(face.getStepX()*.5,face.getStepY()*.5,face.getStepZ()*.5),face,support,false);
    actor.getMainHandItem().useOn(new UseOnContext(actor,InteractionHand.MAIN_HAND,hit));
    require(level.getBlockState(pos).is(block)&&level.getBlockState(pos).getValue(GlowWireBlock.ATTACHMENT)==face,"Native dust useOn places "+name+" on "+face+" from air");
    require(actor.getMainHandItem().getCount()==2,"Placement consumes exactly one dust");
   }
   passed.accept("Three dust types place from air on floor, wall and ceiling through native ItemStack.useOn without attachment crashes");
  }finally{store.update(saved,store.revision());}
 }
}
