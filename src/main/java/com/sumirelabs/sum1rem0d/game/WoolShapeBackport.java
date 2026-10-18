package com.sumirelabs.sum1rem0d.game;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Backports the vanilla-colored wool slabs and stairs before Minecraft 26.3. */
public final class WoolShapeBackport {
    public static final List<String> BLOCKS = names();

    private WoolShapeBackport() {}

    private static List<String> names() {
        var names = new ArrayList<String>(DyeColor.values().length * 2);
        for (DyeColor color : DyeColor.values()) {
            names.add(path(color, false));
            names.add(path(color, true));
        }
        return List.copyOf(names);
    }

    private static String path(DyeColor color, boolean stairs) {
        return color.getName() + "_wool_" + (stairs ? "stairs" : "slab");
    }

    private static boolean required() {
        //? if >=26.3 {
        return false;
        //?} else {
        /*return true;
        *///?}
    }

    public static Item item(DyeColor color, boolean stairs) {
        String name = path(color, stairs);
        //? if >=26.3 {
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(name));
        //?} else {
        /*return BuiltInRegistries.ITEM.getValue(ModContent.id(name));
        *///?}
    }

    private static Block block(String name) {
        return BuiltInRegistries.BLOCK.getValue(ModContent.id(name));
    }

    private static Block createBlock(String name) {
        String color = name.substring(0, name.indexOf("_wool_"));
        Block wool = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(color + "_wool"));
        var properties = BlockBehaviour.Properties.ofFullCopy(wool)
            .setId(ResourceKey.create(Registries.BLOCK, ModContent.id(name)));
        return name.endsWith("_stairs")
            ? new StairBlock(wool.defaultBlockState(), properties)
            : new SlabBlock(properties);
    }

    private static Item createItem(String name) {
        return new BlockItem(block(name), new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ModContent.id(name))));
    }

    public static List<String> registeredBlocks() {
        return required() ? BLOCKS : List.of();
    }

    public static List<net.minecraft.world.item.ItemStack> creativeStacks() {
        if (!required()) return List.of();
        return BLOCKS.stream().map(name -> new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.getValue(ModContent.id(name)))).toList();
    }

    public static void registerFabric() {
        if (!required()) return;
        for (String name : BLOCKS) Registry.register(BuiltInRegistries.BLOCK, ModContent.id(name), createBlock(name));
        for (String name : BLOCKS) Registry.register(BuiltInRegistries.ITEM, ModContent.id(name), createItem(name));
        registerFlammability();
    }

    private static void registerFlammability() {
        if (!required()) return;
        var fire = (com.sumirelabs.sum1rem0d.mixin.FireRegistrationAccess) Blocks.FIRE;
        for (String name : BLOCKS) fire.sum1rem0d$flammable(block(name), 30, 60);
    }

    //? if neoforge {
    /*public static void registerNeo(net.neoforged.neoforge.registries.RegisterEvent event) {
        if (!required()) return;
        event.register(Registries.BLOCK, helper -> {
            for (String name : BLOCKS) helper.register(ModContent.id(name), createBlock(name));
        });
        event.register(Registries.ITEM, helper -> {
            for (String name : BLOCKS) helper.register(ModContent.id(name), createItem(name));
        });
    }

    public static void setupNeo(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(WoolShapeBackport::registerFlammability);
    }
    *///?}
}
