package com.sumirelabs.sum1rem0d.game;

import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

/** Focused native checks for damage-enchantment pairing and chestplate Knockback. */
final class CombatEnchantPatchVerification {
    private CombatEnchantPatchVerification() {}

    static void verify(MinecraftServer server, Consumer<String> passed) throws Exception {
        var config = Sum1reM0d.config();
        var previous = config.snapshot();
        var draft = previous.deepCopy();
        var features = draft.getAsJsonObject("features");
        features.getAsJsonObject("compatible_damage_enchantments").addProperty("enabled", true);
        var chestConfig = features.getAsJsonObject("chestplate_knockback");
        chestConfig.addProperty("enabled", true);
        chestConfig.addProperty("strength_per_level", .8);
        chestConfig.addProperty("chance", 100.0);
        chestConfig.addProperty("affect_players", false);
        chestConfig.addProperty("affect_projectiles", false);
        config.update(draft, config.revision());
        var attacker = EconomyVerification.actor(server, server.overworld(), "EnchantAttacker");
        var victim = EconomyVerification.actor(server, server.overworld(), "EnchantVictim");
        try {
            verifyCompatibility(server, attacker, passed);
            verifyChestplateEnchanting(server, attacker, passed);
            verifyCounterAttack(server, attacker, victim, draft, config, passed);
        } finally {
            attacker.discard();
            victim.discard();
            config.update(previous, config.revision());
        }
    }

    private static void verifyCompatibility(MinecraftServer server, Player player, Consumer<String> passed) throws Exception {
        var lookup = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sharpness = lookup.getOrThrow(Enchantments.SHARPNESS);
        Holder<Enchantment> smite = lookup.getOrThrow(Enchantments.SMITE);
        Holder<Enchantment> bane = lookup.getOrThrow(Enchantments.BANE_OF_ARTHROPODS);
        Holder<Enchantment> depth = lookup.getOrThrow(Enchantments.DEPTH_STRIDER);
        Holder<Enchantment> frost = lookup.getOrThrow(Enchantments.FROST_WALKER);
        require(Enchantment.areCompatible(sharpness, smite) && Enchantment.areCompatible(smite, bane)
                && Enchantment.areCompatible(sharpness, bane), "Sharpness, Smite, and Bane pairings become compatible");
        require(!Enchantment.areCompatible(sharpness, sharpness)
                && EnchantmentHelper.isEnchantmentCompatible(List.of(sharpness), smite),
                "The native table helper allows distinct damage enchantments while rejecting a duplicate enchantment");
        require(!Enchantment.areCompatible(depth, frost), "Unrelated exclusive enchantments remain incompatible");

        var registry = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        EnchantmentHelper.updateEnchantments(sword, mutable -> mutable.set(smite, 1));
        var book = enchantedBook(sharpness, 1);
        var anvil = new AnvilMenu(1701, player.getInventory());
        anvil.getSlot(0).set(sword);
        anvil.getSlot(1).set(book);
        ItemStack result = anvil.getSlot(2).getItem();
        require(EnchantmentHelper.getItemEnchantmentLevel(sharpness, result) == 1
                && EnchantmentHelper.getItemEnchantmentLevel(smite, result) == 1,
                "Native anvil combines Sharpness with an existing Smite enchantment");
        anvil.removed(player);
        passed.accept("Sharpness/Smite/Bane compatibility is pair-limited across native table checks and anvil combining");

        var draft = Sum1reM0d.config().snapshot();
        var off = draft.deepCopy();
        off.getAsJsonObject("features").getAsJsonObject("compatible_damage_enchantments").addProperty("enabled", false);
        Sum1reM0d.config().update(off, Sum1reM0d.config().revision());
        require(!Enchantment.areCompatible(sharpness, smite), "Config OFF restores native damage-enchantment exclusivity");
        var offAnvil = new AnvilMenu(1702, player.getInventory());
        offAnvil.getSlot(0).set(sword);
        offAnvil.getSlot(1).set(book);
        require(EnchantmentHelper.getItemEnchantmentLevel(sharpness, offAnvil.getSlot(2).getItem()) == 0,
                "Config OFF prevents anvil transfer of the conflicting Sharpness book");
        offAnvil.removed(player);
        passed.accept("Damage-enchantment compatibility Config OFF restores native table/anvil rules");
        Sum1reM0d.config().update(draft, Sum1reM0d.config().revision());
    }

    private static void verifyChestplateEnchanting(MinecraftServer server, Player player, Consumer<String> passed) throws Exception {
        var registry = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> knockback = registry.getOrThrow(Enchantments.KNOCKBACK);
        ItemStack chestplate = new ItemStack(Items.IRON_CHESTPLATE);
        require(knockback.value().canEnchant(chestplate) && knockback.value().isPrimaryItem(chestplate)
                && EnchantmentHelper.getAvailableEnchantmentResults(30, chestplate, java.util.stream.Stream.of(knockback))
                        .stream().anyMatch(candidate -> candidate.enchantment().equals(knockback)),
                "Knockback is accepted as a native enchanting-table candidate on chest armor");
        var book = enchantedBook(knockback, 1);
        var anvil = new AnvilMenu(1703, player.getInventory());
        anvil.getSlot(0).set(chestplate);
        anvil.getSlot(1).set(book);
        require(EnchantmentHelper.getItemEnchantmentLevel(knockback, anvil.getSlot(2).getItem()) == 1,
                "Native anvil transfers Knockback onto a chestplate");
        anvil.removed(player);

        var off = Sum1reM0d.config().snapshot();
        off.getAsJsonObject("features").getAsJsonObject("chestplate_knockback").addProperty("enabled", false);
        Sum1reM0d.config().update(off, Sum1reM0d.config().revision());
        require(!knockback.value().canEnchant(chestplate) && !knockback.value().isPrimaryItem(chestplate),
                "Chestplate Config OFF restores native item eligibility");
        var offAnvil = new AnvilMenu(1704, player.getInventory());
        offAnvil.getSlot(0).set(chestplate);
        offAnvil.getSlot(1).set(book);
        require(EnchantmentHelper.getItemEnchantmentLevel(knockback, offAnvil.getSlot(2).getItem()) == 0,
                "Chestplate Config OFF prevents new anvil application");
        offAnvil.removed(player);
        passed.accept("Chestplate Knockback supports native table/anvil application and live OFF");
        var on = Sum1reM0d.config().snapshot();
        on.getAsJsonObject("features").getAsJsonObject("chestplate_knockback").addProperty("enabled", true);
        Sum1reM0d.config().update(on, Sum1reM0d.config().revision());
    }

    private static void verifyCounterAttack(MinecraftServer server, Player attacker, Player victim,
            com.google.gson.JsonObject draft, com.sumirelabs.sum1rem0d.config.ConfigStore config, Consumer<String> passed) throws Exception {
        ServerLevel level = server.overworld();
        Holder<Enchantment> knockback = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.KNOCKBACK);
        ItemStack chestplate = new ItemStack(Items.IRON_CHESTPLATE);
        EnchantmentHelper.updateEnchantments(chestplate, mutable -> mutable.set(knockback, 2));
        victim.setItemSlot(EquipmentSlot.CHEST, chestplate);
        victim.setHealth(20);
        victim.damageCooldownTime = 0;

        Mob mob = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        require(mob != null, "Zombie available for chestplate counterattack test");
        mob.setNoAi(true);
        mob.setNoGravity(true);
        mob.setPos(40.5, 240, 80.5);
        victim.setPos(38.5, 240, 80.5);
        require(level.addFreshEntity(mob), "Counterattack mob joins the native level");
        ServerVerification.spawned(mob);
        mob.setDeltaMovement(Vec3.ZERO);
        require(victim.hurtServer(level, victim.damageSources().mobAttack(mob), 3), "Real mob melee damage reaches the chest wearer");
        require(mob.getDeltaMovement().x > .05, "Chestplate Knockback pushes the successful melee attacker away [motion="+mob.getDeltaMovement()+"]");

        attacker.setPos(40.5, 240, 80.5);
        attacker.setDeltaMovement(Vec3.ZERO);
        victim.setHealth(20);
        victim.damageCooldownTime = 0;
        require(victim.hurtServer(level, victim.damageSources().playerAttack(attacker), 3), "Player melee fixture reaches the chest wearer");
        require(attacker.getDeltaMovement().lengthSqr() == 0, "Player attackers are not counterattacked by default");
        var playerOn = draft.deepCopy();
        playerOn.getAsJsonObject("features").getAsJsonObject("chestplate_knockback").addProperty("affect_players", true);
        config.update(playerOn, config.revision());
        victim.setHealth(20);
        victim.damageCooldownTime = 0;
        require(victim.hurtServer(level, victim.damageSources().playerAttack(attacker), 3)
                && attacker.getDeltaMovement().x > .05, "Player counterattack is enabled by its Config switch");
        var restored = playerOn.deepCopy();
        restored.getAsJsonObject("features").getAsJsonObject("chestplate_knockback").addProperty("affect_players", false);
        config.update(restored, config.revision());

        Arrow arrow = new Arrow(net.minecraft.world.entity.EntityTypes.ARROW, level);
        arrow.setNoGravity(true);
        arrow.setPos(40.5, 240, 80.5);
        arrow.setDeltaMovement(Vec3.ZERO);
        attacker.setDeltaMovement(Vec3.ZERO);
        victim.setHealth(20);
        victim.damageCooldownTime = 0;
        require(victim.hurtServer(level, victim.damageSources().arrow(arrow, attacker), 3), "Projectile fixture reaches the chest wearer");
        require(arrow.getDeltaMovement().lengthSqr() == 0 && attacker.getDeltaMovement().lengthSqr() == 0,
                "Projectile and shooter are not counterattacked by default");
        var projectileOn = restored.deepCopy();
        projectileOn.getAsJsonObject("features").getAsJsonObject("chestplate_knockback").addProperty("affect_projectiles", true);
        config.update(projectileOn, config.revision());
        victim.setHealth(20);
        victim.damageCooldownTime = 0;
        require(victim.hurtServer(level, victim.damageSources().arrow(arrow, attacker), 3)
                && arrow.getDeltaMovement().lengthSqr() > .05 && attacker.getDeltaMovement().lengthSqr() == 0,
                "Projectile counterattack targets the projectile when enabled, not its shooter");
        arrow.discard();

        config.update(restored, config.revision());
        chestConfig(restored, false);
        config.update(restored, config.revision());
        mob.setDeltaMovement(Vec3.ZERO);
        victim.setHealth(20);
        victim.damageCooldownTime = 0;
        require(victim.hurtServer(level, victim.damageSources().mobAttack(mob), 3)
                && mob.getDeltaMovement().lengthSqr() == 0,
                "Chestplate Config OFF disables retaliation while keeping its stored enchantment");
        require(EnchantmentHelper.getItemEnchantmentLevel(knockback, victim.getItemBySlot(EquipmentSlot.CHEST)) == 2,
                "Disabling chestplate retaliation preserves existing Knockback enchantment");
        passed.accept("Chestplate Knockback is limited to successful melee hits, with explicit player/projectile options and reversible OFF");
        mob.discard();
    }

    private static ItemStack enchantedBook(Holder<Enchantment> enchantment, int level) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantmentHelper.updateEnchantments(book, mutable -> mutable.set(enchantment, level));
        return book;
    }

    private static void chestConfig(com.google.gson.JsonObject config, boolean enabled) {
        config.getAsJsonObject("features").getAsJsonObject("chestplate_knockback").addProperty("enabled", enabled);
    }

    private static void require(boolean condition, String message) { ServerVerification.require(condition, message); }
}
