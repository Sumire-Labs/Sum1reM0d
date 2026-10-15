package com.sumirelabs.sum1rem0d.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyboardHandler.class)
public interface KeyboardInputAccessor {
    @Invoker("charTyped") void sum1rem0d$charTyped(long window, CharacterEvent event);
}
