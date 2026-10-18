package com.sumirelabs.sum1rem0d.game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.core.particles.ParticleTypes;

/** Optional registry-backed whale enchantments and one-use next-hit bonus. */
public final class WhaleEnchantmentRules {
    private record Bonus(double percent, long expiresAt) {}
    private static final Map<UUID, Bonus> BONUSES = new HashMap<>();
    private static final Map<UUID, Long> LAST_SPOUT = new HashMap<>();
    private WhaleEnchantmentRules() {}

    public static boolean hasPendingBonus(Player player) { return BONUSES.containsKey(player.getUUID()); }

    public static int level(ServerLevel level, ItemStack stack) {
        if (!Rules.on("sperm_whale") || stack.isEmpty() || !Rules.matches(stack, Rules.list("sperm_whale", "target_items"))) return 0;
        var lookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var key = ResourceKey.create(Registries.ENCHANTMENT, ModContent.id("sperm_whale"));
        var enchantment = lookup.get(key);
        return enchantment.map(holder -> Math.max(0, Math.min(3, EnchantmentHelper.getItemEnchantmentLevel(holder, stack)))).orElse(0);
    }

    public static float consumeBonus(Player attacker, float damage) {
        if (StumbleStrikeRules.isApplyingExtraStrike()) return damage;
        if (!Rules.on("sperm_whale")) { BONUSES.remove(attacker.getUUID()); return damage; }
        if (damage <= 0) return damage;
        Bonus bonus = BONUSES.remove(attacker.getUUID());
        if (bonus == null) return damage;
        long now = attacker.level().getServer() == null ? Long.MAX_VALUE : attacker.level().getServer().overworld().getGameTime();
        return now <= bonus.expiresAt() ? damage * (float) (1 + bonus.percent() / 100.0) : damage;
    }

    public static void afterAttack(Player attacker, DamageSource source, boolean hit) {
        if (StumbleStrikeRules.isApplyingExtraStrike() || !hit || !Rules.on("sperm_whale") || !(attacker.level() instanceof ServerLevel level)
                || source.getEntity() != attacker || source.getDirectEntity() != attacker) return;
        ItemStack weapon = source.getWeaponItem();
        if (weapon == null || weapon.isEmpty()) weapon = attacker.getMainHandItem();
        int rank = level(level, weapon);
        if (rank <= 0) return;
        long now = level.getServer().overworld().getGameTime();
        double percent = Rules.n("sperm_whale", "level_" + rank + "_percent");
        long expires = now + Math.max(1, Rules.i("sperm_whale", "bonus_duration_ticks"));
        BONUSES.put(attacker.getUUID(), new Bonus(percent, expires));
        long last = LAST_SPOUT.getOrDefault(attacker.getUUID(), Long.MIN_VALUE / 2);
        long cooldown = Math.max(0, Rules.i("sperm_whale", "spout_cooldown_ticks"));
        if (now - last >= cooldown) {
            LAST_SPOUT.put(attacker.getUUID(), now);
            int count=Math.clamp(Rules.i("sperm_whale","spout_particles"),0,1024);
            double height=Rules.n("sperm_whale","spout_height"),width=Rules.n("sperm_whale","spout_width");
            if(count>0){
                int steps=Math.max(4,Math.min(12,(int)(height*4)));
                for(int i=0;i<steps;i++){
                    double t=(i+.5)/steps,spread=width*(.15+.85*t);
                    level.sendParticles(ParticleTypes.SPLASH,attacker.getX(),attacker.getY()+attacker.getBbHeight()+height*t,attacker.getZ(),Math.max(1,count/steps),spread,.08,spread,.06);
                }
                level.sendParticles(ParticleTypes.FALLING_WATER,attacker.getX(),attacker.getY()+attacker.getBbHeight()+height,attacker.getZ(),Math.max(1,count/4),width,.1,width,.015);
            }
        }
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        BONUSES.entrySet().removeIf(entry -> now > entry.getValue().expiresAt() || server.getPlayerList().getPlayer(entry.getKey()) == null);
        LAST_SPOUT.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        if (!Rules.on("sperm_whale")) BONUSES.clear();
    }
}
