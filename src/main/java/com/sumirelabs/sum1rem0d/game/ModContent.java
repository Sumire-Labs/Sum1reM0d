package com.sumirelabs.sum1rem0d.game;

import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModContent {
    public static final List<String> EFFECTS = List.of("vegan", "influenza", "headache", "environment_group", "environment_anger", "sleep_hunger", "sleep_slowness");
    public static final List<String> VISUAL_NYLIUM = List.of("warped_nylium", "crimson_nylium");
    public static Block createVisualNylium(String name) { return new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERRACK).setId(ResourceKey.create(Registries.BLOCK, id("sideways_" + name)))); }
    private ModContent() {}
    public static Identifier id(String name) { return Identifier.fromNamespaceAndPath(Sum1reM0d.MOD_ID, name); }
    public static Holder<MobEffect> effect(String name) { return BuiltInRegistries.MOB_EFFECT.getOrThrow(ResourceKey.create(Registries.MOB_EFFECT, id(name))); }
    public static Item coin() { return BuiltInRegistries.ITEM.getValue(id("coin")); }
    public static Item coinRoll() { return BuiltInRegistries.ITEM.getValue(id("coin_roll")); }
    public static Item createCoin(String name) { return new CoinItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id(name))).stacksTo(64)); }
    public static Item mudBall() { return BuiltInRegistries.ITEM.getValue(id("mud_ball")); }
    public static Item createMudBall() { return new MudBallItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id("mud_ball"))).stacksTo(64)); }
    public static Item soap() { return BuiltInRegistries.ITEM.getValue(id("soap")); }
    public static Block upsideDownGrass() { return BuiltInRegistries.BLOCK.getValue(id("upside_down_grass")); }
    public static Block createGrass() { return new VisualGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK).setId(ResourceKey.create(Registries.BLOCK, id("upside_down_grass")))); }
    public static Block createVisualFurnace(String name) { return new VisualFurnaceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE).setId(ResourceKey.create(Registries.BLOCK, id("visual_" + name)))); }
    public static Item createSoap() { return new SoapItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("soap"))).stacksTo(64)); }
    public static MobEffect createEffect(String name) {
        MobEffect effect = new NamedEffect(name);
        if (name.equals("influenza")) {
            effect.addAttributeModifier(Attributes.MOVEMENT_SPEED, id("influenza_speed"), -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            effect.addAttributeModifier(Attributes.ATTACK_DAMAGE, id("influenza_weakness"), -4, AttributeModifier.Operation.ADD_VALUE);
        }
        if (name.equals("headache")) effect.addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, id("headache"), -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (name.equals("sleep_slowness")) effect.addAttributeModifier(Attributes.MOVEMENT_SPEED, id("sleep_slowness"), -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        return effect;
    }
    public static void registerFabric() {
        ExtraContent.registerFabric();
        for (String name : EFFECTS) Registry.register(BuiltInRegistries.MOB_EFFECT, id(name), createEffect(name));
        Registry.register(BuiltInRegistries.BLOCK,id("mixed_concrete"),MixedConcrete.createBlock());
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,id("mixed_concrete"),MixedConcrete.createType());
        Registry.register(BuiltInRegistries.ITEM,id("mixed_concrete"),MixedConcrete.createItem());
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,id("mixed_concrete"),MixedConcreteRecipe.SERIALIZER);
        Registry.register(BuiltInRegistries.ITEM, id("soap"), createSoap());
        Registry.register(BuiltInRegistries.ITEM, id("mud_ball"), createMudBall());
        for (String name : List.of("coin", "coin_roll")) Registry.register(BuiltInRegistries.ITEM,id(name),createCoin(name));
        Registry.register(BuiltInRegistries.MENU,id("trade"),TradeMenu.TYPE);
        Registry.register(BuiltInRegistries.BLOCK, id("upside_down_grass"), createGrass());
        for (String name : VISUAL_NYLIUM) Registry.register(BuiltInRegistries.BLOCK, id("sideways_" + name), createVisualNylium(name));
        PathFarmer.registerFabric();
        for (String name : List.of("furnace", "blast_furnace", "smoker")) Registry.register(BuiltInRegistries.BLOCK, id("visual_" + name), createVisualFurnace(name));
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("configured_crafting"), ConfiguredCraftingRecipe.SERIALIZER);
        Registry.register(BuiltInRegistries.MENU, id("fletching"), FletchingMenu.TYPE);
    }
    private static final class NamedEffect extends MobEffect {
        private final String name;
        NamedEffect(String name) { super(MobEffectCategory.HARMFUL, switch (name) { case "vegan" -> 0x69B83A; case "influenza" -> 0xBBD47C; case "headache" -> 0x8D76A7; default -> 0x3B9F67; }); this.name = name; }
        @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return name.equals("sleep_hunger") || name.equals("environment_anger") && duration % Math.max(1, 25 >> Math.min(5, amplifier)) == 0; }
        @Override public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) { return (name.equals("sleep_hunger") ? MobEffects.HUNGER : MobEffects.POISON).value().applyEffectTick(level, entity, amplifier); }
    }
}
