package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.MaterialNames;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemStack.class)
public abstract class PermanentMaterialNamesMixin {
 @Inject(method="getHoverName",at=@At("HEAD"),cancellable=true)private void sum1rem0d$name(CallbackInfoReturnable<Component> cir){var name=MaterialNames.name(BuiltInRegistries.ITEM.getKey(((ItemStack)(Object)this).getItem()));if(name!=null)cir.setReturnValue(name);}
}
