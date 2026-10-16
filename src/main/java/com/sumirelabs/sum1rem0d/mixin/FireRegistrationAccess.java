package com.sumirelabs.sum1rem0d.mixin;
import net.minecraft.world.level.block.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(FireBlock.class)
public interface FireRegistrationAccess {@Invoker("setFlammable")void sum1rem0d$flammable(Block block,int spread,int burn);}
