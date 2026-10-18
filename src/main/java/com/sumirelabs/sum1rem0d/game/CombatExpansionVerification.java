package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** One bounded native fixture for the inventory transfer and new combat rules. */
final class CombatExpansionVerification {
    static void verify(MinecraftServer server, Consumer<String> passed) throws Exception {
        var config = Sum1reM0d.config(); var saved = config.snapshot(); var draft = saved.deepCopy();
        var features = draft.getAsJsonObject("features");
        for (var entry : features.entrySet()) entry.getValue().getAsJsonObject().addProperty("enabled", false);
        for (String id : List.of("nether_slayer", "end_slayer", "expanded_weapon_enchantments", "elytra_enchanting", "death_box", "random_shulker_colors", "fast_shulker_bullets", "enderman_laser")) features.getAsJsonObject(id).addProperty("enabled", true);
        config.update(draft, config.revision());
        var level = server.overworld(); var actor = EconomyVerification.actor(server, level, "CombatExpansion");
        var oldKeep = level.getGameRules().get(GameRules.KEEP_INVENTORY);
        var pos = new BlockPos(2520, 245, 40);
        ServerVerification.place(level, pos, Blocks.AIR.defaultBlockState()); actor.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        try {
            var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var nether = registry.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, ModContent.id("nether_slayer")));
            var end = registry.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, ModContent.id("end_slayer")));
            var sharpness = registry.getOrThrow(Enchantments.SHARPNESS);
            for (var item : List.of(Items.MACE, Items.TRIDENT)) require(sharpness.value().canEnchant(new ItemStack(item)) && sharpness.value().isPrimaryItem(new ItemStack(item)), "Expanded weapon accepts Sharpness at anvil and table");
            var sword = new ItemStack(Items.DIAMOND_SWORD); sword.enchant(nether, 2); sword.enchant(end, 3);
            actor.setItemSlot(EquipmentSlot.MAINHAND, sword);
            var blaze = (net.minecraft.world.entity.Mob) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("blaze")).create(level, EntitySpawnReason.COMMAND);
            var endermite = (net.minecraft.world.entity.Mob) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("endermite")).create(level, EntitySpawnReason.COMMAND);
            require(DimensionCombatRules.damage(blaze, level, level.damageSources().playerAttack(actor), 4) == 9, "Nether Slayer adds species-specific damage");
            require(DimensionCombatRules.damage(endermite, level, level.damageSources().playerAttack(actor), 4) == 11.5f, "End Slayer adds species-specific damage");
            blaze.discard(); endermite.discard(); passed.accept("Dimension slayers register and apply level-scaled species damage; mace and trident accept damage enchantments");
            var table = new EnchantmentMenu(3000, actor.getInventory()); table.getSlot(0).set(new ItemStack(Items.ELYTRA));
            var elytra = table.getSlot(0).getItem();
            require(elytra.isEnchantable() && elytra.get(DataComponents.ENCHANTABLE) != null, "Elytra receives native enchanting power");
            for (var id : List.of(Enchantments.UNBREAKING, Enchantments.PROTECTION)) require(registry.getOrThrow(id).value().isPrimaryItem(elytra) && registry.getOrThrow(id).value().canEnchant(elytra), "Elytra accepts requested table/anvil enchantment");
            table.getSlot(0).set(ItemStack.EMPTY); passed.accept("Elytra is a native enchanting-table candidate for Unbreaking and Protection");
            actor.getInventory().clearContent(); actor.getInventory().setItem(0, sword); actor.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE)); actor.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            ((SecondOffhandAccess) actor).sum1rem0d$secondOffhand(new ItemStack(Items.GOLDEN_APPLE, 3));
            level.getGameRules().set(GameRules.KEEP_INVENTORY, true, server);
            require(!DeathBoxContent.capture(actor, level) && actor.getInventory().getItem(0).is(Items.DIAMOND_SWORD), "Keep inventory prevents box and inventory changes");
            level.getGameRules().set(GameRules.KEEP_INVENTORY, false, server);
            require(DeathBoxContent.capture(actor, level), "Death box capture succeeds");
            var box = (DeathBoxEntity) level.getBlockEntity(pos);
            require(box != null && box.contents().size() == 4 && actor.getInventory().isEmpty() && ((SecondOffhandAccess) actor).sum1rem0d$secondOffhand().isEmpty(), "Equipment and both offhands move exactly once");
            var stored = box.saveWithoutMetadata(level.registryAccess());
            var restored = new DeathBoxEntity(pos, level.getBlockState(pos)); restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), stored));
            require(restored.contents().size() == 4 && restored.contents().getFirst().isEnchanted(), "Box contents and enchantments survive native chunk serialization");
            var bounds = new net.minecraft.world.phys.AABB(pos).inflate(3); int before = ServerVerification.spawnedIn(level).stream().filter(e -> e instanceof net.minecraft.world.entity.item.ItemEntity && bounds.contains(e.position())).mapToInt(e -> ((net.minecraft.world.entity.item.ItemEntity)e).getItem().getCount()).sum();
            DeathBoxContent.release(level, pos); DeathBoxContent.release(level, pos);
            int after = ServerVerification.spawnedIn(level).stream().filter(e -> e instanceof net.minecraft.world.entity.item.ItemEntity && bounds.contains(e.position())).mapToInt(e -> ((net.minecraft.world.entity.item.ItemEntity)e).getItem().getCount()).sum();
            require(level.getBlockState(pos).isAir() && after - before == 6, "Box drops complete contents exactly once (before=" + before + ", after=" + after + ", air=" + level.getBlockState(pos).isAir() + ")");
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).forEach(net.minecraft.world.entity.Entity::discard);
            passed.accept("Death box respects keepInventory, captures armor/both offhands, persists enchantments and releases without duplication");
            var shulker = (Shulker) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("shulker")).create(level, EntitySpawnReason.NATURAL);
            shulker.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
            require(shulker.getVariant().isPresent(), "Spawned shulker receives synchronized dye variant"); shulker.discard();
            var bullet = (ShulkerBullet) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("shulker_bullet")).create(level, EntitySpawnReason.COMMAND);
            bullet.setPos(pos.getX() + .5, pos.getY() + 3, pos.getZ() + .5); bullet.setNoGravity(true); bullet.setDeltaMovement(new Vec3(.1, 0, 0));
            double startX = bullet.getX(); bullet.tick(); require(bullet.getX() - startX > .35 && bullet.getX() - startX < .5, "Fast bullet moves at configured multiplier without teleporting through collision sweep"); bullet.discard();
            passed.accept("Natural shulkers randomize their persistent color and bullets accelerate through native collision handling");
            var enderman = (net.minecraft.world.entity.Mob) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("enderman")).create(level, EntitySpawnReason.COMMAND);
            enderman.setPos(actor.getX() + 5, actor.getY(), actor.getZ()); enderman.setTarget(actor); enderman.tickCount = 0;
            float health = actor.getHealth(); EndMobRules.tick(enderman);
            require(actor.getHealth() < health, "Eye laser damages a visible hostile target"); enderman.discard();
            passed.accept("Enderman eye laser targets visible enemies and applies configured damage");
            var off = config.snapshot(); for (String id : List.of("nether_slayer", "expanded_weapon_enchantments", "elytra_enchanting", "death_box")) off.getAsJsonObject("features").getAsJsonObject(id).addProperty("enabled", false); config.update(off, config.revision());
            require(!nether.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD)) && !sharpness.value().canEnchant(new ItemStack(Items.TRIDENT)) && !new ItemStack(Items.ELYTRA).isEnchantable(), "Config OFF restores enchantment eligibility");
            require(!DeathBoxContent.capture(actor, level), "Config OFF prevents new death boxes");
            passed.accept("Config OFF restores native enchanting eligibility and disables new death boxes");
        } finally {
            level.getGameRules().set(GameRules.KEEP_INVENTORY, oldKeep, server);
            actor.discard(); config.update(saved, config.revision());
        }
    }
}
