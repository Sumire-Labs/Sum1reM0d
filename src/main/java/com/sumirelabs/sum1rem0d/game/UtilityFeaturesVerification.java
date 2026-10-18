package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;

/** One fixture for the five requested utility features, including failure-prone signal/door transitions. */
final class UtilityFeaturesVerification {
    static void verify(MinecraftServer server,Consumer<String> passed)throws Exception{
        var config=Sum1reM0d.config();var saved=config.snapshot();var draft=saved.deepCopy();var features=draft.getAsJsonObject("features");
        for(var entry:features.entrySet())entry.getValue().getAsJsonObject().addProperty("enabled",false);
        for(var id:List.of("double_doors","glowstone_torch","fragility_curse","wooden_armor","optical_fiber"))features.getAsJsonObject(id).addProperty("enabled",true);
        config.update(draft,config.revision());
        var level=server.overworld();var actor=EconomyVerification.actor(server,level,"UtilityFeatures");
        try{
            var left=new BlockPos(3020,245,40);var right=left.east();
            var lower=Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.NORTH).setValue(DoorBlock.HINGE,DoorHingeSide.LEFT).setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER);
            for(var p:List.of(left,right))ServerVerification.place(level,p.below(),Blocks.STONE.defaultBlockState());
            ServerVerification.place(level,left,lower);ServerVerification.place(level,left.above(),lower.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));
            var other=lower.setValue(DoorBlock.HINGE,DoorHingeSide.RIGHT);ServerVerification.place(level,right,other);ServerVerification.place(level,right.above(),other.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));
            var door=(DoorBlock)Blocks.OAK_DOOR;
            door.setOpen(null,level,level.getBlockState(left),left,true);
            require(level.getBlockState(right).getValue(DoorBlock.OPEN)&&level.getBlockState(right.above()).getValue(DoorBlock.OPEN),"Opening lower half opens both partner halves");
            door.setOpen(null,level,level.getBlockState(right.above()),right.above(),false);
            require(!level.getBlockState(left).getValue(DoorBlock.OPEN),"Closing upper half closes the pair");
            ServerVerification.place(level,left.west(),Blocks.REDSTONE_BLOCK.defaultBlockState());
            require(level.getBlockState(left).getValue(DoorBlock.OPEN)&&level.getBlockState(right).getValue(DoorBlock.OPEN),"One-sided redstone opens both doors");
            ServerVerification.place(level,right.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());level.setBlock(left.west(),Blocks.AIR.defaultBlockState(),3);
            require(level.getBlockState(left).getValue(DoorBlock.OPEN)&&level.getBlockState(right).getValue(DoorBlock.OPEN),"Pair stays open while either side is powered");
            level.setBlock(right.east(),Blocks.AIR.defaultBlockState(),3);
            require(!level.getBlockState(left).getValue(DoorBlock.OPEN)&&!level.getBlockState(right).getValue(DoorBlock.OPEN),"Pair closes after both inputs turn off");
            passed.accept("Double doors synchronize lower/upper interactions and either-side redstone without oscillating");

            var torchPos=new BlockPos(3040,245,40);ServerVerification.place(level,torchPos.below(),Blocks.STONE.defaultBlockState());ServerVerification.place(level,torchPos,Blocks.WATER.defaultBlockState());
            var hit=new BlockHitResult(Vec3.atCenterOf(torchPos),Direction.UP,torchPos,false);var torchStack=new ItemStack(UtilityContent.item("glowstone_torch"));
            var placeContext=new BlockPlaceContext(actor,InteractionHand.MAIN_HAND,torchStack,hit);
            var placed=UtilityContent.block("glowstone_torch").getStateForPlacement(placeContext);
            require(placed.getValue(GlowstoneTorchBlock.WATERLOGGED)&&placed.getLightEmission()==15,"Underwater placement keeps source water and full light");
            ServerVerification.place(level,torchPos,placed);
            require(level.getBlockState(torchPos).getFluidState().is(Fluids.WATER),"Placed glowstone torch is waterlogged");
            var wallPos=torchPos.east(3);ServerVerification.place(level,wallPos.south(),Blocks.STONE.defaultBlockState());
            var wall=UtilityContent.block("wall_glowstone_torch").defaultBlockState().setValue(GlowstoneTorchBlock.FACING,Direction.NORTH).setValue(GlowstoneTorchBlock.WATERLOGGED,true);
            require(wall.canSurvive(level,wallPos)&&wall.getLightEmission()==15,"Wall torch supports waterlogging and light");ServerVerification.place(level,wallPos,wall);
            var torchInput=CraftingInput.of(1,2,List.of(new ItemStack(Items.GLOWSTONE_DUST),new ItemStack(Items.STICK)));
            require(new ConfiguredCraftingRecipe("glowstone_torch").assemble(torchInput).getCount()==4,"Torch recipe produces four");
            passed.accept("Standing/wall glowstone torches preserve water and light; recipe works");

            var curse=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,ModContent.id("fragility_curse")));
            var sword=new ItemStack(Items.IRON_SWORD);sword.enchant(curse,1);sword.hurtAndBreak(2,level,actor,stack->{});
            require(sword.getDamageValue()==6,"Curse multiplies actual native wear by three");
            require(FragilityCurse.apply(sword,level,0)==0,"Prevented durability loss stays zero");
            sword.setDamageValue(sword.getMaxDamage()-1);var breaks=new java.util.concurrent.atomic.AtomicInteger();sword.hurtAndBreak(1,level,actor,stack->breaks.incrementAndGet());
            require(sword.isEmpty()&&breaks.get()==1,"Cursed item breaks once through native lifecycle");
            passed.accept("Fragility curse multiplies native wear after durability prevention and breaks items once");

            var logs=new ItemStack(Items.OAK_LOG);var birch=new ItemStack(Items.BIRCH_LOG);
            var helmet=new ExtraCraftingRecipe("wooden_helmet").assemble(CraftingInput.of(3,2,List.of(logs,birch,logs,birch,ItemStack.EMPTY,logs)));
            require(helmet.is(ExtraContent.item("wooden_helmet")),"Armor recipe accepts mixed log species");
            var iron=List.of(Items.IRON_HELMET,Items.IRON_CHESTPLATE,Items.IRON_LEGGINGS,Items.IRON_BOOTS);var parts=List.of("helmet","chestplate","leggings","boots");var defense=List.of(1,2,1,1);
            for(int i=0;i<4;i++){
                var stack=new ItemStack(ExtraContent.item("wooden_"+parts.get(i)));
                require(stack.getMaxDamage()==new ItemStack(iron.get(i)).getMaxDamage(),"Wood durability equals iron: "+parts.get(i));
                double armor=stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream().filter(e->e.attribute().equals(Attributes.ARMOR)).mapToDouble(e->e.modifier().amount()).sum();
                require(armor==defense.get(i),"Wood armor has requested defense: "+parts.get(i));
            }
            passed.accept("Mixed-log wooden armor recipes create native armor with low defense and iron durability");

            var start=new BlockPos(3080,250,40);int length=36;var fiber=UtilityContent.block("optical_fiber");
            for(int i=0;i<length;i++)for(int z:new int[]{0,1})ServerVerification.place(level,start.east(i).south(z),fiber.defaultBlockState());
            var input=start.west();var source=input.west();var output=start.east(length);var output2=output.east();
            for(var p:List.of(input,source,output,output2))ServerVerification.place(level,p.below(),Blocks.STONE.defaultBlockState());
            ServerVerification.place(level,source,Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.defaultBlockState().setValue(BlockStateProperties.POWER,7));
            ServerVerification.place(level,input,Blocks.REDSTONE_WIRE.defaultBlockState());ServerVerification.place(level,output,Blocks.REDSTONE_WIRE.defaultBlockState());ServerVerification.place(level,output2,Blocks.REDSTONE_WIRE.defaultBlockState());
            OpticalFiberNetwork.invalidate(level);OpticalFiberNetwork.update(level,start);
            require(level.getBlockState(start).getValue(OpticalFiberBlock.POWER)==7&&level.getBlockState(start.east(length-1)).getValue(OpticalFiberBlock.POWER)==7,"Long cable preserves analog power seven");
            require(level.getBlockState(output).getValue(BlockStateProperties.POWER)==7&&level.getBlockState(output2).getValue(BlockStateProperties.POWER)==6,"Cable hands power back to ordinary attenuating dust");
            level.setBlock(source,Blocks.AIR.defaultBlockState(),3);OpticalFiberNetwork.invalidate(level);var nodes=OpticalFiberNetwork.update(level,start);
            for(var p:nodes)require(level.getBlockState(p).getValue(BlockStateProperties.POWER)==0,"Loop and attached dust turn off without feedback: "+p);
            var cableInput=CraftingInput.of(3,3,List.of(new ItemStack(Items.GLASS_PANE),new ItemStack(Items.GLASS_PANE),new ItemStack(Items.GLASS_PANE),new ItemStack(Items.REDSTONE),new ItemStack(Items.GLOWSTONE),new ItemStack(Items.REDSTONE),new ItemStack(Items.GLASS_PANE),new ItemStack(Items.GLASS_PANE),new ItemStack(Items.GLASS_PANE)));
            require(new ConfiguredCraftingRecipe("optical_fiber").assemble(cableInput).getCount()==8,"Fiber recipe produces eight");
            passed.accept("Long looped fiber preserves analog strength, feeds native dust, and fully switches off without self-powering");

            for(var id:List.of("double_doors","glowstone_torch","fragility_curse","wooden_armor","optical_fiber"))features.getAsJsonObject(id).addProperty("enabled",false);config.update(draft,config.revision());
            require(new ConfiguredCraftingRecipe("glowstone_torch").assemble(torchInput).isEmpty()&&new ConfiguredCraftingRecipe("optical_fiber").assemble(cableInput).isEmpty()&&new ExtraCraftingRecipe("wooden_helmet").assemble(CraftingInput.of(3,2,List.of(logs,birch,logs,birch,ItemStack.EMPTY,logs))).isEmpty(),"OFF gates all new recipes");
            var unmodified=new ItemStack(Items.IRON_SWORD);unmodified.enchant(curse,1);unmodified.hurtAndBreak(2,level,actor,stack->{});require(unmodified.getDamageValue()==2,"OFF restores ordinary durability wear");
            door.setOpen(null,level,level.getBlockState(left),left,true);require(!level.getBlockState(right).getValue(DoorBlock.OPEN),"OFF restores independent doors");
            passed.accept("Config OFF independently restores door/wear behavior and disables the new recipes");
        }finally{actor.discard();config.update(saved,config.revision());OpticalFiberNetwork.invalidate(level);}
    }
}
