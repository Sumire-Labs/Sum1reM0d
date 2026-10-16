package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.client.DailyTextures;
import java.util.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MultiPackResourceManager.class)
public abstract class DailyTextureResourcesMixin {
 @Inject(method="getResource",at=@At("RETURN"),cancellable=true)
 private void sum1rem0d$one(Identifier id,CallbackInfoReturnable<Optional<Resource>> cir) { cir.setReturnValue(cir.getReturnValue().map(r->DailyTextures.replace(id,r,(ResourceManager)(Object)this))); }
 @Inject(method="listResources",at=@At("RETURN"),cancellable=true)
 private void sum1rem0d$list(String directory,@Coerce Object selector,CallbackInfoReturnable<Map<Identifier,Resource>> cir) { var map=new TreeMap<>(cir.getReturnValue()); map.replaceAll((id,r)->DailyTextures.replace(id,r,(ResourceManager)(Object)this));cir.setReturnValue(map); }
 @Inject(method="getResourceStack",at=@At("RETURN"),cancellable=true)
 private void sum1rem0d$stack(Identifier id,CallbackInfoReturnable<List<Resource>> cir) { cir.setReturnValue(cir.getReturnValue().stream().map(r->DailyTextures.replace(id,r,(ResourceManager)(Object)this)).toList()); }
 @Inject(method="listResourceStacks",at=@At("RETURN"),cancellable=true)
 private void sum1rem0d$stacks(CallbackInfoReturnable<Map<Identifier,List<Resource>>> cir) { var map=new TreeMap<>(cir.getReturnValue()); map.replaceAll((id,list)->list.stream().map(r->DailyTextures.replace(id,r,(ResourceManager)(Object)this)).toList()); cir.setReturnValue(map); }
}
