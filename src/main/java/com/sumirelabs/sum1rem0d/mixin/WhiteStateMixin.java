package com.sumirelabs.sum1rem0d.mixin;

import com.sumirelabs.sum1rem0d.client.WhiteRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.*;

@Mixin(LivingEntityRenderState.class)
public abstract class WhiteStateMixin implements WhiteRenderState {
    @Unique private boolean sum1rem0d$white;
    public boolean sum1rem0d$isWhite() { return sum1rem0d$white; }
    public void sum1rem0d$setWhite(boolean value) { sum1rem0d$white = value; }
}
