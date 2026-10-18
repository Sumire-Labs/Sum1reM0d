package com.sumirelabs.sum1rem0d.game;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
final class DustWiringVerification {
 static void verify(MinecraftServer server,Consumer<String> passed)throws Exception{
  var config=Sum1reM0d.config();var before=config.snapshot();var level=server.overworld();var origin=new BlockPos(2020,240,40);
  try{
   var draft=before.deepCopy();var f=draft.getAsJsonObject("features");for(String name:List.of("glow_wire","sugar_wire","gunpowder_wire")){var opt=f.getAsJsonObject(name);opt.addProperty("enabled",true);opt.addProperty("opposite_redstone",false);opt.addProperty("inverted",false);}config.update(draft,config.revision());
   for(int power=0;power<=15;power++)require(ExtraContent.block("glow_wire").defaultBlockState().setValue(GlowWireBlock.POWER,power).getLightEmission()==power,"Glow light emission matches signal "+power);
   passed.accept("All 16 glow signal states emit matching light levels");
   String[] names={"glow_wire","sugar_wire","gunpowder_wire"};
   for(int row=0;row<3;row++)for(int x=0;x<3;x++){var p=origin.offset(x,0,row);ServerVerification.place(level,p.below(),Blocks.STONE.defaultBlockState());ServerVerification.place(level,p,ExtraContent.block(names[row]).defaultBlockState());}
   ServerVerification.place(level,origin.west(),Blocks.REDSTONE_BLOCK.defaultBlockState());
   for(int row=0;row<3;row++)GlowstoneWiring.update(level,origin.offset(0,0,row),true);
   for(int x=0;x<3;x++){require(level.getBlockState(origin.east(x)).getValue(GlowWireBlock.POWER)==15-x,"Glow signal attenuates "+x);for(int row=1;row<3;row++)require(level.getBlockState(origin.offset(x,0,row)).getValue(GlowWireBlock.POWER)==0,"Different dust family stays electrically isolated");}
   ServerVerification.place(level,origin.offset(-1,0,1),Blocks.REDSTONE_BLOCK.defaultBlockState());GlowstoneWiring.update(level,origin.south(),true);require(level.getBlockState(origin.offset(2,0,1)).getValue(GlowWireBlock.POWER)==13,"Sugar network carries its own decaying signal");
   f.getAsJsonObject("sugar_wire").addProperty("enabled",false);config.update(draft,config.revision());GlowstoneWiring.update(level,origin.south(),true);require(level.getBlockState(origin.south()).getValue(GlowWireBlock.POWER)==0,"Sugar Config OFF clears output without removing wire");
   var drops=Block.getDrops(level.getBlockState(origin.south()),level,origin.south(),null);require(drops.size()==1&&drops.getFirst().is(Items.SUGAR),"Sugar wire drops the original vanilla dust");
   for(var normal:List.of(net.minecraft.core.Direction.NORTH,net.minecraft.core.Direction.DOWN)){
    var start=origin.offset(0,0,normal==net.minecraft.core.Direction.NORTH?12:20);var block=(GlowWireBlock)ExtraContent.block("glow_wire");
    for(int i=0;i<3;i++){var pos=normal==net.minecraft.core.Direction.NORTH?start.above(i):start.east(i);ServerVerification.place(level,pos.relative(normal.getOpposite()),Blocks.STONE.defaultBlockState());var wire=block.defaultBlockState().setValue(GlowWireBlock.ATTACHMENT,normal);ServerVerification.place(level,pos,wire);require(wire.canSurvive(level,pos),"Wall/ceiling wire has actual supporting face");}
    var source=normal==net.minecraft.core.Direction.NORTH?start.below():start.west();ServerVerification.place(level,source,Blocks.REDSTONE_BLOCK.defaultBlockState());GlowstoneWiring.update(level,start,true);
    var last=normal==net.minecraft.core.Direction.NORTH?start.above(2):start.east(2);require(level.getBlockState(last).getValue(GlowWireBlock.POWER)==13,"Wall/ceiling graph uses attachment plane with attenuation: "+normal);
   }
   passed.accept("Three native dust networks attenuate independently, do not cross-connect, return their dust and respect live Config OFF");
  }finally{config.update(before,config.revision());}
 }
}
