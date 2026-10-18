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
    private record Pending(java.lang.ref.WeakReference<net.minecraft.server.MinecraftServer> server,
                           net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                           java.util.UUID attacker, java.util.UUID victim, long due, float damage) {}
    private static final java.util.List<Pending> PENDING = new java.util.ArrayList<>();
    private StumbleStrikeRules() {}

    public static void tick(net.minecraft.server.MinecraftServer server) {
        if(!Rules.on("stumble_enchantment")){PENDING.clear();return;}
        long now=server.overworld().getGameTime();
        for(var iterator=PENDING.iterator();iterator.hasNext();){
            var pending=iterator.next();
            if(pending.server().get()!=server){iterator.remove();continue;}
            if(now<pending.due())continue;
            iterator.remove();
            var level=server.getLevel(pending.dimension());var attacker=server.getPlayerList().getPlayer(pending.attacker());
            if(level==null || attacker==null || !attacker.isAlive() || attacker.level()!=level)continue;
            var entity=level.getEntity(pending.victim());
            if(entity instanceof LivingEntity victim && victim.isAlive() && attacker.distanceToSqr(victim)<=64)
                strike(victim,level,level.damageSources().playerAttack(attacker),pending.damage());
        }
    }


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
        int delay=Math.clamp(Rules.i("stumble_enchantment","delay_ticks"),0,40);
        if(delay>0){
            PENDING.add(new Pending(new java.lang.ref.WeakReference<>(level.getServer()),level.dimension(),attacker.getUUID(),victim.getUUID(),level.getServer().overworld().getGameTime()+delay,extra));
        }else strike(victim,level,source,extra);
    }

    private static void strike(LivingEntity victim,ServerLevel level,DamageSource source,float extra){
        int cooldown = victim.damageCooldownTime;
        APPLYING.set(true);
        victim.damageCooldownTime = 0;
        try {
            boolean hit=victim.hurtServer(level, source, extra);
            if(hit && Rules.b("stumble_enchantment","hit_feedback")){
                level.playSound(null,victim.blockPosition(),net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG,net.minecraft.sounds.SoundSource.PLAYERS,.65F,1.1F);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,victim.getX(),victim.getY()+victim.getBbHeight()*.5,victim.getZ(),10,.2,.3,.2,.05);
            }
        }
        finally { victim.damageCooldownTime = cooldown; APPLYING.set(false); }
    }
}
