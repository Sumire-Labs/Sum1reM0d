package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageLink;

/** Interaction-oriented native checks for shield clones, wind-charge fields, boots and signed chat display. */
final class AdditionalCombatVerification {
    private static final String CLONE_DEPTH = "sum1rem0d_shield_clone_depth_";
    private AdditionalCombatVerification() {}

    static void verify(MinecraftServer server, java.util.function.Consumer<String> passed) throws Exception {
        var store = Sum1reM0d.config();
        var original = store.snapshot();
        var draft = original.deepCopy();
        var defender = EconomyVerification.actor(server, server.overworld(), "CombatRuleDefender");
        var owner = EconomyVerification.actor(server, server.overworld(), "CombatRuleWindOwner");
        var spawned = new ArrayList<Entity>();
        var level = server.overworld();
        try {
            var features = draft.getAsJsonObject("features");
            for (var entry : features.entrySet()) entry.getValue().getAsJsonObject().addProperty("enabled", false);

            var shield = features.getAsJsonObject("shield_mob_clone");
            shield.addProperty("enabled", true); shield.addProperty("clone_limit_per_hit", 1); shield.addProperty("chain_depth", 1);
            var wind = features.getAsJsonObject("wind_black_hole");
            wind.addProperty("enabled", true); wind.addProperty("radius", 8); wind.addProperty("pull_strength", 0.35);
            wind.addProperty("duration_ticks", 100); wind.addProperty("max_targets", 1);
            var sneak = features.getAsJsonObject("sneak_speed");
            sneak.addProperty("enabled", true); sneak.addProperty("table_enabled", true); sneak.addProperty("boots_enabled", true);
            sneak.addProperty("per_level_speed_bonus", 0.15); sneak.addProperty("per_level_step_height", 1);
            var slime = features.getAsJsonObject("slime_mouth");
            slime.addProperty("enabled", true); slime.addProperty("forced_text", "mmmm"); slime.addProperty("duration_seconds", 60);
            store.update(draft, store.revision());

            var attacker = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, EntitySpawnReason.TRIGGERED);
            require(attacker != null, "Zombie source entity is constructible for shield defense");
            attacker.setPos(32, 245, 32);
            require(level.addFreshEntity(attacker), "Shield attacker fixture is admitted to the server level");
            spawned.add(attacker);
            ShieldCloneRules.blocked(defender, attacker, null);
            var clones = clonesIn(level);
            require(clones.size() == 1 && !clones.get(0).getUUID().equals(attacker.getUUID())
                    && clones.get(0).entityTags().contains(CLONE_DEPTH + "1"),
                    "One shield-defense event spawns one distinct first-generation mob clone");
            ShieldCloneRules.blocked(defender, attacker, null);
            clones = clonesIn(level);
            require(clones.size() == 2 && new HashSet<>(clones.stream().map(Entity::getUUID).toList()).size() == 2,
                    "Each independent shield defense respects the one-clone-per-hit limit and has a unique UUID");
            ShieldCloneRules.blocked(defender, clones.get(0), null);
            require(clonesIn(level).size() == 2, "Configured chain depth prevents a cloned mob from cloning itself recursively");
            shield.addProperty("enabled", false); store.update(draft, store.revision());
            ShieldCloneRules.blocked(defender, attacker, null);
            require(clonesIn(level).size() == 2, "Shield clone Config OFF stops additional clones");
            spawned.addAll(clones);
            passed.accept("Shield-defense mob clones: one per hit, unique UUIDs, chain-depth cap, and Config OFF");

            var first = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, EntitySpawnReason.TRIGGERED);
            var second = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, EntitySpawnReason.TRIGGERED);
            var outside = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, EntitySpawnReason.TRIGGERED);
            var charge = net.minecraft.world.entity.EntityTypes.WIND_CHARGE.create(level, EntitySpawnReason.TRIGGERED);
            require(first != null && second != null && outside != null && charge instanceof Projectile,
                    "Wind charge and living target fixtures are constructible");
            // Use the already-active spawn chunk used by the other native fixtures.
            Vec3 center = new Vec3(4, 245, 4);
            level.getChunkAt(new net.minecraft.core.BlockPos(4, 245, 4));
            first.setPos(center.x + 2, center.y, center.z);
            second.setPos(center.x + 3, center.y, center.z);
            outside.setPos(center.x + 10, center.y, center.z);
            first.setNoGravity(true); second.setNoGravity(true); outside.setNoGravity(true);
            charge.setPos(center);
            ((Projectile) charge).setOwner(owner);
            for (Entity entity : List.of(first, second, outside, charge)) {
                require(level.addFreshEntity(entity), "Wind-charge fixture admitted to the server level");
                spawned.add(entity);
            }
            // The verifier runs inside one server tick; advance fixtures once so the
            // entity-section query used by the production field sees them consistently.
            level.tickNonPassenger(first); level.tickNonPassenger(second); level.tickNonPassenger(outside);
            first.setDeltaMovement(Vec3.ZERO); second.setDeltaMovement(Vec3.ZERO); outside.setDeltaMovement(Vec3.ZERO);
            WindChargeBlackHole.impact((Projectile) charge);
            WindChargeBlackHole.impact((Projectile) charge);
            WindChargeBlackHole.tick(level);
            long pulled = List.of(first, second).stream().filter(entity -> entity.getDeltaMovement().lengthSqr() > 1.0E-8).count();
            double greatestPull = Math.max(first.getDeltaMovement().length(), second.getDeltaMovement().length());
            require(pulled == 1 && greatestPull <= 0.18 && outside.getDeltaMovement().lengthSqr() == 0,
                    "Black hole target/radius bounds: pulled=" + pulled + ", maxSpeed=" + greatestPull
                            + ", outsideSpeed=" + outside.getDeltaMovement().length());
            first.setDeltaMovement(Vec3.ZERO); second.setDeltaMovement(Vec3.ZERO); outside.setDeltaMovement(Vec3.ZERO);
            var offTarget = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, EntitySpawnReason.TRIGGERED);
            var offCharge = net.minecraft.world.entity.EntityTypes.WIND_CHARGE.create(level, EntitySpawnReason.TRIGGERED);
            require(offTarget != null && offCharge instanceof Projectile, "Config-OFF wind fixtures are constructible");
            level.getChunkAt(new net.minecraft.core.BlockPos(4, 245, 8));
            offTarget.setPos(center.x + 1, center.y, center.z); offCharge.setPos(center.add(0, 0, 4));
            offTarget.setNoGravity(true);
            ((Projectile) offCharge).setOwner(owner);
            require(level.addFreshEntity(offTarget) && level.addFreshEntity(offCharge), "Config-OFF wind fixtures are admitted");
            spawned.add(offTarget); spawned.add(offCharge);
            level.tickNonPassenger(offTarget); offTarget.setDeltaMovement(Vec3.ZERO);
            WindChargeBlackHole.impact((Projectile) offCharge);
            wind.addProperty("enabled", false); store.update(draft, store.revision());
            WindChargeBlackHole.tick(level);
            wind.addProperty("enabled", true); store.update(draft, store.revision());
            WindChargeBlackHole.tick(level);
            require(offTarget.getDeltaMovement().lengthSqr() == 0,
                    "Config OFF releases and removes active fields instead of resuming stale pull when re-enabled");
            wind.addProperty("enabled", false); store.update(draft, store.revision());
            WindChargeBlackHole.tick(level);
            passed.accept("Wind-charge black-hole radius/target bounds, duplicate impact protection and Config-OFF field release");

            var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var boots = new ItemStack(Items.IRON_BOOTS);
            require(tableOffersSwiftSneak(enchantments, boots, 100),
                    "Native enchanting-table candidate extraction offers Swift Sneak on boots when both toggles are enabled");
            sneak.addProperty("table_enabled", false); store.update(draft, store.revision());
            var swiftSneak = enchantments.getOrThrow(Enchantments.SWIFT_SNEAK).value();
            boolean tableFlag = Rules.b("sneak_speed", "table_enabled");
            boolean featureFlag = Rules.on("sneak_speed");
            boolean primary = swiftSneak.isPrimaryItem(boots);
            boolean supported = swiftSneak.isSupportedItem(boots);
            boolean offered = tableOffersSwiftSneak(enchantments, boots, 100);
            require(!offered, "Turning off table support removes Swift Sneak from native boot enchantment candidates"
                    + " [enabled=" + featureFlag + ", table=" + tableFlag + ", isPrimaryItem=" + primary
                    + ", isSupportedItem=" + supported + ", helperOffers=" + offered + "]");
            require(!tableOffersSwiftSneak(enchantments, new ItemStack(Items.IRON_LEGGINGS), 100),
                    "Turning off table support removes the vanilla leggings candidate too");
            sneak.addProperty("table_enabled", true); sneak.addProperty("boots_enabled", false); store.update(draft, store.revision());
            require(!tableOffersSwiftSneak(enchantments, boots, 100),
                    "Turning off boot support removes Swift Sneak from native boot enchantment candidates");
            require(tableOffersSwiftSneak(enchantments, new ItemStack(Items.IRON_LEGGINGS), 100),
                    "Boot-only settings preserve native Swift Sneak table candidates on leggings");
            sneak.addProperty("enabled", false); store.update(draft, store.revision());
            require(!tableOffersSwiftSneak(enchantments, new ItemStack(Items.IRON_LEGGINGS), 100),
                    "Turning off the whole feature removes Swift Sneak from all native table candidates");
            sneak.addProperty("enabled", true);
            sneak.addProperty("boots_enabled", true); store.update(draft, store.revision());
            passed.accept("Native enchanting-table Swift Sneak tag and global/table/boot Config gating with leggings baseline");
            boots.enchant(enchantments.getOrThrow(Enchantments.SWIFT_SNEAK), 1);
            defender.setItemSlot(EquipmentSlot.FEET, boots);
            SneakSpeedContent.tick(defender);
            var speed = defender.getAttribute(Attributes.SNEAKING_SPEED);
            var step = defender.getAttribute(Attributes.STEP_HEIGHT);
            var speedId = ModContent.id("boot_sneak_speed");
            var stepId = ModContent.id("boot_sneak_step");
            var speedModifier = speed == null ? null : speed.getModifier(speedId);
            var stepModifier = step == null ? null : step.getModifier(stepId);
            require(speedModifier != null && stepModifier != null && speedModifier.amount() == 0.15
                    && stepModifier.amount() == 1,
                    "Swift Sneak level 1 on boots applies the configured 0.15 speed and one-block step modifier");
            boots.enchant(enchantments.getOrThrow(Enchantments.SWIFT_SNEAK), 2);
            defender.setItemSlot(EquipmentSlot.FEET, boots);
            SneakSpeedContent.tick(defender);
            speedModifier = speed.getModifier(speedId); stepModifier = step.getModifier(stepId);
            require(speedModifier != null && stepModifier != null && speedModifier.amount() == 0.3 && stepModifier.amount() == 2,
                    "Swift Sneak level 2 scales both boot modifiers by the enchantment level");
            sneak.addProperty("enabled", false); store.update(draft, store.revision());
            SneakSpeedContent.tick(defender);
            require(!speed.hasModifier(speedId) && !step.hasModifier(stepId), "Sneak-speed Config OFF removes transient boot modifiers");
            passed.accept("Swift Sneak boot level scaling and Config-OFF modifier cleanup");

            slime.addProperty("enabled", true); store.update(draft, store.revision());
            defender.addEffect(new MobEffectInstance(ModContent.effect("slime_mouth"), 1200));
            UUID session = UUID.randomUUID();
            var signature = new MessageSignature(new byte[256]);
            var signedBody = new SignedMessageBody("signed-original-body", Instant.EPOCH, 0, new LastSeenMessages(List.of()));
            var originalMessage = new PlayerChatMessage(new SignedMessageLink(0, defender.getUUID(), session), signature,
                    signedBody, null, FilterMask.PASS_THROUGH);
            var displayed = SlimeMouthContent.visibleChat(defender, originalMessage);
            require(displayed.hasSignature() && displayed.signature().equals(signature)
                    && displayed.signedBody().equals(signedBody) && displayed.signedContent().equals("signed-original-body")
                    && displayed.unsignedContent().equals(net.minecraft.network.chat.Component.literal("mmmm")),
                    "Muffled chat changes only unsigned recipient display while retaining the signed body and signature");
            slime.addProperty("enabled", false); store.update(draft, store.revision());
            require(SlimeMouthContent.visibleChat(defender, originalMessage) == originalMessage,
                    "Slime-mouth Config OFF passes the untouched signed chat message");
            passed.accept("Slime-mouth signed-message preservation, forced unsigned display and Config OFF");
        } finally {
            draft.getAsJsonObject("features").getAsJsonObject("wind_black_hole").addProperty("enabled", false);
            try { store.update(draft, store.revision()); } catch (Exception ignored) {}
            WindChargeBlackHole.tick(level);
            spawned.forEach(Entity::discard);
            for (Entity entity : clonesIn(level)) entity.discard();
            defender.discard(); owner.discard();
            store.update(original, store.revision());
        }
    }

    private static List<Mob> clonesIn(net.minecraft.server.level.ServerLevel level) {
        return ServerVerification.spawnedIn(level).stream().filter(Mob.class::isInstance).map(Mob.class::cast)
                .filter(mob -> mob.entityTags().stream().anyMatch(tag -> tag.startsWith(CLONE_DEPTH))).toList();
    }

    private static boolean tableOffersSwiftSneak(net.minecraft.core.Registry<net.minecraft.world.item.enchantment.Enchantment> enchantments,
            ItemStack stack, int power) {
        var tableEnchantments = enchantments.get(net.minecraft.tags.EnchantmentTags.IN_ENCHANTING_TABLE).orElseThrow();
        var swiftSneak = enchantments.getOrThrow(Enchantments.SWIFT_SNEAK);
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getAvailableEnchantmentResults(power, stack, tableEnchantments.stream())
                .stream().anyMatch(candidate -> candidate.enchantment().equals(swiftSneak));
    }
}
