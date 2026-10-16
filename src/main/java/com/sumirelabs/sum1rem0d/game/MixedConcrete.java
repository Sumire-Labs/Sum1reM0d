package com.sumirelabs.sum1rem0d.game;

import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class MixedConcrete {
 private MixedConcrete() {}
 public static Block block(){return BuiltInRegistries.BLOCK.getValue(ModContent.id("mixed_concrete"));}
 public static Item item(){return BuiltInRegistries.ITEM.getValue(ModContent.id("mixed_concrete"));}
 @SuppressWarnings("unchecked") public static BlockEntityType<MixedConcreteEntity> type(){return (BlockEntityType<MixedConcreteEntity>)BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(ModContent.id("mixed_concrete"));}
 public static Block createBlock(){return new MixedConcreteBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("white_concrete"))).setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK,ModContent.id("mixed_concrete"))));}
 public static Item createItem(){return new BlockItem(block(),new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM,ModContent.id("mixed_concrete"))));}
 public static BlockEntityType<MixedConcreteEntity> createType(){
  //? if fabric {
  return net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(MixedConcreteEntity::new,block()).build();
  //?} elif neoforge {
  /*return new BlockEntityType<>(MixedConcreteEntity::new,Set.of(block()));
  *///?}
 }
 public static boolean isConcrete(ItemStack stack){if(stack.is(item())) return true;if(!(stack.getItem() instanceof BlockItem bi))return false;var id=BuiltInRegistries.BLOCK.getKey(bi.getBlock());return id.getNamespace().equals("minecraft") && Arrays.stream(DyeColor.values()).anyMatch(c->id.getPath().equals(c.getName()+"_concrete"));}
 public static DyedItemColor color(ItemStack stack){var explicit=stack.get(DataComponents.DYED_COLOR);if(explicit!=null)return explicit;if(stack.is(item()))return new DyedItemColor(0xFFFFFF);var id=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();for(var dye:DyeColor.values())if(id.equals(dye.getName()+"_concrete"))return new DyedItemColor(dye.getTextureDiffuseColor() & 0xFFFFFF);return new DyedItemColor(0xFFFFFF);}
 public static ItemStack stack(int color){var stack=new ItemStack(item());stack.set(DataComponents.DYED_COLOR,new DyedItemColor(color & 0xFFFFFF));return stack;}
}
