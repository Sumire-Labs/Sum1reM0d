package com.sumirelabs.sum1rem0d.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MouseHandler.class)
public interface MouseInputAccessor {
    @Invoker("onButton") void sum1rem0d$onButton(long window, MouseButtonInfo button, int action);
    //? if >=26.3 {
    @Invoker("onMove") void sum1rem0d$onMove(long window, double x, double y, double dx, double dy);
    //?} else {
    /*@Invoker("onMove") void sum1rem0d$onMove(long window, double x, double y);
    *///?}
}
