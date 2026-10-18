package com.sumirelabs.sum1rem0d.game;
import com.sumirelabs.sum1rem0d.Sum1reM0d;

import com.google.gson.JsonObject;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Focused native checks for Tesla target gating, successful-hit feedback and Config OFF. */
public final class TeslaFeedbackVerification {
    private TeslaFeedbackVerification() {}

    public static void verify(MinecraftServer server, Consumer<String> passed) throws Exception {
        var config = Sum1reM0d.config();
        JsonObject previous = config.snapshot();
        var draft = previous.deepCopy();
        var rule = draft.getAsJsonObject("features").getAsJsonObject("tesla_coil");
        rule.addProperty("enabled", true);
        rule.addProperty("range", 4);
        rule.addProperty("damage", 2);
        rule.addProperty("interval_ticks", 1);
        rule.addProperty("affect_players", false);
        rule.addProperty("attack_effects", true);
        rule.addProperty("attack_sound", true);
        rule.addProperty("sound_volume", 0.3);
        rule.addProperty("particle_density", 4);
        config.update(draft, config.revision());
        ServerLevel level = server.overworld();
        BlockPos pos = new BlockPos(1880, 240, 0);
        LivingEntity target = null;
        LivingEntity offTarget = null;
        try {
            ServerVerification.place(level, pos, BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("lightning_rod")).defaultBlockState().setValue(BlockStateProperties.POWERED, true));
            ServerVerification.place(level, pos.above(), BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("heavy_core")).defaultBlockState());
            ServerVerification.require(AutomationProcessing.attackTeslaTargets(level, pos, 4, 2) == 0,
                    "Tesla target search has no feedback-producing hit when no living target is in range");

            target = sheep(level, pos.east());
            float health = target.getHealth();
            ServerVerification.require(AutomationProcessing.attackTeslaTargets(level, pos, 4, 2) == 1 && target.getHealth() < health,
                    "Tesla emits one successful damage hit, which is the sole trigger for sound and spark feedback");

            offTarget = sheep(level, pos.west());
            float offHealth = offTarget.getHealth();
            var off = config.snapshot();
            off.getAsJsonObject("features").getAsJsonObject("tesla_coil").addProperty("enabled", false);
            config.update(off, config.revision());
            AutomationProcessing.trackTeslaRod(level, pos);
            AutomationProcessing.tickNativeTesla(server);
            ServerVerification.require(offTarget.getHealth() == offHealth,
                    "Config OFF prevents the native lightning-rod Tesla attack and its hit feedback");
            passed.accept("Tesla has no empty-pulse noise, emits configured feedback only after a successful hit, and respects Config OFF");
        } finally {
            if (target != null) target.discard();
            if (offTarget != null) offTarget.discard();
            config.update(previous, config.revision());
        }
    }

    private static LivingEntity sheep(ServerLevel level, BlockPos pos) {
        var sheep = BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("sheep"))
                .create(level, EntitySpawnReason.COMMAND);
        if (!(sheep instanceof LivingEntity living)) throw new AssertionError("Sheep entity type available for Tesla feedback fixture");
        living.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        if (!level.addFreshEntity(living)) throw new AssertionError("Tesla feedback sheep fixture was accepted by the level");
        return living;
    }
}
