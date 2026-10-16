package com.sumirelabs.sum1rem0d.mixin;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(PoiTypes.class)
public interface PathPoiAccess {
 @Invoker("registerBlockStates") static void sum1rem0d$register(Holder<PoiType> type,Set<BlockState> states) { throw new AssertionError(); }
}
