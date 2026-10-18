package com.sumirelabs.sum1rem0d.game;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Registry factory for the invisible, server-authoritative seat passenger anchor. */
public final class MobilityContent {
    public static final java.util.List<String> CAULDRON_BLOCKS = java.util.List.of("large_cauldron", "huge_cauldron");
    public static final MenuType<ChestMenu> COPPER_CHEST_8;
    public static final MenuType<ChestMenu> COPPER_CHEST_16;
    static {
        COPPER_CHEST_8 = new MenuType<>((id, inventory) -> clientCopperMenu(id, inventory, 8), FeatureFlags.DEFAULT_FLAGS);
        COPPER_CHEST_16 = new MenuType<>((id, inventory) -> clientCopperMenu(id, inventory, 16), FeatureFlags.DEFAULT_FLAGS);
    }
    private MobilityContent() {}
    private static ChestMenu clientCopperMenu(int id, Inventory inventory, int rows) {
        MenuType<ChestMenu> type = copperMenuType(rows);
        return new ChestMenu(type, id, inventory, new SimpleContainer(rows * 9), rows);
    }
    private static MenuType<ChestMenu> copperMenuType(int rows) { return rows == 8 ? COPPER_CHEST_8 : COPPER_CHEST_16; }
    @SuppressWarnings("unchecked")
    public static EntityType<SeatEntity> type() {
        return (EntityType<SeatEntity>) net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(ModContent.id("seat"));
    }
    public static EntityType<SeatEntity> createSeatType() {
        return EntityType.Builder.of(SeatEntity::new, MobCategory.MISC)
                .sized(0.01f, 0.01f)
                .clientTrackingRange(8)
                .updateInterval(1)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, ModContent.id("seat")));
    }
    public static LargeCauldronBlock largeCauldron() {
        Block block = BuiltInRegistries.BLOCK.getValue(ModContent.id("large_cauldron"));
        return block instanceof LargeCauldronBlock cauldron ? cauldron : null;
    }
    public static LargeCauldronBlock hugeCauldron() {
        Block block = BuiltInRegistries.BLOCK.getValue(ModContent.id("huge_cauldron"));
        return block instanceof LargeCauldronBlock cauldron ? cauldron : null;
    }
    public static Block cauldronBlock(String name) { return BuiltInRegistries.BLOCK.getValue(ModContent.id(name)); }
    public static Block createCauldronBlock(String name) {
        int depth = name.equals("huge_cauldron") ? 2 : 1;
        return new LargeCauldronBlock(depth, BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON)
                .pushReaction(
                        //? if >=26.3 {
                        net.minecraft.world.level.material.PushReaction.IMMOVEABLE
                        //?} else {
                        /*net.minecraft.world.level.material.PushReaction.BLOCK
                        *///?}
                )
                .setId(ResourceKey.create(Registries.BLOCK, ModContent.id(name))));
    }
    public static Item createCauldronItem(String name) {
        return new BlockItem(cauldronBlock(name), new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ModContent.id(name))));
    }
    public static java.util.List<ItemStack> creativeStacks() { return CAULDRON_BLOCKS.stream().map(name -> new ItemStack(BuiltInRegistries.ITEM.getValue(ModContent.id(name)))).toList(); }
    public static void registerFabric() {
        for (String name : CAULDRON_BLOCKS) Registry.register(BuiltInRegistries.BLOCK, ModContent.id(name), createCauldronBlock(name));
        for (String name : CAULDRON_BLOCKS) Registry.register(BuiltInRegistries.ITEM, ModContent.id(name), createCauldronItem(name));
        Registry.register(BuiltInRegistries.MENU, ModContent.id("copper_chest_8"), COPPER_CHEST_8);
        Registry.register(BuiltInRegistries.MENU, ModContent.id("copper_chest_16"), COPPER_CHEST_16);
    }
}
