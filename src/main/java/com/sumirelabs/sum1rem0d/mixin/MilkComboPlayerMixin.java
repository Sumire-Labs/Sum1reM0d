package com.sumirelabs.sum1rem0d.mixin;

import com.mojang.serialization.Codec;
import com.sumirelabs.sum1rem0d.game.MilkComboAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class MilkComboPlayerMixin implements MilkComboAccess {
    @Unique private long sum1rem0d$lastMilkDay = Long.MIN_VALUE;
    @Unique private int sum1rem0d$milkStreak;
    @Override public long sum1rem0d$lastMilkDay() { return sum1rem0d$lastMilkDay; }
    @Override public void sum1rem0d$lastMilkDay(long day) { sum1rem0d$lastMilkDay = day; }
    @Override public int sum1rem0d$milkStreak() { return sum1rem0d$milkStreak; }
    @Override public void sum1rem0d$milkStreak(int streak) { sum1rem0d$milkStreak = Math.max(0, streak); }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void sum1rem0d$saveMilkCombo(ValueOutput output, CallbackInfo ci) {
        output.store("sum1rem0d_last_milk_day", Codec.LONG, sum1rem0d$lastMilkDay);
        output.store("sum1rem0d_milk_streak", Codec.INT, sum1rem0d$milkStreak);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void sum1rem0d$loadMilkCombo(ValueInput input, CallbackInfo ci) {
        sum1rem0d$lastMilkDay = input.read("sum1rem0d_last_milk_day", Codec.LONG).orElse(Long.MIN_VALUE);
        sum1rem0d$milkStreak = Math.max(0, input.read("sum1rem0d_milk_streak", Codec.INT).orElse(0));
    }

    @Inject(method = "restoreFrom", at = @At("RETURN"))
    private void sum1rem0d$restoreMilkCombo(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo ci) {
        var old = (MilkComboAccess) oldPlayer;
        sum1rem0d$lastMilkDay = old.sum1rem0d$lastMilkDay();
        sum1rem0d$milkStreak = old.sum1rem0d$milkStreak();
        var pendingCigarette = oldPlayer.getEffect(com.sumirelabs.sum1rem0d.game.ModContent.effect("cigarette_delay"));
        if (pendingCigarette != null) ((ServerPlayer) (Object) this).addEffect(new net.minecraft.world.effect.MobEffectInstance(pendingCigarette));
    }
}
