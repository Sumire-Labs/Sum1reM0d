package com.sumirelabs.sum1rem0d.game;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class UtilityContent {
    public static final List<String> BLOCKS=List.of("glowstone_torch","wall_glowstone_torch","optical_fiber");
    public static final List<String> ITEMS=List.of("glowstone_torch","optical_fiber");
    private UtilityContent(){}
    public static Block block(String name){return BuiltInRegistries.BLOCK.getValue(ModContent.id(name));}
    public static Item item(String name){return BuiltInRegistries.ITEM.getValue(ModContent.id(name));}
    public static Block createBlock(String name){
        var properties=BlockBehaviour.Properties.of().noOcclusion().strength(.3f).setId(ResourceKey.create(Registries.BLOCK,ModContent.id(name)));
        if(name.equals("optical_fiber"))return new OpticalFiberBlock(properties.lightLevel(s->s.getValue(OpticalFiberBlock.POWER)).sound(SoundType.GLASS));
        return new GlowstoneTorchBlock(name.equals("wall_glowstone_torch"),properties.noCollision().instabreak().lightLevel(s->s.getValue(GlowstoneTorchBlock.LIGHT)).sound(SoundType.WOOD));
    }
    public static Item createItem(String name){
        var properties=new Item.Properties().setId(ResourceKey.create(Registries.ITEM,ModContent.id(name)));
        return name.equals("glowstone_torch")?new StandingAndWallBlockItem(block(name),block("wall_glowstone_torch"),Direction.DOWN,properties):new BlockItem(block(name),properties);
    }
    public static void registerFabric(){
        for(var name:BLOCKS)Registry.register(BuiltInRegistries.BLOCK,ModContent.id(name),createBlock(name));
        for(var name:ITEMS)Registry.register(BuiltInRegistries.ITEM,ModContent.id(name),createItem(name));
    }
    public static ItemStack craft(String feature,CraftingInput input){
        if(!Rules.on(feature)||!Rules.b(feature,"recipe_enabled"))return ItemStack.EMPTY;
        if(feature.equals("glowstone_torch")){
            int sticks=0,dust=0;
            for(var stack:input.items())if(!stack.isEmpty()){if(stack.is(Items.STICK))sticks++;else if(stack.is(Items.GLOWSTONE_DUST))dust++;else return ItemStack.EMPTY;}
            return sticks==1&&dust==1?new ItemStack(item(feature),Rules.i(feature,"recipe_output")):ItemStack.EMPTY;
        }
        if(input.width()!=3||input.height()!=3)return ItemStack.EMPTY;
        for(int i=0;i<9;i++)if(!input.getItem(i).is(i==4?Items.GLOWSTONE:i==3||i==5?Items.REDSTONE:Items.GLASS_PANE))return ItemStack.EMPTY;
        return new ItemStack(item(feature),Rules.i(feature,"recipe_output"));
    }
}
