package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.ClientRules;
import com.sumirelabs.sum1rem0d.game.*;
import com.sumirelabs.sum1rem0d.network.ConfigStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;

final class PlacementRepairClientChecks {
    static void verify(Minecraft mc){
        verifyCableModels(mc);
        var previous=ClientRules.snapshot();var fixture=previous.deepCopy();var shelf=fixture.getAsJsonObject("features").getAsJsonObject("shelf_weapon");
        shelf.addProperty("enabled",true);shelf.addProperty("target_item_ids","");shelf.addProperty("damage",42);shelf.addProperty("guard_enabled",true);shelf.addProperty("guard_damage_reduction",.5);
        try{
            ClientRules.accept(new ConfigStatePayload(ClientRules.revision,false,fixture.toString(),"",0,false));
            var item=BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace("poplar_shelf")).orElse(Items.AIR);
            if(item==Items.AIR)throw new AssertionError("Native poplar shelf is unavailable in this client fixture");
            var stack=new ItemStack(item);var tooltip=stack.getTooltipLines(Item.TooltipContext.of(mc.level),mc.player,TooltipFlag.NORMAL);
            if(tooltip.stream().noneMatch(c->c.getString().contains("42"))||tooltip.stream().noneMatch(c->c.getString().contains("50%")))throw new AssertionError("Native shelf tooltip does not show configured damage and guard reduction: "+tooltip);
            var tab=BuiltInRegistries.CREATIVE_MODE_TAB.getValue(Identifier.withDefaultNamespace("combat"));
            tab.buildContents(new CreativeModeTab.ItemDisplayParameters(mc.level.enabledFeatures(),true,mc.level.registryAccess()));
            if(tab.getDisplayItems().stream().noneMatch(s->s.is(item)))throw new AssertionError("Poplar shelf is missing from the native Combat tab");
            shelf.addProperty("damage",84);ClientRules.accept(new ConfigStatePayload(ClientRules.revision,false,fixture.toString(),"",0,false));
            if(stack.getTooltipLines(Item.TooltipContext.of(mc.level),mc.player,TooltipFlag.NORMAL).stream().noneMatch(c->c.getString().contains("84")))throw new AssertionError("Shelf tooltip does not follow a live damage Config change");
        }finally{ClientRules.accept(new ConfigStatePayload(ClientRules.revision,false,previous.toString(),"",0,false));}
    }
    private static void verifyCableModels(Minecraft mc){
        //? if >=26.3 {
        var fiber=UtilityContent.block("optical_fiber");
        for(int power:new int[]{0,15})for(var direction:Direction.values()){
            var state=fiber.defaultBlockState().setValue(OpticalFiberBlock.POWER,power).setValue(OpticalFiberBlock.CONNECTIONS.get(direction),true);
            var parts=new java.util.ArrayList<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>();
            mc.getModelManager().getBlockStateModelSet().get(state).collectParts(net.minecraft.util.RandomSource.create(1),parts);
            float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
            var faces=new java.util.ArrayList<Direction>();faces.add(null);faces.addAll(java.util.List.of(Direction.values()));
            for(var part:parts)for(var face:faces)for(var quad:part.getQuads(face))for(int vertex=0;vertex<4;vertex++){
                var p=quad.position(vertex);float coordinate=switch(direction.getAxis()){case X->p.x();case Y->p.y();case Z->p.z();};
                min=Math.min(min,coordinate);max=Math.max(max,coordinate);
            }
            float expectedMin=direction.getAxisDirection()==Direction.AxisDirection.NEGATIVE?0:5f/16;
            float expectedMax=direction.getAxisDirection()==Direction.AxisDirection.POSITIVE?1:11f/16;
            if(Math.abs(min-expectedMin)>.0001||Math.abs(max-expectedMax)>.0001)throw new AssertionError("Baked cable arm points the wrong way: "+direction+", power="+power+", bounds="+min+".."+max);
        }
        //?}
    }
}
