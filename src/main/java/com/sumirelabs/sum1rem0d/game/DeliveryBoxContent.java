package com.sumirelabs.sum1rem0d.game;

import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Registration factory for the owner controlled Sumzon delivery box. */
public final class DeliveryBoxContent {
    public static final String ID = "sumzon_delivery_box";
    public static final MenuType<DeliveryBoxMenu> MENU = new MenuType<>(DeliveryBoxMenu::new, FeatureFlags.DEFAULT_FLAGS);
    private DeliveryBoxContent() {}

    public static Block block() { return BuiltInRegistries.BLOCK.getValue(ModContent.id(ID)); }
    public static Item item() { return BuiltInRegistries.ITEM.getValue(ModContent.id(ID)); }
    @SuppressWarnings("unchecked")
    public static BlockEntityType<DeliveryBoxEntity> type() {
        return (BlockEntityType<DeliveryBoxEntity>) BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(ModContent.id(ID));
    }
    public static Block createBlock() {
        var props = BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)
            .setId(ResourceKey.create(Registries.BLOCK, ModContent.id(ID)));
        return new DeliveryBoxBlock(props);
    }
    public static Item createItem() {
        return new BlockItem(block(), new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ModContent.id(ID))));
    }
    public static BlockEntityType<DeliveryBoxEntity> createType() {
        //? if fabric {
        return net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder
            .create(DeliveryBoxEntity::new, block()).build();
        //?} elif neoforge {
        /*return new BlockEntityType<>(DeliveryBoxEntity::new, Set.of(block()));
        *///?}
    }
    public static void registerFabric() {
        Registry.register(BuiltInRegistries.BLOCK, ModContent.id(ID), createBlock());
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ModContent.id(ID), createType());
        Registry.register(BuiltInRegistries.ITEM, ModContent.id(ID), createItem());
        Registry.register(BuiltInRegistries.MENU, ModContent.id(ID), MENU);
    }
}
