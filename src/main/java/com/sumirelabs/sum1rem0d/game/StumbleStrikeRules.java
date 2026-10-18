package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Safe optional lookup and recursion guarded follow-up strike for the Stumble enchantment. */
public final class StumbleStrikeRules {
    private static final ThreadLocal<Boolean> APPLYING = ThreadLocal.withInitial(() -> false);
    private StumbleStrikeRules() {}

    public static boolean isApplyingExtraStrike() { return APPLYING.get(); }

    public static void afterDamage(LivingEntity victim, ServerLevel level, DamageSource source, boolean hit) {
        if (APPLYING.get() || !hit || !victim.isAlive() || !Rules.on("stumble_enchantment")
                || !(source.getEntity() instanceof Player attacker) || source.getDirectEntity() != attacker) return;
        var weapon = source.getWeaponItem();
        if (weapon == null || weapon.isEmpty()) return;
        var key = ResourceKey.create(Registries.ENCHANTMENT, ModContent.id("stumble"));
        var enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key);
        if (enchantment.isEmpty()) return;
        int rank = EnchantmentHelper.getItemEnchantmentLevel(enchantment.get(), weapon);
        if (rank <= 0 || rank > Rules.i("stumble_enchantment", "maximum_level")) return;
        float extra = (float) (Rules.n("stumble_enchantment", "extra_damage") * rank);
        if (extra <= 0) return;
        int cooldown = victim.damageCooldownTime;
        APPLYING.set(true);
        victim.damageCooldownTime = 0;
        try { victim.hurtServer(level, source, extra); }
        finally { victim.damageCooldownTime = cooldown; APPLYING.set(false); }
    }
}
