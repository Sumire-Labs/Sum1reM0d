package com.sumirelabs.sum1rem0d.game;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
/** Separate bounded graph; signal is complemented only at external inputs, then decays normally. */
public final class GlowstoneWiring {
 private static final ThreadLocal<Boolean> READING=ThreadLocal.withInitial(()->false),UPDATING=ThreadLocal.withInitial(()->false);
 private static boolean dirty;
 private static final Map<ServerLevel,Map<BlockPos,Long>> PROCESSED=new WeakHashMap<>();
 private GlowstoneWiring(){}
 public static boolean reading(){return READING.get();}public static boolean updating(){return UPDATING.get();}
 public static void changedDuringUpdate(Block source){if(!(source instanceof GlowWireBlock) && !(source instanceof RedstoneWireBlock))dirty=true;}
 public static void close(MinecraftServer server){PROCESSED.keySet().removeIf(l->l.getServer()==server);}
 public static void invalidate(MinecraftServer server){PROCESSED.forEach((l,m)->{if(l.getServer()==server)m.clear();});}
 public static void unloaded(ServerLevel level,net.minecraft.world.level.ChunkPos chunk){var map=PROCESSED.get(level);if(map!=null)map.keySet().removeIf(p->(p.getX()>>4)==(chunk.getMinBlockX()>>4) && (p.getZ()>>4)==(chunk.getMinBlockZ()>>4));}
 private static boolean external(BlockState state){return state.isSignalSource() && !(state.getBlock() instanceof GlowWireBlock) && !(state.getBlock() instanceof RedstoneWireBlock);}
 private static BlockState rawState(BlockState state){if(state.getBlock() instanceof RedstoneTorchBlock && RedstoneGameplay.part("torches"))return state.setValue(BlockStateProperties.LIT,!state.getValue(BlockStateProperties.LIT));if(state.getBlock() instanceof RepeaterBlock && RedstoneGameplay.part("repeaters"))return state.setValue(BlockStateProperties.POWERED,!state.getValue(BlockStateProperties.POWERED));return state;}
 public static Set<BlockPos> update(ServerLevel level,BlockPos start,boolean force){
  if(UPDATING.get() || !(level.getBlockState(start).getBlock() instanceof GlowWireBlock block))return Set.of();var processed=PROCESSED.computeIfAbsent(level,l->new HashMap<>());if(!force && processed.getOrDefault(start,Long.MIN_VALUE)==level.getGameTime())return Set.of();
  String feature=block.feature();
  UPDATING.set(true);dirty=false;
  try{
   var nodes=new LinkedHashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();queue.add(start.immutable());int limit=Rules.i(feature,"wire_limit");
   while(!queue.isEmpty() && nodes.size()<limit){var p=queue.removeFirst();if(nodes.contains(p) || !level.hasChunkAt(p) || !level.getBlockState(p).is(block))continue;nodes.add(p);for(var d:block.directions(level,p)){var n=block.connection(level,p,d);if(n!=null)queue.add(n.immutable());}}
   var powers=new HashMap<BlockPos,Integer>();var frontier=new PriorityQueue<Map.Entry<BlockPos,Integer>>((a,b)->Integer.compare(b.getValue(),a.getValue()));boolean hasInput=false;
   boolean enabled=Rules.on(feature),inverse=Rules.b(feature,"opposite_redstone")?!RedstoneGameplay.part("wire"):Rules.b(feature,"inverted");
   READING.set(true);
   try{if(enabled)for(var p:nodes){int strongest=0;boolean input=false;
    for(var d:Direction.values()){var n=p.relative(d);if(!level.hasChunkAt(n))continue;var state=level.getBlockState(n);if(state.getBlock() instanceof GlowWireBlock || state.getBlock() instanceof RedstoneWireBlock)continue;
     if(external(state)){input=true;var raw=rawState(state);strongest=Math.max(strongest,RedstoneGameplay.raw(()->raw.getSignal(level,n,d)));}
     else if(state.isRedstoneConductor(level,n)){boolean source=false;for(var side:Direction.values())if(level.hasChunkAt(n.relative(side)) && external(level.getBlockState(n.relative(side))))source=true;if(source){input=true;strongest=Math.max(strongest,RedstoneGameplay.raw(()->level.getDirectSignalTo(n)));}}
    }
    if(input){hasInput=true;int power=inverse?15-strongest:strongest;powers.put(p,power);if(power>0)frontier.add(Map.entry(p,power));}
   }}finally{READING.set(false);}
   if(enabled && inverse && !hasInput && !nodes.isEmpty()){var anchor=nodes.stream().min(Comparator.comparingLong(BlockPos::asLong)).orElseThrow();powers.put(anchor,15);frontier.add(Map.entry(anchor,15));}
   while(!frontier.isEmpty()){var e=frontier.remove();if(e.getValue()!=powers.getOrDefault(e.getKey(),0) || e.getValue()<=1)continue;for(var d:block.directions(level,e.getKey())){var n=block.connection(level,e.getKey(),d);if(n!=null && nodes.contains(n) && powers.getOrDefault(n,0)<e.getValue()-1){powers.put(n,e.getValue()-1);frontier.add(Map.entry(n,e.getValue()-1));}}}
   var changed=new ArrayList<BlockPos>();for(var p:nodes){var old=level.getBlockState(p);var next=block.connections(level,p,old).setValue(GlowWireBlock.POWER,enabled?powers.getOrDefault(p,0):0);processed.put(p,level.getGameTime());if(old!=next){level.setBlock(p,next,2);changed.add(p);}}
   for(var p:changed){level.updateNeighborsAt(p,block);for(var d:Direction.values())if(level.hasChunkAt(p.relative(d)))level.updateNeighborsAt(p.relative(d),block);}
   if(dirty && !nodes.isEmpty())level.scheduleTick(nodes.iterator().next(),block,1);return nodes;
  }finally{READING.set(false);UPDATING.set(false);}
 }
}
