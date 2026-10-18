package com.sumirelabs.sum1rem0d.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(EnchantmentMenu.class)
public abstract class CompressedBookshelfEnchantingMixin {
    @WrapOperation(method="lambda$slotsChanged$0",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentCost(Lnet/minecraft/util/RandomSource;IILnet/minecraft/world/item/ItemStack;)I"))
    private int sum1rem0d$compressedPower(RandomSource random,int slot,int shelves,ItemStack stack,Operation<Integer> original,ItemStack input,Level level,BlockPos table){
        if(!Rules.on("compressed_bookshelves"))return original.call(random,slot,shelves,stack);
        int power=BookshelfEnchanting.adjustPower(level,table,shelves);
        return Rules.on("compressed_bookshelves")&&Rules.b("compressed_bookshelves","expanded_levels")?BookshelfEnchanting.expandedCost(random,slot,power,stack):original.call(random,slot,power,stack);
    }
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="getEnchantmentList")
    private java.util.List<net.minecraft.world.item.enchantment.EnchantmentInstance> sum1rem0d$usableHighLevelCandidates(net.minecraft.core.RegistryAccess registry,ItemStack stack,int slot,int level,Operation<java.util.List<net.minecraft.world.item.enchantment.EnchantmentInstance>> original){
        var result=original.call(registry,stack,slot,level);
        if(result.isEmpty()&&level>30&&Rules.on("compressed_bookshelves")&&Rules.b("compressed_bookshelves","expanded_levels"))return original.call(registry,stack,slot,30);
        return result;
    }
}
