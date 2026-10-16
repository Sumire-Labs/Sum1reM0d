package com.sumirelabs.sum1rem0d.mixin;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import com.sumirelabs.sum1rem0d.game.NoChatReports;
import net.minecraft.network.protocol.status.ServerStatus;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Advertises the upstream NCR discovery field while preserving the original status codec. */
@Mixin(ServerStatus.class)
public abstract class NoChatReportsStatusMixin {
  @Shadow @Final @Mutable public static Codec<ServerStatus> CODEC;

  @Inject(method = "<clinit>", at = @At("TAIL"))
  private static void sum1rem0d$status(CallbackInfo ci) {
    var original = CODEC;
    CODEC =
        new Codec<>() {
          @Override
          public <T> DataResult<Pair<ServerStatus, T>> decode(DynamicOps<T> ops, T input) {
            return original.decode(ops, input);
          }

          @Override
          public <T> DataResult<T> encode(ServerStatus value, DynamicOps<T> ops, T prefix) {
            var result = original.encode(value, ops, prefix);
            if (NoChatReports.enabled() && !value.enforcesSecureChat())
              return result.flatMap(
                  encoded ->
                      ops.mergeToMap(
                          encoded,
                          ops.createString("preventsChatReports"),
                          ops.createBoolean(true)));
            return result;
          }
        };
  }
}
