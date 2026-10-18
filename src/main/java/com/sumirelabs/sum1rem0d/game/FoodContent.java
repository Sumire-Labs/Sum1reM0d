package com.sumirelabs.sum1rem0d.game;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;

/** L02/L06/L07/W24 food and drink content. */
public final class FoodContent {
    public static final List<String> ITEMS = List.of("cigarette", "bottled_milk", "milk_bundle", "cat_meat", "cooked_cat_meat");
    private FoodContent() {}

    public static Item item(String name) { return BuiltInRegistries.ITEM.getValue(ModContent.id(name)); }

    public static Item createItem(String name) {
        var properties = new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ModContent.id(name)));
        return switch (name) {
            case "cigarette" -> new CigaretteItem(properties.stacksTo(16).food(new FoodProperties(0, 0, true)));
            case "bottled_milk" -> new BottledMilkItem(properties.stacksTo(16)
                    .component(DataComponents.CONSUMABLE, Consumables.MILK_BUCKET)
                    .usingConvertsTo(Items.GLASS_BOTTLE));
            case "milk_bundle" -> new MilkBundleItem(properties.stacksTo(1).component(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY));
            case "cat_meat" -> new CatMeatItem(properties.stacksTo(64).food(new FoodProperties(3, 0.3f, false)), false);
            case "cooked_cat_meat" -> new CatMeatItem(properties.stacksTo(64).food(new FoodProperties(7, 0.7f, false)), true);
            default -> throw new IllegalArgumentException("Unknown food content item: " + name);
        };
    }

    public static void afterUse(ServerPlayer player, ItemStack eaten) {
        if ((eaten.is(Items.MILK_BUCKET) || eaten.is(item("bottled_milk")))) MilkComboContent.afterMilk(player);
    }

    static void consumeCigarette(LivingEntity entity, Level level) {
        if (!(entity instanceof ServerPlayer player) || !Rules.on("cigarette")) return;
        Holder<MobEffect> buff = randomEffect(player, "cigarette", "buff_effects");
        if (buff == null) return;
        var effect = new MobEffectInstance(buff, Math.max(1, Rules.i("cigarette", "buff_seconds")) * 20,
                Math.max(0, Rules.i("cigarette", "buff_level") - 1));
        player.addEffect(effect);
        var timer = BuiltInRegistries.MOB_EFFECT.getOptional(ModContent.id("cigarette_delay")).orElse(null);
        if (timer != null) player.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(timer),
                Math.max(1, Rules.i("cigarette", "buff_seconds")) * 20, 0, true, false, false));
    }

    static void consumeRawCatMeat(LivingEntity entity) {
        if (entity.level().isClientSide() || !(entity instanceof Player player) || !Rules.on("cat_meat")) return;
        if (player.getRandom().nextDouble() < Rules.n("cat_meat", "poison_chance"))
            player.addEffect(new MobEffectInstance(MobEffects.POISON, Rules.i("cat_meat", "poison_seconds") * 20, 0));
        if (player.getRandom().nextDouble() < Rules.n("cat_meat", "hunger_chance"))
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, Rules.i("cat_meat", "hunger_seconds") * 20, 0));
    }

    public static int catMeatDropCount(net.minecraft.util.RandomSource random) {
        if (!Rules.on("cat_meat") || random.nextDouble() >= Math.clamp(Rules.n("cat_meat", "drop_chance"), 0, 1)) return 0;
        int minimum = Math.max(1, Rules.i("cat_meat", "drop_min"));
        int maximum = Math.max(minimum, Rules.i("cat_meat", "drop_max"));
        return minimum + random.nextInt(maximum - minimum + 1);
    }

    static Holder<MobEffect> randomEffect(ServerPlayer player, String feature, String key) {
        List<Holder<MobEffect>> pool = Rules.list(feature, key).stream().map(Identifier::tryParse)
                .filter(java.util.Objects::nonNull).map(BuiltInRegistries.MOB_EFFECT::getOptional)
                .flatMap(java.util.Optional::stream).map(BuiltInRegistries.MOB_EFFECT::wrapAsHolder).toList();
        return pool.isEmpty() ? null : pool.get(player.getRandom().nextInt(pool.size()));
    }
}
