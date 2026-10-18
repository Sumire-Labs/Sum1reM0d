package com.sumirelabs.sum1rem0d.mixin;

import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Optional access to NeoForge's wrapper so the fuel slot limit stays scoped to furnace slot 1. */
@Pseudo
@Mixin(targets="net.neoforged.neoforge.transfer.item.VanillaContainerWrapper",remap=false)
public interface NeoVanillaContainerAccess {
    @Accessor(value="container",remap=false)
    Container sum1rem0d$getContainer();
}
