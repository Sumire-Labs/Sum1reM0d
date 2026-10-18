package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Native verifier for the durability threshold/bar and wooden mining/attack rules. */
final class CombatItemRulesVerification {
    private CombatItemRulesVerification() {}

    static void verify(MinecraftServer server, java.util.function.Consumer<String> passed) throws Exception {
        var store = Sum1reM0d.config();
        var original = store.snapshot();
        var draft = original.deepCopy();
        var attacker = EconomyVerification.actor(server, server.overworld(), "CombatItemRulesAttacker");
        var victim = EconomyVerification.actor(server, server.overworld(), "CombatItemRulesVictim");
        try {
            var features = draft.getAsJsonObject("features");
            for (var entry : features.entrySet()) entry.getValue().getAsJsonObject().addProperty("enabled", false);
            var gold = features.getAsJsonObject("gold_equipment");
            gold.addProperty("enabled", true);
            gold.addProperty("durability_multiplier", 1000);
            var chain = features.getAsJsonObject("chainmail_equipment");
            chain.addProperty("enabled", true);
            chain.addProperty("durability_multiplier", 1000);
            var wooden = features.getAsJsonObject("wooden_nerf");
            wooden.addProperty("enabled", true);
            wooden.addProperty("attack_multiplier", 0.25);
            wooden.addProperty("mining_speed_multiplier", 0.25);
            store.update(draft, store.revision());

            var goldTool = new ItemStack(Items.GOLDEN_PICKAXE);
            int goldBase = goldTool.get(DataComponents.MAX_DAMAGE);
            require(goldTool.getMaxDamage() == goldBase * 1000, "Golden pickaxe maximum durability is multiplied");
            goldTool.setDamageValue(goldBase - 1);
            require(!goldTool.isBroken() && goldTool.getBarWidth() > 0, "Scaled durability threshold and bar use the same maximum");
            goldTool.setDamageValue(goldTool.getMaxDamage() - 1);
            require(!goldTool.isBroken() && goldTool.nextDamageWillBreak(), "Scaled maximum controls final durability point");
            goldTool.setDamageValue(goldTool.getMaxDamage());
            require(goldTool.isBroken(), "Scaled maximum controls broken state");

            var chainArmor = new ItemStack(Items.CHAINMAIL_CHESTPLATE);
            require(chainArmor.getMaxDamage() == chainArmor.get(DataComponents.MAX_DAMAGE) * 1000, "Chain chestplate maximum durability is multiplied");
            var woodTool = new ItemStack(Items.WOODEN_PICKAXE);
            require(Math.abs(woodTool.getDestroySpeed(Blocks.STONE.defaultBlockState()) - 0.5f) < 0.001f,
                    "Wooden pickaxe mining speed uses configured multiplier");
            attacker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, woodTool);
            float scaledAttack = CombatItemRules.incomingDamage(victim, victim.damageSources().playerAttack(attacker), 8);
            require(Math.abs(scaledAttack - 2) < 0.001f, "Wooden attack damage uses configured multiplier");

            var netherite = features.getAsJsonObject("netherite_upgrade");
            netherite.addProperty("enabled", true); netherite.addProperty("durability_multiplier", 2); netherite.addProperty("attack_bonus", 2);
            netherite.addProperty("armor_toughness_bonus", 2);
            store.update(draft, store.revision());
            var toughness = attacker.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
            require(toughness != null, "Player exposes the armor toughness attribute");
            var foreignToughness = net.minecraft.resources.Identifier.fromNamespaceAndPath("verification", "foreign_toughness");
            toughness.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    foreignToughness, 1, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
            attacker.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
            attacker.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
            double twoPieceToughness = toughness.getValue();
            NetheriteArmorToughnessRules.tick(attacker);
            require(Math.abs(toughness.getValue() - twoPieceToughness - 4) < 0.001,
                    "Each equipped netherite armor piece adds its configured transient toughness bonus");
            attacker.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, ItemStack.EMPTY);
            double afterUnequip = toughness.getValue();
            NetheriteArmorToughnessRules.tick(attacker);
            require(Math.abs(toughness.getValue() - afterUnequip + 2) < 0.001,
                    "Removing one netherite armor piece removes only that slot's toughness modifier");
            attacker.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, ItemStack.EMPTY);
            attacker.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
            NetheriteArmorToughnessRules.tick(attacker);
            double beforeToughnessOff = toughness.getValue();
            netherite.addProperty("enabled", false); store.update(draft, store.revision());
            NetheriteArmorToughnessRules.tick(attacker);
            require(Math.abs(toughness.getValue() - beforeToughnessOff + 2) < 0.001 && toughness.hasModifier(foreignToughness),
                    "Netherite Config OFF removes its own transient bonus while preserving another modifier");
            netherite.addProperty("enabled", true); store.update(draft, store.revision());
            NetheriteArmorToughnessRules.tick(attacker);
            attacker.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, ItemStack.EMPTY);
            NetheriteArmorToughnessRules.tick(attacker);
            require(toughness.hasModifier(foreignToughness), "Unequipping netherite armor does not remove another source's toughness modifier");
            PaxelContent.refreshDefaults();
            for (String material : PaxelContent.MATERIALS) {
                String namespace = List.of("emerald", "redstone", "lapis").contains(material) ? "sum1rem0d" : "minecraft";
                var pickaxe = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(namespace, material + "_pickaxe")).orElseThrow().getDefaultInstance();
                var paxel = new ItemStack(PaxelContent.item(material));
                int rawDamage = pickaxe.get(DataComponents.MAX_DAMAGE);
                int expectedDamage = material.equals("golden") ? rawDamage * 1000 : material.equals("netherite") ? rawDamage * 2 : rawDamage;
                require(paxel.get(DataComponents.MAX_DAMAGE) == rawDamage && paxel.getMaxDamage() == expectedDamage,
                        "Paxel " + material + " copies raw material durability and applies rebalance at most once");
                float baseSpeed = paxel.get(DataComponents.TOOL).getMiningSpeed(Blocks.STONE.defaultBlockState());
                float expectedSpeed = material.equals("wooden") ? baseSpeed * 0.25f : baseSpeed;
                require(Math.abs(paxel.getDestroySpeed(Blocks.STONE.defaultBlockState()) - expectedSpeed) < 0.001f,
                        "Paxel " + material + " mines at its material speed with only the configured wooden modifier");
                attacker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, paxel);
                float expectedAttack = material.equals("wooden") ? 2 : material.equals("netherite") ? 10 : 8;
                require(Math.abs(CombatItemRules.incomingDamage(victim, victim.damageSources().playerAttack(attacker), 8) - expectedAttack) < 0.001f,
                        "Paxel " + material + " receives the correct one-time melee rebalance");
            }
            attacker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_SWORD));
            require(Math.abs(CombatItemRules.incomingDamage(victim, victim.damageSources().playerAttack(attacker), 8) - 10) < 0.001f,
                    "Actual netherite weapons receive one attack bonus");
            attacker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_INGOT));
            require(Math.abs(CombatItemRules.incomingDamage(victim, victim.damageSources().playerAttack(attacker), 8) - 8) < 0.001f,
                    "Netherite ingots and scraps do not receive a weapon damage bonus");
            passed.accept("Combat durability, break thresholds, gold/chain/netherite tools, wooden melee/mining and all ten material paxels");

            for (var entry : features.entrySet()) entry.getValue().getAsJsonObject().addProperty("enabled", false);
            store.update(draft, store.revision());
            for (String material : PaxelContent.MATERIALS) {
                var paxel = new ItemStack(PaxelContent.item(material));
                require(paxel.getMaxDamage() == paxel.get(DataComponents.MAX_DAMAGE),
                        "Config OFF restores raw durability on the " + material + " paxel");
                require(Math.abs(paxel.getDestroySpeed(Blocks.STONE.defaultBlockState())
                        - paxel.get(DataComponents.TOOL).getMiningSpeed(Blocks.STONE.defaultBlockState())) < 0.001f,
                        "Config OFF restores raw mining speed on the " + material + " paxel");
                attacker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, paxel);
                require(Math.abs(CombatItemRules.incomingDamage(victim, victim.damageSources().playerAttack(attacker), 8) - 8) < 0.001f,
                        "Config OFF restores raw melee damage on the " + material + " paxel");
            }
            require(goldTool.getMaxDamage() == goldBase && chainArmor.getMaxDamage() == chainArmor.get(DataComponents.MAX_DAMAGE),
                    "Config OFF restores vanilla durability immediately");
            require(Math.abs(woodTool.getDestroySpeed(Blocks.STONE.defaultBlockState()) - 2.0f) < 0.001f,
                    "Config OFF restores wooden mining speed immediately");
            require(Math.abs(CombatItemRules.incomingDamage(victim, victim.damageSources().playerAttack(attacker), 8) - 8) < 0.001f,
                    "Config OFF restores wooden attack damage immediately");
            passed.accept("Combat item rules revert immediately when Config is disabled");
        } finally {
            store.update(original, store.revision());
            attacker.discard();
            victim.discard();
        }
    }
}
