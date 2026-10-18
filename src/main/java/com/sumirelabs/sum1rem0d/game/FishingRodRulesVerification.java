package com.sumirelabs.sum1rem0d.game;

import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Native checks for long casts, safe grapple release, lava reward delivery and Wind Burst debris. */
final class FishingRodRulesVerification {
    private FishingRodRulesVerification() {}

    static void verify(MinecraftServer server,Consumer<String> passed)throws Exception{
        var config=Sum1reM0d.config();var previous=config.snapshot();var draft=previous.deepCopy();var features=draft.getAsJsonObject("features");
        features.getAsJsonObject("fishing_cast").addProperty("enabled",true);
        features.getAsJsonObject("fishing_cast").addProperty("speed_multiplier",2.5);
        features.getAsJsonObject("fishing_cast").addProperty("vertical_multiplier",.75);
        features.getAsJsonObject("fishing_cast").addProperty("gravity_multiplier",.4);
        features.getAsJsonObject("fishing_grapple").addProperty("enabled",true);
        features.getAsJsonObject("fishing_grapple").addProperty("pull_strength",1.5);
        features.getAsJsonObject("fishing_grapple").addProperty("upward_boost",.35);
        features.getAsJsonObject("fishing_grapple").addProperty("maximum_range",32);
        features.getAsJsonObject("lava_fishing").addProperty("enabled",true);
        features.getAsJsonObject("lava_fishing").addProperty("minimum_wait_ticks",0);
        features.getAsJsonObject("lava_fishing").addProperty("maximum_wait_ticks",0);
        features.getAsJsonObject("wind_burst_blocks").addProperty("enabled",true);
        features.getAsJsonObject("wind_burst_blocks").addProperty("radius",2.5);
        features.getAsJsonObject("wind_burst_blocks").addProperty("maximum_blocks",1);
        config.update(draft,config.revision());
        try{
            verifyCast(server,server.overworld(),passed);
            verifyGrapple(server,server.overworld(),passed);
            verifyLavaFishing(server.overworld(),passed);
            verifyWindBurst(server.overworld(),config,passed);
        }finally{config.update(previous,config.revision());}
    }

    private static void verifyCast(MinecraftServer server,ServerLevel level,Consumer<String> passed){
        var player=EconomyVerification.actor(server,level,"SumireLongCast");
        try{
            var hook=new FishingHook(player,level,0,0);hook.setDeltaMovement(.4,.2,.3);Vec3 before=hook.getDeltaMovement();FishingRodRules.longCast(hook);Vec3 after=hook.getDeltaMovement();
            require(Math.abs(after.x-before.x*2.5)<1.0e-8&&Math.abs(after.z-before.z*2.5)<1.0e-8&&Math.abs(after.y-before.y*2.5*.75)<1.0e-8,"Long cast multiplies the horizontal throw speed and configured vertical component");
            require(Math.abs(FishingRodRules.gravity(hook,.03)-.012)<1.0e-8,"Long cast reduces bobber gravity");
            hook.discard();
            player.setPos(.5,245,.5);player.setYRot(0);player.setXRot(0);level.addNewPlayer(player);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.FISHING_ROD));
            var castResult=Items.FISHING_ROD.use(level,player,net.minecraft.world.InteractionHand.MAIN_HAND);var liveHook=player.fishing;
            require(castResult.consumesAction()&&liveHook!=null&&level.getEntity(liveHook.getUUID())==liveHook,"Native fishing-rod use creates and spawns its real bobber [result="+castResult+", hook="+liveHook+"]");
            double horizontal=Math.hypot(liveHook.getDeltaMovement().x,liveHook.getDeltaMovement().z);
            require(horizontal>1.2,"Real bobber launch passes through the native long-cast constructor hook [velocity="+liveHook.getDeltaMovement()+"]");
            //? if >=26.3 {
            require(Math.abs(liveHook.getGravity()-.012)<1.0e-8,"Native bobber gravity getter applies the configured multiplier");
            //?}
            require(!liveHook.isNoGravity()&&liveHook.level().getFluidState(liveHook.blockPosition()).isEmpty(),"Flight-gravity fixture starts in air with gravity enabled");
            int ticksBefore=liveHook.tickCount;var positionBefore=liveHook.position();var velocityBefore=liveHook.getDeltaMovement();
            level.tickNonPassenger(liveHook);
            require(liveHook.tickCount>ticksBefore&&level.getEntity(liveHook.getUUID())==liveHook,
                    "Real spawned fishing hook advances through the ServerLevel entity tick entry [ticks="+ticksBefore+"->"+liveHook.tickCount+", alive="+liveHook.isAlive()+", removed="+liveHook.isRemoved()+", owner="+(liveHook.getPlayerOwner()==player)+", inLevel="+(level.getEntity(liveHook.getUUID())==liveHook)+", pos="+positionBefore+"->"+liveHook.position()+", velocity="+velocityBefore+"->"+liveHook.getDeltaMovement()+"]");
            double expectedY=(velocityBefore.y-.012)*.92;
            require(Math.abs(liveHook.getDeltaMovement().y-expectedY)<1.0e-6,"Actual bobber flight applies gravity before native drag [before="+velocityBefore.y+", expected="+expectedY+", actual="+liveHook.getDeltaMovement().y+"]");
            liveHook.discard();passed.accept("Fishing rod native use, launched bobber entity tick, long-cast speed and configured gravity");
        }finally{player.discard();}
    }

    private static void verifyGrapple(MinecraftServer server,ServerLevel level,Consumer<String> passed){
        var player=EconomyVerification.actor(server,level,"SumireGrapple");var pos=findNonOceanFixture(level);var wet=pos.east();
        try{
            ServerVerification.place(level,pos,Blocks.STONE.defaultBlockState());
            ServerVerification.place(level,pos.above(),Blocks.STONE.defaultBlockState());
            ServerVerification.place(level,pos.above(2),Blocks.STONE.defaultBlockState());
            ServerVerification.place(level,wet,Blocks.WATER.defaultBlockState());
            player.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+4.5);level.addNewPlayer(player);
            var hook=new FishingHook(player,level,0,0);hook.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
            var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false);FishingRodRules.blockHit(hook,hit);
            Integer grappleResult=FishingRodRules.retrieve(hook,new ItemStack(Items.FISHING_ROD));
            require(Integer.valueOf(1).equals(grappleResult),"Solid block hook retrieves as a grapple [result="+grappleResult+", owner="+hook.getPlayerOwner()+", alive="+player.isAlive()+", inLevel="+(player.level()==level)+", anchor="+pos+", biome="+level.getBiome(pos).unwrapKey().map(Object::toString).orElse("unknown")+"]");
            require(player.getDeltaMovement().z<0&&player.getDeltaMovement().y>0,"Grapple pulls the player toward the anchor and upward");
            var waterHook=new FishingHook(player,level,0,0);waterHook.setPos(Vec3.atCenterOf(wet));
            FishingRodRules.blockHit(waterHook,new BlockHitResult(Vec3.atCenterOf(wet),Direction.NORTH,wet,false));
            require(FishingRodRules.retrieve(waterHook,new ItemStack(Items.FISHING_ROD))==null,"Water does not become a grapple anchor");
            waterHook.discard();

            player.setDeltaMovement(Vec3.ZERO);player.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+3.5);player.setYRot(180);player.setXRot(20);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.FISHING_ROD));
            var cast=Items.FISHING_ROD.use(level,player,net.minecraft.world.InteractionHand.MAIN_HAND);var liveHook=player.fishing;
            require(cast.consumesAction()&&liveHook!=null&&level.getEntity(liveHook.getUUID())==liveHook,"Native fishing rod use spawns a real owner-linked hook for grapple test");
            for(int tick=0;tick<6&&!liveHook.isRemoved();tick++)level.tickNonPassenger(liveHook);
            var hookPos=liveHook.position();int durabilityBefore=player.getMainHandItem().getDamageValue();
            var reel=Items.FISHING_ROD.use(level,player,net.minecraft.world.InteractionHand.MAIN_HAND);
            require(reel.consumesAction()&&liveHook.isRemoved()&&player.getDeltaMovement().z<0&&player.getDeltaMovement().y>0&&player.getMainHandItem().getDamageValue()==durabilityBefore+1,
                    "Native rod cast/ticks through block-anchor capture and real retrieve, pulling its owner and charging one durability [reel="+reel+", hookPos="+hookPos+", hookRemoved="+liveHook.isRemoved()+", playerVelocity="+player.getDeltaMovement()+", durability="+player.getMainHandItem().getDamageValue()+", anchor="+pos+", anchorState="+level.getBlockState(pos)+"]");
            passed.accept("Fishing hook block grapple pulls the owner and excludes water; real rod use/tick/collision/retrieve path works on the dedicated server");
        }finally{player.discard();}
    }

    private static void verifyLavaFishing(ServerLevel level,Consumer<String> passed){
        var pos=new BlockPos(12,240,904);ServerVerification.place(level,pos,Blocks.LAVA.defaultBlockState());
        var player=EconomyVerification.actor(level.getServer(),level,"SumireLavaFisher");
        try{
            player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);level.addNewPlayer(player);var hook=new FishingHook(player,level,0,0);hook.setPos(Vec3.atCenterOf(pos));FishingRodRules.tick(hook);
            int before=(int)ServerVerification.spawnedIn(level).stream().filter(ItemEntity.class::isInstance).count();
            require(Integer.valueOf(1).equals(FishingRodRules.retrieve(hook,new ItemStack(Items.FISHING_ROD))),"Ready lava fishing consumes the bobber and gives special loot");
            var after=ServerVerification.spawnedIn(level).stream().filter(ItemEntity.class::isInstance).map(ItemEntity.class::cast).toList();
            require(after.size()>before&&after.stream().anyMatch(entity->List.of(Items.MAGMA_CREAM,Items.GOLD_NUGGET,Items.BLAZE_POWDER,Items.QUARTZ,Items.NETHER_WART,Items.GHAST_TEAR,Items.NETHERITE_SCRAP).contains(entity.getItem().getItem())),"Lava fishing emits one configured special item entity");
            passed.accept("Lava fishing wait, server loot and bobber consumption");
        }finally{player.discard();}
    }

    private static void verifyWindBurst(ServerLevel level,com.sumirelabs.sum1rem0d.config.ConfigStore config,Consumer<String> passed)throws Exception{
        var center=new BlockPos(12,240,908);var target=center.east();ServerVerification.place(level,target,Blocks.STONE.defaultBlockState());
        int removed=WindBurstBlockRules.blow(level,1,null,Vec3.atCenterOf(center),new ItemStack(Items.MACE));
        require(removed==1&&level.getBlockState(target).isAir(),"Wind Burst releases bounded nearby block debris");
        require(ServerVerification.spawnedIn(level).stream().filter(ItemEntity.class::isInstance).map(ItemEntity.class::cast).anyMatch(entity->entity.position().distanceToSqr(Vec3.atCenterOf(target))<=4&&entity.getItem().is(Items.COBBLESTONE)),"Wind Burst throws the block loot instead of deleting it");
        var off=config.snapshot();off.getAsJsonObject("features").getAsJsonObject("wind_burst_blocks").addProperty("enabled",false);config.update(off,config.revision());
        ServerVerification.place(level,target,Blocks.STONE.defaultBlockState());require(WindBurstBlockRules.blow(level,1,null,Vec3.atCenterOf(center),new ItemStack(Items.MACE))==0&&level.getBlockState(target).is(Blocks.STONE),"Wind Burst OFF leaves surrounding blocks unchanged");
        passed.accept("Wind Burst bounded block drops, thrown loot and live OFF");
    }

    private static BlockPos findNonOceanFixture(ServerLevel level){
        var spawn=level.getRespawnData().pos();
        for(int radius=0;radius<=64;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(radius>0&&Math.abs(dx)!=radius&&Math.abs(dz)!=radius)continue;
            var pos=new BlockPos(spawn.getX()+dx,240,spawn.getZ()+dz);
            if(!level.getBiome(pos).is(net.minecraft.tags.BiomeTags.IS_OCEAN))return pos;
        }
        throw new IllegalStateException("No non-ocean biome found for grapple verification");
    }

    private static void require(boolean condition,String message){ServerVerification.require(condition,message);}
}
