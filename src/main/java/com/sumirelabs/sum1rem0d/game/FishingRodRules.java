package com.sumirelabs.sum1rem0d.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;

/** Server-authoritative long casts, block grappling, and lava fishing. */
public final class FishingRodRules {
    private static final Map<UUID, BlockPos> BLOCK_HOOKS = new HashMap<>();
    private static final Map<UUID, Long> LAVA_BITE_AT = new HashMap<>();
    private static final Set<UUID> LAVA_BITE_SHOWN = new HashSet<>();
    private FishingRodRules() {}

    public static void longCast(FishingHook hook) {
        if (!Rules.on("fishing_cast")) return;
        Vec3 velocity=hook.getDeltaMovement();
        double speed=Math.clamp(Rules.n("fishing_cast","speed_multiplier"),1,8);
        double vertical=Math.clamp(Rules.n("fishing_cast","vertical_multiplier"),0,2);
        hook.setDeltaMovement(velocity.x*speed,velocity.y*speed*vertical,velocity.z*speed);
    }

    public static double gravity(FishingHook hook,double vanilla) {
        return Rules.on("fishing_cast")?vanilla*Math.clamp(Rules.n("fishing_cast","gravity_multiplier"),0,1):vanilla;
    }

    public static void blockHit(FishingHook hook, BlockHitResult hit) {
        if(!Rules.on("fishing_grapple")||!(hook.level() instanceof ServerLevel level))return;
        var pos=hit.getBlockPos();if(!validGrappleBlock(level,pos))return;
        BLOCK_HOOKS.put(hook.getUUID(),pos.immutable());
    }

    private static boolean validGrappleBlock(ServerLevel level,BlockPos pos){
        if(!level.hasChunkAt(pos))return false;
        var state=level.getBlockState(pos);
        if(state.isAir()||state.hasBlockEntity()||state.getCollisionShape(level,pos).isEmpty()||state.getDestroySpeed(level,pos)<0)return false;
        if(state.getFluidState().is(FluidTags.WATER)||state.getFluidState().is(FluidTags.LAVA))return false;
        return !level.getBiome(pos).is(BiomeTags.IS_OCEAN);
    }

    /** null lets vanilla retrieve; 0 keeps an early lava cast; 1 consumes one rod durability. */
    public static Integer retrieve(FishingHook hook,ItemStack rod){
        if(!(hook.level() instanceof ServerLevel level))return null;
        if(!rod.is(Items.FISHING_ROD))return null;
        if(Rules.on("lava_fishing")&&inLava(hook))return lavaRetrieve(hook,rod,level);
        if(!Rules.on("fishing_grapple"))return null;
        var anchor=BLOCK_HOOKS.get(hook.getUUID());if(anchor==null)return null;
        var player=hook.getPlayerOwner();if(!(player instanceof ServerPlayer serverPlayer)||!serverPlayer.isAlive())return null;
        if(!validGrappleBlock(level,anchor))return null;
        Vec3 toward=Vec3.atCenterOf(anchor).subtract(serverPlayer.position());double distance=toward.length();
        if(distance<0.1||distance>Rules.n("fishing_grapple","maximum_range"))return null;
        double strength=Math.clamp(Rules.n("fishing_grapple","pull_strength"),0.1,8);
        double lift=Math.clamp(Rules.n("fishing_grapple","upward_boost"),0,2);
        Vec3 velocity=toward.normalize().scale(strength).add(0,lift,0);
        serverPlayer.setDeltaMovement(velocity);
        //? if >=26.3 {
        serverPlayer.syncPosition=true;
        //?} else {
        /*serverPlayer.hurtMarked=true;
        *///?}
        hook.discard();return 1;
    }

    private static Integer lavaRetrieve(FishingHook hook,ItemStack rod,ServerLevel level){
        var player=hook.getPlayerOwner();if(!(player instanceof ServerPlayer serverPlayer)||!serverPlayer.isAlive())return null;
        long now=level.getGameTime();long due=LAVA_BITE_AT.computeIfAbsent(hook.getUUID(),ignored->now+waitTicks(level));
        if(now<due)return 0;
        var loot=nextLavaLoot(level);if(loot.isEmpty())return 0;
        var item=new ItemEntity(level,hook.getX(),hook.getY(),hook.getZ(),loot);
        item.setDeltaMovement(serverPlayer.position().subtract(hook.position()).normalize().scale(.08).add(0,.12,0));
        if(!level.addFreshEntity(item)){item.discard();return 0;}
        hook.discard();return 1;
    }

    public static void tick(FishingHook hook){
        if(!(hook.level() instanceof ServerLevel level))return;
        if(!Rules.on("lava_fishing")||!inLava(hook)){LAVA_BITE_AT.remove(hook.getUUID());LAVA_BITE_SHOWN.remove(hook.getUUID());return;}
        long now=level.getGameTime();long due=LAVA_BITE_AT.computeIfAbsent(hook.getUUID(),ignored->now+waitTicks(level));
        if(now>=due&&LAVA_BITE_SHOWN.add(hook.getUUID())){
            level.playSound(null,hook.blockPosition(),net.minecraft.sounds.SoundEvents.LAVA_POP,net.minecraft.sounds.SoundSource.NEUTRAL,.7F,1.2F);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA,hook.getX(),hook.getY()+.15,hook.getZ(),4,.12,.04,.12,.02);
        }
    }

    private static int waitTicks(ServerLevel level){int low=Math.clamp(Rules.i("lava_fishing","minimum_wait_ticks"),0,72000);int high=Math.clamp(Rules.i("lava_fishing","maximum_wait_ticks"),low,72000);return high<=low?low:low+level.getRandom().nextInt(high-low+1);}
    private static boolean inLava(FishingHook hook){var pos=hook.blockPosition();return hook.level().hasChunkAt(pos)&&hook.level().getFluidState(pos).is(FluidTags.LAVA);}
    private static ItemStack nextLavaLoot(ServerLevel level){
        var valid=new ArrayList<net.minecraft.world.item.Item>();
        for(String raw:Rules.list("lava_fishing","loot_items")){
            if(raw.startsWith("#")){var id=net.minecraft.resources.Identifier.tryParse(raw.substring(1));if(id!=null)BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM,id)).forEach(holder->{if(holder.value()!=Items.AIR)valid.add(holder.value());});}
            else{var id=Rules.id(raw);if(id!=null)BuiltInRegistries.ITEM.getOptional(id).filter(item->item!=Items.AIR).ifPresent(valid::add);}
        }
        if(valid.isEmpty())return ItemStack.EMPTY;
        var item=valid.get(level.getRandom().nextInt(valid.size()));int max=Math.clamp(Rules.i("lava_fishing","maximum_item_count"),1,16);
        return new ItemStack(item,1+level.getRandom().nextInt(max));
    }

    public static void removed(FishingHook hook){BLOCK_HOOKS.remove(hook.getUUID());LAVA_BITE_AT.remove(hook.getUUID());LAVA_BITE_SHOWN.remove(hook.getUUID());}
}
