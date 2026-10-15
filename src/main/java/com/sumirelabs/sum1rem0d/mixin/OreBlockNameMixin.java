package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.game.Rules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Block.class)
public abstract class OreBlockNameMixin {
    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void sum1rem0d$name(CallbackInfoReturnable<MutableComponent> cir) {
        if (!ClientGameState.daily("diamond_disguise")) return;
        Block original = (Block) (Object) this;
        var id = Rules.id(ClientRules.text("diamond_disguise", "replacement"));
        if (id == null) return; var replacement = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        if (replacement != null && replacement != original && Rules.matches(original.defaultBlockState(), ClientRules.list("diamond_disguise", "target_blocks"))) cir.setReturnValue(replacement.getName());
    }
}
