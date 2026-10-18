package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Edible collectible that permanently adds a bounded amount of maximum health. */
public final class LifeBerryItem extends Item {
    public LifeBerryItem(Properties properties) { super(properties.food(new FoodProperties(2, 0.3f, true))); }

    public static Item create() {
        return new LifeBerryItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ModContent.id("life_berry"))).stacksTo(16));
    }

    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide() && entity instanceof Player player) applyBonus(player);
        return result;
    }

    public static void applyBonus(Player player) {
        if (!Rules.on("life_berry")) return;
        var attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;
        double perBerry = Math.max(0, Rules.n("life_berry", "max_health_bonus"));
        double maximum = Math.max(0, Rules.n("life_berry", "maximum_bonus"));
        if (perBerry <= 0 || maximum <= 0) return;
        double owned = attribute.getModifiers().stream()
                .filter(modifier -> modifier.id().getNamespace().equals("sum1rem0d") && modifier.id().getPath().startsWith("life_berry_bonus_"))
                .mapToDouble(AttributeModifier::amount).sum();
        double addition = Math.max(0, Math.min(perBerry, maximum - owned));
        if (addition > 0) {
            int index = 0;
            while (attribute.hasModifier(ModContent.id("life_berry_bonus_" + index))) index++;
            attribute.addPermanentModifier(new AttributeModifier(ModContent.id("life_berry_bonus_" + index), addition, AttributeModifier.Operation.ADD_VALUE));
            player.setHealth((float) Math.min(player.getMaxHealth(), player.getHealth() + addition));
        }
    }

    public static void restoreAfterDeath(Player player, Player oldPlayer) {
        var destination = player.getAttribute(Attributes.MAX_HEALTH);
        var source = oldPlayer.getAttribute(Attributes.MAX_HEALTH);
        if (destination == null || source == null) return;
        for (var modifier : source.getPermanentModifiers()) {
            if (modifier.id().getNamespace().equals("sum1rem0d") && modifier.id().getPath().startsWith("life_berry_bonus_")
                    && !destination.hasModifier(modifier.id()))
                destination.addPermanentModifier(modifier);
        }
    }
}
