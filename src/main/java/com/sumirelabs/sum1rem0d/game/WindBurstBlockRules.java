package com.sumirelabs.sum1rem0d.game;
import net.minecraft.world.entity.Entity;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Bounded block debris thrown by the mace's native Wind Burst enchantment. */
public final class WindBurstBlockRules {
    private static final TagKey<net.minecraft.world.level.block.Block> IMMUNE=TagKey.create(Registries.BLOCK,Identifier.withDefaultNamespace("blocks_wind_charge_explosions"));
    private WindBurstBlockRules() {}

    public static int blow(ServerLevel level,int enchantmentLevel,Player attacker,Vec3 center,ItemStack tool){
        if(!Rules.on("wind_burst_blocks"))return 0;
        double radius=Math.clamp(Rules.n("wind_burst_blocks","radius"),1,8);
        double radiusSquared=radius*radius;
        var origin=BlockPos.containing(center);
        int bound=(int)Math.ceil(radius);
        var candidates=new ArrayList<BlockPos>();
        for(var pos:BlockPos.betweenClosed(origin.offset(-bound,-bound,-bound),origin.offset(bound,bound,bound)))
            if(Vec3.atCenterOf(pos).distanceToSqr(center)<=radiusSquared)candidates.add(pos.immutable());
        candidates.sort(Comparator.comparingDouble(pos->Vec3.atCenterOf(pos).distanceToSqr(center)));
        int max=Math.clamp(Rules.i("wind_burst_blocks","maximum_blocks"),1,64),moved=0;
        double levelScale=1+Math.max(0,Math.min(3,enchantmentLevel)-1)*.15;double speed=Math.clamp(Rules.n("wind_burst_blocks","throw_speed"),0,5)*levelScale,lift=Math.clamp(Rules.n("wind_burst_blocks","vertical_speed"),0,3)*levelScale;
        for(var pos:candidates){
            if(moved>=max)break;if(!level.hasChunkAt(pos))continue;
            BlockState state=level.getBlockState(pos);
            if(state.isAir()||state.hasBlockEntity()||state.is(IMMUNE)||!state.getFluidState().isEmpty()||state.getDestroySpeed(level,pos)<0)continue;
            var drops=Block.getDrops(state,level,pos,null,attacker,tool);
            if(drops.isEmpty())continue;
            var spawned=new ArrayList<ItemEntity>();boolean accepted=true;
            Vec3 away=Vec3.atCenterOf(pos).subtract(center);if(away.lengthSqr()<1.0e-6)away=new Vec3(level.getRandom().nextDouble()-.5,.2,level.getRandom().nextDouble()-.5);away=away.normalize();
            for(ItemStack drop:drops){var entity=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,drop.copy());entity.setDeltaMovement(away.scale(speed).add(0,lift,0));if(!level.addFreshEntity(entity)){entity.discard();accepted=false;break;}spawned.add(entity);}
            if(!accepted||!level.destroyBlock(pos,false)){spawned.forEach(Entity::discard);continue;}
            moved++;
        }
        return moved;
    }
}
