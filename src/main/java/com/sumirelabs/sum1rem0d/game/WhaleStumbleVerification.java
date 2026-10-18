package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Confirms Stumble's recursive strike cannot consume or replace Whale's next independent attack bonus. */
final class WhaleStumbleVerification {
    private WhaleStumbleVerification() {}

    static void verify(MinecraftServer server, java.util.function.Consumer<String> passed) throws Exception {
        var store = Sum1reM0d.config();
        var original = store.snapshot();
        var draft = original.deepCopy();
        var attacker = EconomyVerification.actor(server, server.overworld(), "WhaleStumbleAttacker");
        var victim = EconomyVerification.actor(server, server.overworld(), "WhaleStumbleVictim");
        try {
            var features = draft.getAsJsonObject("features");
            for (var entry : features.entrySet()) entry.getValue().getAsJsonObject().addProperty("enabled", false);
            var whale = features.getAsJsonObject("sperm_whale");
            whale.addProperty("enabled", true);
            whale.addProperty("level_1_percent", 15);
            whale.addProperty("bonus_duration_ticks", 200);
            var stumble = features.getAsJsonObject("stumble_enchantment");
            stumble.addProperty("enabled", true);
            stumble.addProperty("extra_damage", 4);
            stumble.addProperty("maximum_level", 1);
            store.update(draft, store.revision());

            var lookup = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var whaleHolder = lookup.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, ModContent.id("sperm_whale")));
            var stumbleHolder = lookup.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, ModContent.id("stumble")));
            var sword = new ItemStack(Items.DIAMOND_SWORD);
            EnchantmentHelper.updateEnchantments(sword, mutable -> { mutable.set(whaleHolder, 1); mutable.set(stumbleHolder, 1); });
            attacker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
            victim.setHealth(20);
            victim.damageCooldownTime = 0;
            require(victim.hurtServer(server.overworld(), victim.damageSources().playerAttack(attacker), 8), "Combined enchantment attack is accepted");
            require(victim.getHealth() <= 8, "Stumble contributes one extra strike to the original attack");
            require(WhaleEnchantmentRules.hasPendingBonus(attacker), "Whale bonus survives the nested stumble strike");
            float nextHit = CombatItemRules.incomingDamage(victim, victim.damageSources().playerAttack(attacker), 12);
            require(Math.abs(nextHit - 13.8f) < 0.001f && !WhaleEnchantmentRules.hasPendingBonus(attacker),
                    "Only the next independent hit consumes Whale's configured level-one bonus");
            passed.accept("Whale and Stumble combination retains and consumes the next-hit bonus once");
        } finally {
            store.update(original, store.revision());
            attacker.discard();
            victim.discard();
        }
    }
}
