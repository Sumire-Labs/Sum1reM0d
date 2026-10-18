package com.sumirelabs.sum1rem0d.game;

import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Independent content and registry factories for the A01-A07/A09 automation machines. */
public final class AutomationContent {
    public static final List<String> BLOCKS=List.of("automated_crafting_table","jukebox_grinder","washing_cauldron","tesla_coil","auto_clicker");
    public static final List<String> MATERIAL_BLOCKS=List.of("dust");
    public static final List<String> ITEMS=List.of("iron_dust","copper_dust","gold_dust","washed_iron_dust","washed_copper_dust","washed_gold_dust");
    public static final MenuType<AutomationMenu> MENU=new MenuType<>(AutomationMenu::new,FeatureFlags.DEFAULT_FLAGS);
    private AutomationContent(){}
    public static boolean retiredItem(net.minecraft.world.item.Item item){var id=BuiltInRegistries.ITEM.getKey(item);return id.getNamespace().equals("sum1rem0d")&&List.of("automated_crafting_table","washing_cauldron","tesla_coil").contains(id.getPath());}
    public static Block block(String name){return BuiltInRegistries.BLOCK.getValue(ModContent.id(name));}
    public static Item item(String name){return BuiltInRegistries.ITEM.getValue(ModContent.id(name));}
    public static boolean needsLegacySulfur(){return BuiltInRegistries.ITEM.getOptional(net.minecraft.resources.Identifier.withDefaultNamespace("sulfur")).orElse(Items.AIR)==Items.AIR;}
    public static Item sulfur(){return needsLegacySulfur()?item("sulfur"):BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("sulfur"));}
    @SuppressWarnings("unchecked") public static BlockEntityType<AutomationMachineEntity> type(){return (BlockEntityType<AutomationMachineEntity>)BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(ModContent.id("automation_machine"));}
    public static Block createBlock(String name){
        if(name.equals("dust"))return new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).setId(ResourceKey.create(Registries.BLOCK,ModContent.id(name))));
        Block copy=switch(name){case "automated_crafting_table"->Blocks.CRAFTING_TABLE;case "jukebox_grinder"->Blocks.JUKEBOX;case "washing_cauldron"->Blocks.CAULDRON;case "tesla_coil"->Blocks.IRON_BLOCK;case "auto_clicker"->Blocks.DISPENSER;default->throw new IllegalArgumentException(name);};
        return new AutomationMachineBlock(name,BlockBehaviour.Properties.ofFullCopy(copy).setId(ResourceKey.create(Registries.BLOCK,ModContent.id(name))));
    }
    public static Item createBlockItem(String name){return new BlockItem(block(name),new Item.Properties().setId(ResourceKey.create(Registries.ITEM,ModContent.id(name))));}
    public static Item createItem(String name){return new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,ModContent.id(name))).stacksTo(64));}
    public static List<ItemStack> creativeStacks(){var out=new ArrayList<ItemStack>();for(var id:BLOCKS)if(!retiredItem(item(id)))out.add(new ItemStack(item(id)));for(var id:MATERIAL_BLOCKS)out.add(new ItemStack(item(id)));for(var id:ITEMS)out.add(new ItemStack(item(id)));if(sulfur()!=Items.AIR)out.add(new ItemStack(sulfur()));return List.copyOf(out);}
    public static BlockEntityType<AutomationMachineEntity> createType(){
        var blocks=BLOCKS.stream().map(AutomationContent::block).toArray(Block[]::new);
        //? if fabric {
        return net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(AutomationMachineEntity::new,blocks).build();
        //?} elif neoforge {
        /*return new BlockEntityType<>(AutomationMachineEntity::new,Set.of(blocks));
        *///?}
    }
    public static void registerFabric(){
        for(var id:BLOCKS)Registry.register(BuiltInRegistries.BLOCK,ModContent.id(id),createBlock(id));
        for(var id:MATERIAL_BLOCKS)Registry.register(BuiltInRegistries.BLOCK,ModContent.id(id),createBlock(id));
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,ModContent.id("automation_machine"),createType());
        for(var id:BLOCKS)Registry.register(BuiltInRegistries.ITEM,ModContent.id(id),createBlockItem(id));
        for(var id:MATERIAL_BLOCKS)Registry.register(BuiltInRegistries.ITEM,ModContent.id(id),createBlockItem(id));
        for(var id:ITEMS)Registry.register(BuiltInRegistries.ITEM,ModContent.id(id),createItem(id));
        if(needsLegacySulfur())Registry.register(BuiltInRegistries.ITEM,ModContent.id("sulfur"),createItem("sulfur"));
        Registry.register(BuiltInRegistries.MENU,ModContent.id("automation_machine"),MENU);
    }
}
