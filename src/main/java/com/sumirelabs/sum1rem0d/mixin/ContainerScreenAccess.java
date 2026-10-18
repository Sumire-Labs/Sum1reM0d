package com.sumirelabs.sum1rem0d.mixin;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccess {
    @Accessor("leftPos") int sum1rem0d$leftPos();
    @Accessor("leftPos") void sum1rem0d$setLeftPos(int value);
    @Accessor("imageWidth") int sum1rem0d$imageWidth();
    @Accessor("imageHeight") int sum1rem0d$imageHeight();
    @Accessor("topPos") int sum1rem0d$topPos();
    @Accessor("menu") AbstractContainerMenu sum1rem0d$menu();
}
