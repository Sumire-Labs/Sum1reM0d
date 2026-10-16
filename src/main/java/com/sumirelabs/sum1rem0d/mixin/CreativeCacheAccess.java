package com.sumirelabs.sum1rem0d.mixin;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(CreativeModeTabs.class)
public interface CreativeCacheAccess {
 @Accessor("CACHED_PARAMETERS") static void sum1rem0d$invalidate(CreativeModeTab.ItemDisplayParameters value){throw new AssertionError();}
}
