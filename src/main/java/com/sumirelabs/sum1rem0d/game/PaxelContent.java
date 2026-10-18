package com.sumirelabs.sum1rem0d.game;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.server.level.ServerPlayer;
import com.sumirelabs.sum1rem0d.network.PaxelRecipeMapPayload;
import com.sumirelabs.sum1rem0d.network.PlatformNetwork;

/** Material-specific paxels with combined axe, shovel, and pickaxe tool rules. */
public final class PaxelContent {
    public static final List<String> MATERIALS = List.of("wooden", "stone", "copper", "iron", "golden", "diamond", "netherite", "emerald", "redstone", "lapis");
    public static final List<String> ITEMS = MATERIALS.stream().map(material -> material + "_paxel").toList();
    private PaxelContent() {}

    public static Item item(String material) {
        return BuiltInRegistries.ITEM.getValue(ModContent.id(material + "_paxel"));
    }

    public static void sendRecipeMap(ServerPlayer player) {
        var server = player.level().getServer();
        if (server != null) PlatformNetwork.send(player,
                new PaxelRecipeMapPayload(PaxelRecipe.selectionTable(server.overworld().getSeed())));
    }

    public static Item createItem(String material) {
        if (material.endsWith("_paxel")) material = material.substring(0, material.length() - "_paxel".length());
        if (!MATERIALS.contains(material)) throw new IllegalArgumentException("Unknown paxel material " + material);
        return new PaxelItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ModContent.id(material + "_paxel"))).stacksTo(1));
    }

    /** Computes server defaults after all registries have bound item components. */
    public static void refreshDefaults() {
        for (String material : MATERIALS) {
            Item paxel = item(material);
            if (paxel == net.minecraft.world.item.Items.AIR || !paxel.builtInRegistryHolder().areComponentsBound()) continue;
            String namespace = List.of("emerald", "redstone", "lapis").contains(material) ? "sum1rem0d" : "minecraft";
            ItemStack pickaxe = toolStack(namespace, material + "_pickaxe");
            ItemStack sword = toolStack(namespace, material + "_sword");
            ItemStack axe = toolStack(namespace, material + "_axe");
            ItemStack shovel = toolStack(namespace, material + "_shovel");
            var components = DataComponentMap.builder().addAll(paxel.components())
                    .set(DataComponents.TOOL, mergedTool(pickaxe, axe, shovel))
                    // Read the raw component so the shared combat mixin applies configured
                    // gold/netherite durability exactly once to the paxel too.
                    .set(DataComponents.MAX_DAMAGE, pickaxe.getOrDefault(DataComponents.MAX_DAMAGE, 0))
                    .set(DataComponents.DAMAGE, 0);
            var modifiers = paxelModifiers(pickaxe.get(DataComponents.ATTRIBUTE_MODIFIERS), sword.get(DataComponents.ATTRIBUTE_MODIFIERS));
            if (modifiers != null) components.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
            paxel.builtInRegistryHolder().bindComponents(components.build());
        }
    }

    /** Use the material sword's damage plus one while retaining pickaxe attack speed. */
    static ItemAttributeModifiers paxelModifiers(ItemAttributeModifiers pickaxe, ItemAttributeModifiers sword) {
        if (pickaxe == null || sword == null) return pickaxe;
        var damageAttribute = Attributes.ATTACK_DAMAGE;
        var entries = new ArrayList<ItemAttributeModifiers.Entry>();
        for (var entry : pickaxe.modifiers())
            if (!entry.attribute().equals(damageAttribute)) entries.add(entry);
        boolean swordDamageFound = false;
        for (var entry : sword.modifiers()) if (entry.attribute().equals(damageAttribute)) {
            var modifier = entry.modifier();
            entries.add(new ItemAttributeModifiers.Entry(entry.attribute(), new AttributeModifier(
                    modifier.id(), modifier.amount() + 1, modifier.operation()), entry.slot()));
            swordDamageFound = true;
        }
        if (!swordDamageFound) for (var entry : pickaxe.modifiers()) if (entry.attribute().equals(damageAttribute)) {
            var modifier = entry.modifier();
            entries.add(new ItemAttributeModifiers.Entry(entry.attribute(), new AttributeModifier(
                    modifier.id(), modifier.amount() + 1, modifier.operation()), entry.slot()));
        }
        return new ItemAttributeModifiers(entries);
    }

    private static ItemStack toolStack(String namespace, String path) {
        var id = Identifier.fromNamespaceAndPath(namespace, path);
        var item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) throw new IllegalStateException("Missing tool for paxel material: " + id);
        return item.getDefaultInstance();
    }

    static Tool mergedTool(ItemStack pickaxe, ItemStack axe, ItemStack shovel) {
        Tool pick = pickaxe.get(DataComponents.TOOL), ax = axe.get(DataComponents.TOOL), sh = shovel.get(DataComponents.TOOL);
        if (pick == null || ax == null || sh == null) throw new IllegalStateException("Paxel ingredient item has no tool component");
        var rules = new ArrayList<Tool.Rule>();
        rules.addAll(pick.rules()); rules.addAll(ax.rules()); rules.addAll(sh.rules());
        float speed = Math.max(pick.defaultMiningSpeed(), Math.max(ax.defaultMiningSpeed(), sh.defaultMiningSpeed()));
        int damage = Math.max(pick.damagePerBlock(), Math.max(ax.damagePerBlock(), sh.damagePerBlock()));
        boolean creative = pick.canDestroyBlocksInCreative() || ax.canDestroyBlocksInCreative() || sh.canDestroyBlocksInCreative();
        return new Tool(rules, speed, damage, creative);
    }
}
