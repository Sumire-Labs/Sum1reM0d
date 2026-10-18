package com.sumirelabs.sum1rem0d.game;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** Invisible timer that triggers the configured cigarette drawback after its buff ends. */
public final class CigaretteDelayEffect extends MobEffect {
    public CigaretteDelayEffect() { super(MobEffectCategory.BENEFICIAL, 0xBDA36A); }
    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration == 1; }
    @Override public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity instanceof ServerPlayer player && Rules.on("cigarette")) {
            var effect = FoodContent.randomEffect(player, "cigarette", "debuff_effects");
            if (effect != null) player.addEffect(new MobEffectInstance(effect,
                    Math.max(1, Rules.i("cigarette", "debuff_seconds")) * 20,
                    Math.max(0, Rules.i("cigarette", "debuff_level") - 1)));
        }
        return true;
    }
}
