package com.sumirelabs.sum1rem0d.game;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
public final class BookshelfEnchanting {
    private BookshelfEnchanting(){}
    public static int adjustPower(Level level,BlockPos table,int vanilla){
        long total=vanilla;
        // NeoForge already asks our block for its full getEnchantPowerBonus.
        //? if fabric {
        for(var offset:EnchantingTableBlock.BOOKSHELF_OFFSETS){var pos=table.offset(offset);if(!level.hasChunkAt(pos))continue;
            if(level.getBlockState(pos).getBlock() instanceof CompressedBookshelfBlock block&&EnchantingTableBlock.isValidBookShelf(level,table,offset))total+=BookshelfContent.power(block.tier())-1;
        }
        //?}
        return (int)Math.min(BookshelfContent.cap(),Math.max(0,total));
    }
    public static int expandedCost(RandomSource random,int slot,int shelves,ItemStack stack){
        if(stack.get(net.minecraft.core.component.DataComponents.ENCHANTABLE)==null)return 0;
        int power=Math.min(BookshelfContent.cap(),shelves),base=random.nextInt(8)+1+(power>>1)+random.nextInt(power+1);
        int value=slot==0?Math.max(base/3,1):slot==1?base*2/3+1:Math.max(base,power*2);
        return Math.min(Rules.i("compressed_bookshelves","maximum_level"),value);
    }
}
