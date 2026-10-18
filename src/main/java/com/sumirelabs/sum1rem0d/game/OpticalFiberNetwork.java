package com.sumirelabs.sum1rem0d.game;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Solves external sources, never cached wire output. Fiber costs zero; dust-to-dust costs one. */
public final class OpticalFiberNetwork {
    private static final ThreadLocal<Boolean> READING=ThreadLocal.withInitial(()->false),UPDATING=ThreadLocal.withInitial(()->false);
    private static final Map<ServerLevel,Map<BlockPos,Long>> PROCESSED=new WeakHashMap<>();
    private record Power(BlockPos pos,int value){}
    private OpticalFiberNetwork(){}
    public static boolean reading(){return READING.get();}
    public static boolean updating(){return UPDATING.get();}
    public static void invalidate(ServerLevel level){PROCESSED.remove(level);}
    public static void close(net.minecraft.server.MinecraftServer server){PROCESSED.keySet().removeIf(l->l.getServer()==server);}
    public static void unloaded(ServerLevel level,net.minecraft.world.level.ChunkPos chunk){var map=PROCESSED.get(level);if(map!=null)map.keySet().removeIf(p->(p.getX()>>4)==(chunk.getMinBlockX()>>4)&&(p.getZ()>>4)==(chunk.getMinBlockZ()>>4));}
    private static boolean cable(BlockState state){return state.getBlock() instanceof OpticalFiberBlock;}
    private static boolean node(BlockState state){return cable(state)||state.is(Blocks.REDSTONE_WIRE);}
    private static List<BlockPos> neighbors(ServerLevel level,BlockPos pos){
        var result=new ArrayList<BlockPos>(); boolean fiber=cable(level.getBlockState(pos));
        for(var d:Direction.values()){
            var next=pos.relative(d);if(!level.hasChunkAt(next))continue;var other=level.getBlockState(next);
            if(fiber&&cable(other)||d.getAxis().isHorizontal()&&node(other)){result.add(next);continue;}
            if(fiber||!d.getAxis().isHorizontal())continue;
            if(level.hasChunkAt(next.above())&&!level.getBlockState(pos.above()).isRedstoneConductor(level,pos.above())&&level.getBlockState(next).isFaceSturdy(level,next,Direction.UP)&&level.getBlockState(next.above()).is(Blocks.REDSTONE_WIRE))result.add(next.above());
            if(level.hasChunkAt(next.below())&&!other.isRedstoneConductor(level,next)&&level.getBlockState(next.below()).is(Blocks.REDSTONE_WIRE))result.add(next.below());
        }
        return result;
    }
    private static int externalInput(ServerLevel level,BlockPos pos){
        int power=0;
        for(var d:Direction.values()){
            var next=pos.relative(d);if(!level.hasChunkAt(next))continue;var state=level.getBlockState(next);
            power=Math.max(power,state.getSignal(level,next,d));
            if(state.isRedstoneConductor(level,next))for(var side:Direction.values()){
                var source=next.relative(side);if(level.hasChunkAt(source))power=Math.max(power,level.getBlockState(source).getDirectSignal(level,source,side));
            }
        }
        return power;
    }
    public static Set<BlockPos> update(ServerLevel level,BlockPos start){
        if(updating()||!cable(level.getBlockState(start)))return Set.of();
        var processed=PROCESSED.computeIfAbsent(level,l->new HashMap<>());
        if(processed.getOrDefault(start,Long.MIN_VALUE)==level.getGameTime())return Set.of();
        UPDATING.set(true);
        try{
            int limit=Rules.i("optical_fiber","network_limit");
            var nodes=new LinkedHashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();queue.add(start.immutable());
            while(!queue.isEmpty()&&nodes.size()<=limit){var p=queue.removeFirst();if(nodes.contains(p)||!level.hasChunkAt(p)||!node(level.getBlockState(p)))continue;nodes.add(p);queue.addAll(neighbors(level,p));}
            boolean enabled=Rules.on("optical_fiber")&&nodes.size()<=limit;
            if(nodes.size()>limit)for(var pending:queue)if(!nodes.contains(pending)&&level.hasChunkAt(pending)&&cable(level.getBlockState(pending)))level.scheduleTick(pending,level.getBlockState(pending).getBlock(),1);
            var powers=new HashMap<BlockPos,Integer>();var seeds=new HashMap<BlockPos,Integer>();var frontier=new PriorityQueue<Power>(Comparator.comparingInt(Power::value).reversed());
            READING.set(true);
            try{for(var p:nodes){int input=externalInput(level,p);seeds.put(p,input);powers.put(p,input);if(input>0)frontier.add(new Power(p,input));}}
            finally{READING.set(false);}
            while(!frontier.isEmpty()){
                var current=frontier.remove();if(powers.get(current.pos())!=current.value())continue;
                for(var next:neighbors(level,current.pos()))if(nodes.contains(next)){
                    boolean fromFiber=cable(level.getBlockState(current.pos())),toFiber=cable(level.getBlockState(next));
                    // Disabled cable cannot bridge two ordinary dust networks.
                    if(!enabled&&(fromFiber||toFiber))continue;
                    int value=current.value()-(fromFiber||toFiber?0:1);
                    if(value>powers.getOrDefault(next,0)){powers.put(next,value);frontier.add(new Power(next,value));}
                }
            }
            boolean inverted=RedstoneGameplay.part("wire");
            if(enabled&&inverted){
                // Each fiber component takes the visible, inverted dust input, once.
                var unseen=new HashSet<BlockPos>();for(var p:nodes)if(cable(level.getBlockState(p)))unseen.add(p);
                while(!unseen.isEmpty()){
                    var group=new ArrayList<BlockPos>();queue.clear();queue.add(unseen.iterator().next());int strongest=0;
                    while(!queue.isEmpty()){var p=queue.removeFirst();if(!unseen.remove(p))continue;group.add(p);strongest=Math.max(strongest,seeds.getOrDefault(p,0));for(var n:neighbors(level,p))if(nodes.contains(n)){if(cable(level.getBlockState(n)))queue.add(n);else strongest=Math.max(strongest,15-powers.getOrDefault(n,0));}}
                    for(var p:group)powers.put(p,strongest);
                }
            }
            var changed=new ArrayList<BlockPos>();
            for(var p:nodes){var old=level.getBlockState(p);boolean fiber=cable(old);int power=fiber?(enabled?powers.getOrDefault(p,0):0):inverted?15-powers.getOrDefault(p,0):powers.getOrDefault(p,0);var next=old.setValue(OpticalFiberBlock.POWER,power);if(fiber){next=((OpticalFiberBlock)old.getBlock()).connections(level,p,next);processed.put(p,level.getGameTime());}if(old!=next){level.setBlock(p,next,2);changed.add(p);}}
            for(var p:changed){var block=level.getBlockState(p).getBlock();level.updateNeighborsAt(p,block);for(var d:Direction.values())if(level.hasChunkAt(p.relative(d)))level.updateNeighborsAt(p.relative(d),block);}
            return nodes;
        }finally{READING.set(false);UPDATING.set(false);}
    }
}
