package com.sumirelabs.sum1rem0d.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Client-only access to the final slot coordinates while a scrollable chest screen is open. */
@Mixin(Slot.class)
public interface CopperChestSlotLayoutMixin {
    @Mutable @Accessor("x") void sum1rem0d$setX(int x);
    @Mutable @Accessor("y") void sum1rem0d$setY(int y);
}
