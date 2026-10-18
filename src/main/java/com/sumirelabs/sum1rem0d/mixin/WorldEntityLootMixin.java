package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.WorldEntityRules;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Mob.class)
public abstract class WorldEntityLootMixin {
 @Inject(method="dropFromLootTable",at=@At("HEAD"),cancellable=true)
 private void sum1rem0d$golem(ServerLevel level,DamageSource damage,boolean killedByPlayer,CallbackInfo ci){if(WorldEntityRules.golemLoot((Mob)(Object)this,level))ci.cancel();}
}
