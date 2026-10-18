package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.InfestedBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(InfestedBlock.class)
public abstract class InfestedSwarmMixin {
 @Inject(method="spawnInfestation",at=@At("HEAD"),cancellable=true)
 private void sum1rem0d$swarm(ServerLevel level,BlockPos pos,CallbackInfo ci){if(Rules.on("infested_swarm")){WorldSurvivalRules.infestation(level,pos);ci.cancel();}}
}
