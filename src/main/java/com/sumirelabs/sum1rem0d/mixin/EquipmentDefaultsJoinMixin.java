package com.sumirelabs.sum1rem0d.mixin;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerPlayer.class)
public abstract class EquipmentDefaultsJoinMixin {
 @Inject(method="initInventoryMenu",at=@At("HEAD"))private void sum1rem0d$defaults(CallbackInfo ci){com.sumirelabs.sum1rem0d.game.EquipmentDefaults.send((ServerPlayer)(Object)this);}
}
