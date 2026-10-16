package com.sumirelabs.sum1rem0d.mixin;
import com.sumirelabs.sum1rem0d.game.ExtraEquipment;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(EquipmentLayerRenderer.class)
public abstract class RadiantArmorMixin {
 @ModifyVariable(method="renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",at=@At("HEAD"),argsOnly=true,ordinal=0)
 private int sum1rem0d$light(int light,EquipmentClientInfo.LayerType type,ResourceKey<?> asset,Model<?> model,Object state,ItemStack stack,PoseStack pose,SubmitNodeCollector collector,int original,Identifier texture,int outline,int order){return ExtraEquipment.glowing(stack)?0xF000F0:light;}
}
