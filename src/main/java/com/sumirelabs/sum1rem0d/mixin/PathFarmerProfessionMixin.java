package com.sumirelabs.sum1rem0d.mixin;
import java.util.function.Predicate;
import com.sumirelabs.sum1rem0d.game.*;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.*;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(VillagerProfession.class)
public abstract class PathFarmerProfessionMixin {
 @Inject(method={"heldJobSite","acquirableJobSite"},at=@At("RETURN"),cancellable=true)
 private void sum1rem0d$job(CallbackInfoReturnable<Predicate<Holder<PoiType>>> cir) {
  var original=cir.getReturnValue();
  boolean farmer=((VillagerProfession)(Object)this).name().equals(net.minecraft.network.chat.Component.translatable("entity.minecraft.villager.farmer"));
  cir.setReturnValue(poi -> farmer && Rules.on("path_farmer") ? poi.is(PathFarmer.KEY) : (!poi.is(PathFarmer.KEY) || Rules.on("path_farmer")) && original.test(poi));
 }
}
