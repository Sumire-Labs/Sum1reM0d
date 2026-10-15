package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.game.Gameplay;
import com.sumirelabs.sum1rem0d.game.WorldState;
import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(MinecraftServer.class)
public abstract class ServerLifecycleMixin {
    @Inject(method = "loadLevel", at = @At("RETURN"))
    private void sum1rem0d$validateConfig(CallbackInfo ci) {
        if (com.sumirelabs.sum1rem0d.Sum1reM0d.initialized()) com.sumirelabs.sum1rem0d.game.RegisteredConfigValidation.validate(com.sumirelabs.sum1rem0d.Sum1reM0d.config().snapshot(), (MinecraftServer) (Object) this);
    }
    @Inject(method = "tickServer", at = @At("TAIL"))
    private void sum1rem0d$tick(BooleanSupplier time, CallbackInfo ci) { Gameplay.tick((MinecraftServer) (Object) this); com.sumirelabs.sum1rem0d.game.ServerVerification.tick((MinecraftServer) (Object) this); com.sumirelabs.sum1rem0d.game.ToastVerification.tick((MinecraftServer) (Object) this); com.sumirelabs.sum1rem0d.game.MultiplayerVerification.tick((MinecraftServer) (Object) this); }
    @Inject(method = "stopServer", at = @At("HEAD"))
    private void sum1rem0d$restoreVerification(CallbackInfo ci) { com.sumirelabs.sum1rem0d.game.MultiplayerVerification.stopping((MinecraftServer) (Object) this); com.sumirelabs.sum1rem0d.game.EconomyMultiplayerVerification.stopping((MinecraftServer)(Object)this); }
    @Inject(method = "saveAllChunks", at = @At("HEAD"))
    private void sum1rem0d$save(boolean silent, boolean flush, boolean force, CallbackInfoReturnable<Boolean> cir) { WorldState.saveIfLoaded((MinecraftServer) (Object) this); }
    @Inject(method = "stopServer", at = @At("TAIL"))
    private void sum1rem0d$stop(CallbackInfo ci) { WorldState.unload((MinecraftServer) (Object) this); }
}
