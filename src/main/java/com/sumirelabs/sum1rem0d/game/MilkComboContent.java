package com.sumirelabs.sum1rem0d.game;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

/** Consecutive in-game-day milk bonus. The streak is persisted on ServerPlayer. */
public final class MilkComboContent {
    private MilkComboContent() {}

    public static void afterMilk(ServerPlayer player) {
        if (!Rules.on("milk_combo") || !player.isAlive() || player.isSpectator()) return;
        var access = (MilkComboAccess) player;
        long day = Math.floorDiv(player.level().getServer().overworld().getOverworldClockTime(), 24000L);
        long last = access.sum1rem0d$lastMilkDay();
        int streak = access.sum1rem0d$milkStreak();
        if (day == last) {
            // Same-day drinks refresh the current tier but cannot farm additional tiers.
        } else if (day == last + 1) streak++;
        else streak = 1;
        access.sum1rem0d$lastMilkDay(day);
        access.sum1rem0d$milkStreak(Math.max(1, streak));

        var effect = FoodContent.randomEffect(player, "milk_combo", "buff_effects");
        if (effect == null) return;
        int every = Math.max(1, Rules.i("milk_combo", "days_per_level"));
        int level = Math.min(Math.max(1, Rules.i("milk_combo", "maximum_level")), 1 + (streak - 1) / every);
        player.addEffect(new MobEffectInstance(effect, Math.max(1, Rules.i("milk_combo", "duration_seconds")) * 20, level - 1));
    }
}
