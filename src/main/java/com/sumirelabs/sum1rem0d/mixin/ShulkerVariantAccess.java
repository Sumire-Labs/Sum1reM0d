package com.sumirelabs.sum1rem0d.mixin;
import java.util.Optional;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Shulker.class)
public interface ShulkerVariantAccess {
    @Invoker("setVariant") void sum1rem0d$setVariant(Optional<DyeColor> color);
}
