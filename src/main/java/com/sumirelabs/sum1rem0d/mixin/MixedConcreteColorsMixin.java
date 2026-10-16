package com.sumirelabs.sum1rem0d.mixin;
import java.util.List;
import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.client.color.block.*;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockColors.class)
public abstract class MixedConcreteColorsMixin {
 @Unique private static final BlockTintSource sum1rem0d$COLOR=new com.sumirelabs.sum1rem0d.client.MixedConcreteTint();
 @Inject(method="getTintSources",at=@At("HEAD"),cancellable=true) private void sum1rem0d$list(BlockState state,CallbackInfoReturnable<List<BlockTintSource>> cir){if(state.is(MixedConcrete.block()))cir.setReturnValue(List.of(sum1rem0d$COLOR));}
 @Inject(method="getTintSource",at=@At("HEAD"),cancellable=true) private void sum1rem0d$one(BlockState state,int tint,CallbackInfoReturnable<BlockTintSource> cir){if(state.is(MixedConcrete.block()) && tint==0)cir.setReturnValue(sum1rem0d$COLOR);}
}
