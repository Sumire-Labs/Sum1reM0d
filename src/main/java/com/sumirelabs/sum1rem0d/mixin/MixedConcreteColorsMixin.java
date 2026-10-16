package com.sumirelabs.sum1rem0d.mixin;
import java.util.List;
import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.client.color.block.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockColors.class)
public abstract class MixedConcreteColorsMixin {
 @Unique private static final BlockTintSource sum1rem0d$COLOR=new com.sumirelabs.sum1rem0d.client.MixedConcreteTint();
 // Sodium builds its color-provider cache from register(), not calls to getTintSource().
 @SuppressWarnings("deprecation")
 @Inject(method="createDefault",at=@At("RETURN"))
 private static void sum1rem0d$register(CallbackInfoReturnable<BlockColors> cir){
  var colors=cir.getReturnValue();
  colors.register(List.of(sum1rem0d$COLOR),MixedConcrete.block(),ExtraContent.block("mixed_wool"),ExtraContent.block("mixed_bed"));
  colors.register(List.of(com.sumirelabs.sum1rem0d.client.GlowWireTint.INSTANCE),ExtraContent.block("glow_wire"));
 }
}
