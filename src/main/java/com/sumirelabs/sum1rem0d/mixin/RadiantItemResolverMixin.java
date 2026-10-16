package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.client.RadiantItemState;
import com.sumirelabs.sum1rem0d.game.ExtraEquipment;
import net.minecraft.client.renderer.item.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ItemOwner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemModelResolver.class)
public abstract class RadiantItemResolverMixin {
 @Inject(method="appendItemLayers",at=@At("HEAD"))private void sum1rem0d$mark(ItemStackRenderState state,ItemStack stack,ItemDisplayContext context,Level level,ItemOwner owner,int seed,CallbackInfo ci){if(ExtraEquipment.radiant(stack))((RadiantItemState)state).sum1rem0d$radiant(true);}
}
