package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.ExtraContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.village.poi.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(PoiType.class)
public abstract class DyedBedPoiMixin {
 @Inject(method="is",at=@At("HEAD"),cancellable=true)
 private void sum1rem0d$home(BlockState state,CallbackInfoReturnable<Boolean> cir){if(BuiltInRegistries.POINT_OF_INTEREST_TYPE.get(PoiTypes.HOME).isPresent() && (Object)this==BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(PoiTypes.HOME).value() && state.hasProperty(BlockStateProperties.BED_PART) && state.is(ExtraContent.block("mixed_bed")))cir.setReturnValue(state.getValue(BlockStateProperties.BED_PART)==BedPart.HEAD);}
}
