package com.sumirelabs.sum1rem0d.game;

import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class BookshelfContent {
    public static final List<String> BLOCKS=java.util.stream.IntStream.rangeClosed(1,8).mapToObj(i->"compressed_bookshelf_"+i).toList();
    private BookshelfContent(){}
    public static long units(int tier){long count=1;for(int i=0;i<tier;i++)count*=9;return count;}
    public static int cap(){return Rules.on("compressed_bookshelves")&&Rules.b("compressed_bookshelves","expanded_levels")?Rules.i("compressed_bookshelves","maximum_level")/2:15;}
    public static long rawPower(int tier){return Rules.on("compressed_bookshelves")?Math.max(1,Math.round(units(tier)*Rules.n("compressed_bookshelves","power_multiplier"))):1;}
    public static int power(int tier){return (int)Math.min(cap(),rawPower(tier));}
    public static int color(int tier){
        double brightness=Math.max(Rules.n("compressed_bookshelves","minimum_brightness")/100,1-tier*Rules.n("compressed_bookshelves","darkening_per_tier"));
        int c=(int)Math.round(255*Math.clamp(brightness,0,1));return 0xFF000000|c<<16|c<<8|c;
    }
    public static int tier(ItemStack stack){if(stack.is(Items.BOOKSHELF))return 0;return stack.getItem() instanceof BlockItem item&&item.getBlock() instanceof CompressedBookshelfBlock block?block.tier():-1;}
    public static Block block(int tier){return BuiltInRegistries.BLOCK.getValue(ModContent.id("compressed_bookshelf_"+tier));}
    public static Item item(int tier){return tier==0?Items.BOOKSHELF:BuiltInRegistries.ITEM.getValue(ModContent.id("compressed_bookshelf_"+tier));}
    public static Block createBlock(String name){int tier=Integer.parseInt(name.substring(name.lastIndexOf('_')+1));return new CompressedBookshelfBlock(tier,BlockBehaviour.Properties.ofFullCopy(Blocks.BOOKSHELF).setId(ResourceKey.create(Registries.BLOCK,ModContent.id(name))));}
    public static Item createItem(String name){int tier=Integer.parseInt(name.substring(name.lastIndexOf('_')+1));return new BlockItem(block(tier),new Item.Properties().setId(ResourceKey.create(Registries.ITEM,ModContent.id(name)))){
        @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<net.minecraft.network.chat.Component> out,TooltipFlag flag){
            super.appendHoverText(stack,context,display,out,flag);
            out.accept(net.minecraft.network.chat.Component.translatable("tooltip.sum1rem0d.compressed_bookshelf.units",units(tier)).withStyle(net.minecraft.ChatFormatting.GRAY));
            out.accept(net.minecraft.network.chat.Component.translatable("tooltip.sum1rem0d.compressed_bookshelf.power",rawPower(tier)).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
        }
    };}
    public static void registerFabric(){
        for(var name:BLOCKS)Registry.register(BuiltInRegistries.BLOCK,ModContent.id(name),createBlock(name));
        for(var name:BLOCKS)Registry.register(BuiltInRegistries.ITEM,ModContent.id(name),createItem(name));
        flammable();
    }
    public static void flammable(){var fire=(com.sumirelabs.sum1rem0d.mixin.FireRegistrationAccess)Blocks.FIRE;for(int i=1;i<=8;i++)fire.sum1rem0d$flammable(block(i),30,20);}
    public static ItemStack craft(CraftingInput input){
        if(!Rules.on("compressed_bookshelves")||!Rules.b("compressed_bookshelves","recipe_enabled"))return ItemStack.EMPTY;
        ItemStack first=ItemStack.EMPTY;int count=0;
        for(var stack:input.items())if(!stack.isEmpty()){if(first.isEmpty())first=stack;else if(!ItemStack.isSameItemSameComponents(first,stack))return ItemStack.EMPTY;count++;}
        if(first.isEmpty())return ItemStack.EMPTY;int tier=tier(first);
        if(tier<0)return ItemStack.EMPTY;
        if(count==9&&input.width()==3&&input.height()==3&&tier<Rules.i("compressed_bookshelves","max_tier"))return new ItemStack(item(tier+1));
        if(count==1&&tier>0&&Rules.b("compressed_bookshelves","unpack_enabled"))return new ItemStack(item(tier-1),9);
        return ItemStack.EMPTY;
    }
}
