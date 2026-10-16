package com.sumirelabs.sum1rem0d.game;
import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
public final class ExtraContent {
 public static final List<String> BLOCKS=List.of("piston_base","mixed_wool","mixed_bed","glow_wire");
 public static final List<String> ITEMS=java.util.stream.Stream.concat(List.of("piston_base","tamed_book","mixed_wool","mixed_bed").stream(),ExtraEquipment.ITEMS.stream()).toList();
 public static final List<String> BLOCK_ENTITIES=List.of("mixed_wool","mixed_bed");
 private ExtraContent(){}
 public static Block block(String name){return BuiltInRegistries.BLOCK.getValue(ModContent.id(name));}
 public static Item item(String name){return BuiltInRegistries.ITEM.getValue(ModContent.id(name));}
 public static Block createBlock(String name){return switch(name){case "glow_wire"->new GlowWireBlock(BlockBehaviour.Properties.of().noCollision().noOcclusion().instabreak().replaceable().lightLevel(s->s.getValue(GlowWireBlock.POWER)).setId(ResourceKey.create(Registries.BLOCK,ModContent.id(name))));case "piston_base"->new PistonRootBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.PISTON).setId(ResourceKey.create(Registries.BLOCK,ModContent.id(name))));case "mixed_wool"->new DyedWoolBlock(BlockBehaviour.Properties.ofFullCopy(BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("white_wool"))).setId(ResourceKey.create(Registries.BLOCK,ModContent.id(name))));case "mixed_bed"->new DyedBedBlock(BlockBehaviour.Properties.ofFullCopy(BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("white_bed"))).setId(ResourceKey.create(Registries.BLOCK,ModContent.id(name))));default->throw new IllegalArgumentException(name);};}
 public static Item createItem(String name){var props=new Item.Properties().setId(ResourceKey.create(Registries.ITEM,ModContent.id(name)));return switch(name){case "tamed_book"->new WritableBookItem(props.stacksTo(1).component(DataComponents.WRITABLE_BOOK_CONTENT,WritableBookContent.EMPTY));case "piston_base","mixed_wool"->new BlockItem(block(name),props);case "mixed_bed"->new BlockItem(block(name),props.stacksTo(1));default->{if(ExtraEquipment.ITEMS.contains(name))yield ExtraEquipment.create(name);throw new IllegalArgumentException(name);}};}
 @SuppressWarnings("unchecked") public static net.minecraft.world.level.block.entity.BlockEntityType<TextileEntity> type(String name){return (net.minecraft.world.level.block.entity.BlockEntityType<TextileEntity>)BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(ModContent.id(name));}
 public static net.minecraft.world.level.block.entity.BlockEntityType<TextileEntity> createType(String name){
  //? if fabric {
  return net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create((net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state)->new TextileEntity(name,pos,state),block(name)).build();
  //?} elif neoforge {
  /*return new net.minecraft.world.level.block.entity.BlockEntityType<>((pos,state)->new TextileEntity(name,pos,state),Set.of(block(name)));
  *///?}
 }
 //? if neoforge {
 /*public static void setupNeo(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event){event.enqueueWork(DyeTextiles::registerHomes);}
 *///?}
 public static void registerFabric(){for(var name:BLOCKS)Registry.register(BuiltInRegistries.BLOCK,ModContent.id(name),createBlock(name));for(var name:BLOCK_ENTITIES)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,ModContent.id(name),createType(name));for(var name:ITEMS)Registry.register(BuiltInRegistries.ITEM,ModContent.id(name),createItem(name));Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,ModContent.id("extra_crafting"),ExtraCraftingRecipe.SERIALIZER);DyeTextiles.registerHomes();}
}
