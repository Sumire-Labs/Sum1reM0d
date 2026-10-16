package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.client.RadiantItemState;
import com.sumirelabs.sum1rem0d.config.ClientRules;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemStackRenderState.class)
public abstract class RadiantItemRenderMixin implements RadiantItemState {
 @Unique private boolean sum1rem0d$glowing;
 public void sum1rem0d$radiant(boolean value){sum1rem0d$glowing=value;}
 @Inject(method="clear",at=@At("HEAD"))private void sum1rem0d$clear(CallbackInfo ci){sum1rem0d$glowing=false;}
 @ModifyVariable(method="submit",at=@At("HEAD"),argsOnly=true,ordinal=0)private int sum1rem0d$light(int light){return sum1rem0d$glowing && ClientRules.enabled("redstone_equipment") && ClientRules.bool("redstone_equipment","glow_enabled")?0xF000F0:light;}
}
