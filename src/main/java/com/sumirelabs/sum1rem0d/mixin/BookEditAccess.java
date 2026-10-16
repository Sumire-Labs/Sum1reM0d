package com.sumirelabs.sum1rem0d.mixin;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(BookEditScreen.class)
public interface BookEditAccess {
 @Accessor("book") ItemStack sum1rem0d$book();
 @Accessor("page") MultiLineEditBox sum1rem0d$page();
}
