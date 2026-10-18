package com.sumirelabs.sum1rem0d.game;

import com.google.gson.JsonObject;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.Vec3;

/** Native server regression cases for automation, including loaded cross-chunk I/O. */
final class AutomationVerification {
    private AutomationVerification() {}

    static void verify(MinecraftServer server, Consumer<String> passed) throws Exception {
        var config = Sum1reM0d.config();
        var original = config.snapshot();
        var level = server.overworld();
        var draft = original.deepCopy();
        var features = draft.getAsJsonObject("features");
        for (var id : List.of("automated_crafting", "jukebox_grinder", "washing_cauldron", "tesla_coil", "auto_clicker", "sulfur_gunpowder"))
            features.getAsJsonObject(id).addProperty("enabled", true);
        // The native clicker fixture uses vanilla redstone power; keep the independent inversion feature out of this test.
        features.getAsJsonObject("redstone_inversion").addProperty("enabled", false);
        features.getAsJsonObject("automated_crafting").addProperty("interval_ticks", 1);
        features.getAsJsonObject("washing_cauldron").addProperty("interval_ticks", 1);
        features.getAsJsonObject("large_cauldron").addProperty("enabled", true);
        features.getAsJsonObject("large_cauldron").addProperty("auto_merge", true);
        features.getAsJsonObject("tesla_coil").addProperty("range", 4);
        features.getAsJsonObject("tesla_coil").addProperty("damage", 2);
        features.getAsJsonObject("tesla_coil").addProperty("interval_ticks", 1);
        features.getAsJsonObject("tesla_coil").addProperty("affect_players", false);
        features.getAsJsonObject("auto_clicker").addProperty("interval_ticks",5);
        features.getAsJsonObject("auto_clicker").addProperty("entity_interaction",true);
        config.update(draft, config.revision());
        try {
            verifyRecipes(level);
            passed.accept("Automation grinder conversions, sulfur recipe and config gate");
            verifyCrafterAcrossChunk(level, passed);
            verifyWasherAcrossChunk(level, passed);
            verifyWasherWithMergedCauldrons(level, config, passed);
            verifyJukeboxInventory(server, level, passed);
            verifyTesla(server, level, passed);
            verifyMachinePersistence(server, level, passed);
            verifyIndependentClicker(server, level, passed);
        } finally {
            config.update(original, config.revision());
        }
    }

    private static void verifyRecipes(ServerLevel level) throws Exception {
        require(AutomationProcessing.grindResult(new ItemStack(Items.RAW_IRON)).is(AutomationContent.item("iron_dust")) && AutomationProcessing.grindResult(new ItemStack(Items.RAW_IRON)).getCount() == 2, "Raw iron becomes two iron dust");
        require(AutomationProcessing.grindResult(new ItemStack(Items.RAW_COPPER)).getCount() == 2 && AutomationProcessing.grindResult(new ItemStack(Items.RAW_GOLD)).getCount() == 2, "Raw copper and gold each become two dust");
        require(AutomationProcessing.grindResult(new ItemStack(Items.COBBLESTONE)).is(Items.GRAVEL), "Cobblestone grinds to gravel");
        require(AutomationProcessing.grindResult(new ItemStack(Items.GRAVEL)).is(Items.SAND), "Gravel grinds to sand");
        require(AutomationProcessing.grindResult(new ItemStack(Items.SAND)).is(AutomationContent.item("dust")), "Sand grinds to the placed dust block item");
        require(AutomationProcessing.grindResult(new ItemStack(Items.BLAZE_ROD)).is(Items.BLAZE_POWDER) && AutomationProcessing.grindResult(new ItemStack(Items.BLAZE_ROD)).getCount() == 3, "Blaze rod grinds to three blaze powder");
        require(AutomationProcessing.grindResult(new ItemStack(Items.SUGAR_CANE)).getCount() == 2 && AutomationProcessing.grindResult(new ItemStack(Items.BONE)).getCount() == 5, "Sugar cane and bone recipes");
        require(BuiltInRegistries.BLOCK.getKey(AutomationContent.block("dust")).toString().equals("sum1rem0d:dust"), "Dust is a registered block, not a loose-only item");
        var input = CraftingInput.of(3, 1, List.of(new ItemStack(AutomationContent.sulfur()), new ItemStack(Items.CHARCOAL), new ItemStack(AutomationContent.item("dust"))));
        var recipe = new ConfiguredCraftingRecipe("sulfur_gunpowder");
        require(recipe.matches(input, level) && recipe.assemble(input).is(Items.GUNPOWDER) && recipe.assemble(input).getCount() == 4, "Sulfur, charcoal and dust craft four gunpowder");
        var off = Sum1reM0d.config().snapshot(); off.getAsJsonObject("features").getAsJsonObject("sulfur_gunpowder").addProperty("enabled", false); Sum1reM0d.config().update(off, Sum1reM0d.config().revision());
        require(!recipe.matches(input, level) && recipe.assemble(input).isEmpty(), "Sulfur recipe OFF blocks crafting");
        var on = Sum1reM0d.config().snapshot(); on.getAsJsonObject("features").getAsJsonObject("sulfur_gunpowder").addProperty("enabled", true); Sum1reM0d.config().update(on, Sum1reM0d.config().revision());
    }

    private static void verifyCrafterAcrossChunk(ServerLevel level, Consumer<String> passed) {
        var pos = new BlockPos(15, 240, 760);
        var state = Blocks.CRAFTER.defaultBlockState().setValue(BlockStateProperties.ORIENTATION, FrontAndTop.fromFrontAndTop(Direction.EAST, Direction.UP));
        ServerVerification.place(level, pos, state);
        ServerVerification.place(level, pos.west(), Blocks.BARREL.defaultBlockState());
        ServerVerification.place(level, pos.east(), Blocks.CHEST.defaultBlockState());
        require(level.getBlockEntity(pos) instanceof CrafterBlockEntity, "Vanilla crafter block entity exists");
        var crafter = (CrafterBlockEntity) level.getBlockEntity(pos);
        var source = (net.minecraft.world.Container) level.getBlockEntity(pos.west());
        var output = (net.minecraft.world.Container) level.getBlockEntity(pos.east());
        crafter.setItem(0, new ItemStack(Items.OAK_LOG));
        source.setItem(0, new ItemStack(Items.OAK_LOG));
        require(!crafter.isSlotDisabled(0) && crafter.getItem(0).is(Items.OAK_LOG), "Native crafter retains its enabled oak-log recipe sample");
        var sample=CraftingInput.of(3,3,java.util.stream.IntStream.range(0,9).mapToObj(slot->slot==0?new ItemStack(Items.OAK_LOG):ItemStack.EMPTY).toList());
        require(level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,sample,level).isPresent(), "Vanilla oak-log planks recipe resolves from the crafter sample");
        require(AutomationProcessing.enabled("automated_crafting")&&output!=null&&source!=null,"Native crafter automation is enabled and both chunk-boundary containers are loaded");
        for (int slot = 0; slot < output.getContainerSize(); slot++) output.setItem(slot, new ItemStack(Items.STONE, 64));
        require(!AutomationProcessing.craftFromVanillaCrafter(level, pos, crafter) && source.getItem(0).getCount() == 1, "Full cross-boundary output rejects craft without consuming input");
        output.clearContent();
        require(AutomationProcessing.craftFromVanillaCrafter(level, pos, crafter), "Vanilla crafter draws from side barrel across chunk boundary");
        require(source.getItem(0).isEmpty() && crafter.getItem(0).is(Items.OAK_LOG) && output.getItem(0).is(Items.OAK_PLANKS) && output.getItem(0).getCount() == 4, "Cross-boundary crafting consumes only storage input and outputs once");
        passed.accept("Vanilla crafter with cross-chunk side input/output and full-output rollback");
    }

    private static void verifyWasherAcrossChunk(ServerLevel level, Consumer<String> passed) {
        var pos = new BlockPos(15, 240, 780);
        ServerVerification.place(level, pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 3));
        ServerVerification.place(level, pos.above(), Blocks.BARREL.defaultBlockState());
        ServerVerification.place(level, pos.east(), Blocks.CHEST.defaultBlockState());
        var input = (net.minecraft.world.Container) level.getBlockEntity(pos.above());
        var output = (net.minecraft.world.Container) level.getBlockEntity(pos.east());
        input.setItem(0, new ItemStack(AutomationContent.item("iron_dust")));
        require(AutomationProcessing.tryNativeWash(level, pos), "Vanilla water cauldron washes chest/barrel input with output across chunk boundary");
        require(input.getItem(0).isEmpty() && output.getItem(0).is(AutomationContent.item("washed_iron_dust")) && level.getBlockState(pos).getValue(BlockStateProperties.LEVEL_CAULDRON) == 2, "Successful wash stores output and consumes exactly one water level");
        for (int slot = 0; slot < output.getContainerSize(); slot++) output.setItem(slot, new ItemStack(Items.STONE, 64));
        input.setItem(0, new ItemStack(AutomationContent.item("iron_dust")));
        require(!AutomationProcessing.tryNativeWash(level, pos) && input.getItem(0).getCount() == 1 && level.getBlockState(pos).getValue(BlockStateProperties.LEVEL_CAULDRON) == 2, "Full cross-boundary wash output keeps dust and water");
        output.clearContent();
        var unrelated=new ItemStack(Items.PAPER);unrelated.set(net.minecraft.core.component.DataComponents.DYED_COLOR,new net.minecraft.world.item.component.DyedItemColor(0xB03030));input.setItem(0,unrelated);
        require(!AutomationProcessing.tryNativeWash(level,pos)&&ItemStack.matches(input.getItem(0),unrelated)&&level.getBlockState(pos).getValue(BlockStateProperties.LEVEL_CAULDRON)==2,
                "An unrelated item with a manually attached dye component is not washed or consumed");
        var bundleItem=BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_bundle"));
        net.minecraft.world.item.component.BundleContents.Mutable bundleContents;
        //? if >=26.3 {
        bundleContents=new net.minecraft.world.item.component.BundleContents.Mutable();
        //?} else {
        /*bundleContents=new net.minecraft.world.item.component.BundleContents.Mutable(net.minecraft.world.item.component.BundleContents.EMPTY);
        *///?}
        bundleContents.tryInsert(new ItemStack(Items.DIAMOND,2));var expectedBundleContents=bundleContents.toImmutable();
        var bundle=new ItemStack(bundleItem);bundle.set(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS,expectedBundleContents);bundle.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("保管用の赤い袋"));bundle.set(net.minecraft.core.component.DataComponents.DYED_COLOR,new net.minecraft.world.item.component.DyedItemColor(0xB03030));var expectedBundleName=bundle.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        input.setItem(0,bundle.copy());
        require(AutomationProcessing.tryNativeWash(level,pos),"Dye wash accepts a colored vanilla bundle");
        var whiteBundle=output.getItem(0);
        var bundleOutputContents=whiteBundle.get(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS);var bundleOutputName=whiteBundle.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);var bundleOutputColor=whiteBundle.get(net.minecraft.core.component.DataComponents.DYED_COLOR);
        boolean bundleType=whiteBundle.is(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("white_bundle")));
        boolean bundleContentsMatch=java.util.Objects.equals(bundleOutputContents,expectedBundleContents);boolean bundleNameMatch=java.util.Objects.equals(bundleOutputName,expectedBundleName);boolean bundleColorMatch=java.util.Objects.equals(bundleOutputColor,new net.minecraft.world.item.component.DyedItemColor(0xFFFFFF));
        require(bundleType&&bundleContentsMatch&&bundleNameMatch&&bundleColorMatch,
                "Bundle whitening keeps exact contents/name and applies the white tint [id="+BuiltInRegistries.ITEM.getKey(whiteBundle.getItem())+", count="+whiteBundle.getCount()+", contents="+bundleOutputContents+", expectedContents="+expectedBundleContents+", name="+bundleOutputName+", expectedName="+expectedBundleName+", color="+bundleOutputColor+", expectedColor=white, water="+level.getBlockState(pos)+"]");

        output.clearContent();var redShulker=BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_shulker_box"));
        var shulkerContents=net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.stream.IntStream.range(0,27).mapToObj(slot->slot==0?new ItemStack(Items.EMERALD,3):ItemStack.EMPTY).toList());
        var shulker=new ItemStack(redShulker);shulker.set(net.minecraft.core.component.DataComponents.CONTAINER,shulkerContents);shulker.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("保管用の赤い箱"));shulker.set(net.minecraft.core.component.DataComponents.DYED_COLOR,new net.minecraft.world.item.component.DyedItemColor(0xB03030));var expectedShulkerName=shulker.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        input.setItem(0,shulker.copy());
        require(AutomationProcessing.tryNativeWash(level,pos),"Dye wash accepts a colored vanilla shulker box");var whiteShulker=output.getItem(0);
        require(whiteShulker.is(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("white_shulker_box")))
                        &&whiteShulker.get(net.minecraft.core.component.DataComponents.CONTAINER).equals(shulkerContents)
                        &&whiteShulker.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME).equals(expectedShulkerName)
                        &&whiteShulker.get(net.minecraft.core.component.DataComponents.DYED_COLOR).equals(new net.minecraft.world.item.component.DyedItemColor(0xFFFFFF)),
                "Shulker whitening keeps exact inventory/name and applies the white tint");

        var bundleVariants=new java.util.ArrayList<String>();bundleVariants.add("bundle");for(var color:net.minecraft.world.item.DyeColor.values())bundleVariants.add(color.getName()+"_bundle");
        for(String variant:bundleVariants){var item=BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(variant));var colored=new ItemStack(item);String title="colored "+variant;colored.set(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS,expectedBundleContents);colored.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(title));colored.set(net.minecraft.core.component.DataComponents.DYED_COLOR,new net.minecraft.world.item.component.DyedItemColor(0xB03030));
            level.setBlock(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON,3),3);output.clearContent();input.setItem(0,colored.copy());require(AutomationProcessing.tryNativeWash(level,pos),"Every dyed vanilla bundle variant washes: "+variant);var result=output.getItem(0);
            require(result.is(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("white_bundle")))&&result.get(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS).equals(expectedBundleContents)&&result.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME).equals(net.minecraft.network.chat.Component.literal(title))&&result.get(net.minecraft.core.component.DataComponents.DYED_COLOR).equals(new net.minecraft.world.item.component.DyedItemColor(0xFFFFFF)),"Dyed bundle variant preserves contents/name and explicitly becomes white: "+variant);
        }
        var shulkerVariants=new java.util.ArrayList<String>();shulkerVariants.add("shulker_box");for(var color:net.minecraft.world.item.DyeColor.values())shulkerVariants.add(color.getName()+"_shulker_box");
        for(String variant:shulkerVariants){var item=BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(variant));var colored=new ItemStack(item);String title="colored "+variant;colored.set(net.minecraft.core.component.DataComponents.CONTAINER,shulkerContents);colored.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(title));colored.set(net.minecraft.core.component.DataComponents.DYED_COLOR,new net.minecraft.world.item.component.DyedItemColor(0xB03030));
            level.setBlock(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON,3),3);output.clearContent();input.setItem(0,colored.copy());require(AutomationProcessing.tryNativeWash(level,pos),"Every dyed vanilla shulker variant washes: "+variant);var result=output.getItem(0);
            require(result.is(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("white_shulker_box")))&&result.get(net.minecraft.core.component.DataComponents.CONTAINER).equals(shulkerContents)&&result.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME).equals(net.minecraft.network.chat.Component.literal(title))&&result.get(net.minecraft.core.component.DataComponents.DYED_COLOR).equals(new net.minecraft.world.item.component.DyedItemColor(0xFFFFFF)),"Dyed shulker variant preserves contents/name and explicitly becomes white: "+variant);
        }

        level.setBlock(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON,3),3);output.clearContent();
        var leather=new ItemStack(Items.LEATHER_HELMET);leather.setDamageValue(7);leather.set(net.minecraft.core.component.DataComponents.DYED_COLOR,new net.minecraft.world.item.component.DyedItemColor(0x123456));leather.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("傷ついた革帽子"));var expectedLeatherName=leather.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);input.setItem(0,leather.copy());
        require(AutomationProcessing.tryNativeWash(level,pos),"Wash cleans a dyed leather armor component");var whiteLeather=output.getItem(0);
        require(whiteLeather.is(Items.LEATHER_HELMET)&&whiteLeather.getDamageValue()==7
                        &&whiteLeather.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME).equals(expectedLeatherName)
                        &&whiteLeather.get(net.minecraft.core.component.DataComponents.DYED_COLOR).equals(new net.minecraft.world.item.component.DyedItemColor(0xFFFFFF)),
                "Leather whitening retains durability and custom name while setting explicit white color");

        output.clearContent();var mixedWool=DyeTextiles.stack("mixed_wool",0x3456AB);mixedWool.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("混色の羊毛"));var expectedMixedWoolName=mixedWool.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);input.setItem(0,mixedWool.copy());
        require(AutomationProcessing.tryNativeWash(level,pos)&&output.getItem(0).is(ExtraContent.item("mixed_wool"))
                        &&output.getItem(0).get(net.minecraft.core.component.DataComponents.DYED_COLOR).equals(new net.minecraft.world.item.component.DyedItemColor(0xFFFFFF))
                        &&output.getItem(0).get(net.minecraft.core.component.DataComponents.CUSTOM_NAME).equals(expectedMixedWoolName),
                "Mod mixed-color wool is whitened without dropping its item identity or custom name");

        passed.accept("Vanilla water cauldron with cross-chunk barrel input/output and full-output rollback");
    }

    private static void verifyWasherWithMergedCauldrons(ServerLevel level,com.sumirelabs.sum1rem0d.config.ConfigStore config,Consumer<String> passed)throws Exception{
        var pos=new BlockPos(31,240,780);
        ServerVerification.place(level,pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON,2));
        ServerVerification.place(level,pos.east(),Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON,1));
        var large=MobilityContent.largeCauldron();
        require(level.getBlockState(pos).getBlock()==large&&level.getBlockState(pos.east()).getBlock()==large&&large.availableWater(level,pos)==3,
                "Adjacent vanilla water cauldrons merge before automated washing and expose their combined water");
        ServerVerification.place(level,pos.above(),Blocks.BARREL.defaultBlockState());
        ServerVerification.place(level,pos.south(),Blocks.CHEST.defaultBlockState());
        var firstInput=(net.minecraft.world.Container)level.getBlockEntity(pos.above());var firstOutput=(net.minecraft.world.Container)level.getBlockEntity(pos.south());
        firstInput.setItem(0,new ItemStack(AutomationContent.item("iron_dust")));
        require(AutomationProcessing.tryNativeWash(level,pos)&&firstInput.getItem(0).isEmpty()&&firstOutput.getItem(0).is(AutomationContent.item("washed_iron_dust"))
                        &&large.availableWater(level,pos.east())==2,
                "Merged 2x1 cauldron washes from an upper barrel and consumes one shared water unit");
        ServerVerification.place(level,pos.east().above(),Blocks.BARREL.defaultBlockState());
        ServerVerification.place(level,pos.east().south(),Blocks.CHEST.defaultBlockState());
        var secondInput=(net.minecraft.world.Container)level.getBlockEntity(pos.east().above());var secondOutput=(net.minecraft.world.Container)level.getBlockEntity(pos.east().south());
        secondInput.setItem(0,new ItemStack(AutomationContent.item("iron_dust")));
        require(AutomationProcessing.tryNativeWash(level,pos.east())&&secondInput.getItem(0).isEmpty()&&secondOutput.getItem(0).is(AutomationContent.item("washed_iron_dust"))
                        &&large.availableWater(level,pos)==1,
                "Washing from the other merged cell consumes the same persisted shared water pool");
        for(int slot=0;slot<secondOutput.getContainerSize();slot++)secondOutput.setItem(slot,new ItemStack(Items.STONE,64));
        secondInput.setItem(0,new ItemStack(AutomationContent.item("iron_dust")));
        require(!AutomationProcessing.tryNativeWash(level,pos.east())&&secondInput.getItem(0).getCount()==1&&large.availableWater(level,pos)==1,
                "Full side output keeps the input and every shared water cell unchanged");
        var off=config.snapshot();off.getAsJsonObject("features").getAsJsonObject("washing_cauldron").addProperty("enabled",false);config.update(off,config.revision());
        secondOutput.clearContent();
        require(!AutomationProcessing.tryNativeWash(level,pos.east())&&secondInput.getItem(0).getCount()==1&&large.availableWater(level,pos)==1,
                "Washing Config OFF preserves input and merged-cauldron water");
        var on=config.snapshot();on.getAsJsonObject("features").getAsJsonObject("washing_cauldron").addProperty("enabled",true);config.update(on,config.revision());

        var hugePos=pos.south(8);
        for(int z=0;z<2;z++)for(int x=0;x<2;x++)ServerVerification.place(level,hugePos.offset(x,0,z),Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON,1));
        var huge=MobilityContent.hugeCauldron();
        ServerVerification.place(level,hugePos.above(),Blocks.BARREL.defaultBlockState());ServerVerification.place(level,hugePos.north(),Blocks.CHEST.defaultBlockState());
        var hugeInput=(net.minecraft.world.Container)level.getBlockEntity(hugePos.above());var hugeOutput=(net.minecraft.world.Container)level.getBlockEntity(hugePos.north());
        hugeInput.setItem(0,new ItemStack(AutomationContent.item("iron_dust")));
        require(huge!=null&&level.getBlockState(hugePos).getBlock()==huge&&huge.availableWater(level,hugePos)==4,
                "Four adjacent vanilla water cauldrons merge into a shared-water huge footprint");
        require(AutomationProcessing.tryNativeWash(level,hugePos)&&hugeInput.getItem(0).isEmpty()&&hugeOutput.getItem(0).is(AutomationContent.item("washed_iron_dust"))&&huge.availableWater(level,hugePos.east().south())==3,
                "Huge cauldron accepts an upper-container wash and consumes exactly one shared water unit");
        passed.accept("Large/huge auto-merged water cauldrons wash through upper containers, side outputs, shared volumes, full-output rollback and Config OFF");
    }

    private static void verifyJukeboxInventory(MinecraftServer server, ServerLevel level, Consumer<String> passed) {
        var player = EconomyVerification.actor(server, level, "SumireGrinder");
        try {
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.getNonEquipmentItems().size(); i++) inventory.setItem(i, new ItemStack(Items.STONE, 64));
            inventory.setItem(inventory.getSelectedSlot(), new ItemStack(Items.RAW_IRON,64));
            require(!AutomationProcessing.grindFromHand(player, new BlockPos(4, 240, 800), net.minecraft.world.InteractionHand.MAIN_HAND) && inventory.getItem(inventory.getSelectedSlot()).is(Items.RAW_IRON), "Jukebox grinding refuses a full inventory without consuming input");
            require(inventory.getItem(inventory.getSelectedSlot()).getCount()==64,"Failed full-inventory grinding preserves the entire raw ore stack");
            inventory.setItem(1, ItemStack.EMPTY);
            require(AutomationProcessing.grindFromHand(player, new BlockPos(4, 240, 800), net.minecraft.world.InteractionHand.MAIN_HAND), "Jukebox grinding succeeds when output fits");
            require(inventory.getItem(1).is(AutomationContent.item("iron_dust")) && inventory.getItem(1).getCount() == 2 && inventory.getItem(inventory.getSelectedSlot()).getCount()==63, "Jukebox transaction returns exactly two dust and consumes one raw ore");
            for (int i = 0; i < inventory.getNonEquipmentItems().size(); i++) inventory.setItem(i, new ItemStack(Items.STONE, 64));
            inventory.setItem(inventory.getSelectedSlot(), new ItemStack(Items.RAW_IRON));
            require(AutomationProcessing.grindFromHand(player, new BlockPos(5, 240, 800), net.minecraft.world.InteractionHand.MAIN_HAND), "Consuming the only input frees a main-inventory slot for the complete output");
            require(inventory.getItem(inventory.getSelectedSlot()).is(AutomationContent.item("iron_dust"))&&inventory.getItem(inventory.getSelectedSlot()).getCount()==2,"Output simulation includes the slot freed by consuming one held item");
            passed.accept("Vanilla jukebox full-main-inventory refusal, successful output and held-input freed-slot simulation");
        } finally { player.discard(); }
    }

    private static void verifyTesla(MinecraftServer server, ServerLevel level, Consumer<String> passed) {
        var rod = new BlockPos(15, 240, 800);
        ServerVerification.place(level, rod, BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("lightning_rod")).defaultBlockState().setValue(BlockStateProperties.POWERED, true));
        ServerVerification.place(level, rod.above(), BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("heavy_core")).defaultBlockState());
        var animal = BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("sheep")).create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        require(animal instanceof net.minecraft.world.entity.LivingEntity, "Sheep entity type available for tesla test");
        animal.setPos(rod.getX() + .5, rod.getY(), rod.getZ() + 1.5); level.addFreshEntity(animal); ServerVerification.spawned(animal);
        float health = ((net.minecraft.world.entity.LivingEntity) animal).getHealth();
        AutomationProcessing.trackTeslaRod(level, rod); AutomationProcessing.tickNativeTesla(server);
        require(((net.minecraft.world.entity.LivingEntity) animal).getHealth() < health, "Powered vanilla rod with heavy core above emits damage");
        passed.accept("Native lightning rod/heavy-core powered tesla setup damages a nearby animal");
    }

    private static void verifyMachinePersistence(MinecraftServer server, ServerLevel level, Consumer<String> passed) throws Exception {
        var pos = new BlockPos(8, 240, 820);
        var state = AutomationContent.block("auto_clicker").defaultBlockState();
        ServerVerification.place(level, pos, state);
        require(level.getBlockEntity(pos) instanceof AutomationMachineEntity, "Registered clicker creates its machine inventory");
        var machine = (AutomationMachineEntity) level.getBlockEntity(pos);
        machine.setItem(0, new ItemStack(Items.DIAMOND_PICKAXE));
        machine.toggleClickMode();
        machine.cooldown(8);
        machine.lastPowered(true);
        var saved = machine.saveWithFullMetadata(level.registryAccess());
        var restored = (AutomationMachineEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, state, saved, level.registryAccess());
        require(restored != null, "Automation machine native data reload");
        restored.setLevel(level);
        require(restored.getItem(0).is(Items.DIAMOND_PICKAXE) && !restored.leftClick() && restored.cooldown() == 8 && restored.lastPowered(), "Clicker item, mode and state survive save/load");
        var config = Sum1reM0d.config();
        var off = config.snapshot(); off.getAsJsonObject("features").getAsJsonObject("auto_clicker").addProperty("enabled", false); config.update(off, config.revision());
        AutomationProcessing.tick(server);
        require(machine.getItem(0).is(Items.DIAMOND_PICKAXE), "Config OFF retains clicker tool without processing loss");
        config.update(draftForAutomation(config.snapshot()), config.revision());
        passed.accept("Automation machine tool/mode serialization and disabled-feature item retention");
    }

    private static JsonObject draftForAutomation(JsonObject config) {
        config.getAsJsonObject("features").getAsJsonObject("auto_clicker").addProperty("enabled", true);
        return config;
    }

    private static void verifyIndependentClicker(MinecraftServer server,ServerLevel level,Consumer<String> passed)throws Exception{
        var config=Sum1reM0d.config();var original=config.snapshot();var owner=EconomyVerification.actor(server,level,"SumireClickerOwner");
        try{
            require(server.getPlayerList().getPlayers().isEmpty(),"Automation must work without a connected human player");
            owner.setPos(5000,240,5000);owner.getInventory().setItem(0,new ItemStack(Items.DIAMOND,7));var ownerStack=owner.getInventory().getItem(0).copy();
            var block=(AutomationMachineBlock)AutomationContent.block("auto_clicker");var state=block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH);
            var spawn=level.getRespawnData().pos();
            var pos=new BlockPos(spawn.getX()+512,240,spawn.getZ()+512);var target=pos.north();
            ServerVerification.place(level,pos,state);ServerVerification.place(level,pos.south(),Blocks.REDSTONE_BLOCK.defaultBlockState());ServerVerification.place(level,pos.west(),Blocks.REDSTONE_BLOCK.defaultBlockState());ServerVerification.place(level,target,Blocks.STONE.defaultBlockState());
            require(level.hasNeighborSignal(pos),"Clicker fixture places adjacent redstone blocks [best="+level.getBestNeighborSignal(pos)+", south="+level.getBlockState(pos.south())+", west="+level.getBlockState(pos.west())+"]");
            block.setPlacedBy(level,pos,state,owner,new ItemStack(block));var machine=(AutomationMachineEntity)level.getBlockEntity(pos);machine.setItem(0,new ItemStack(Items.DIAMOND_PICKAXE));
            require(machine.actorOwner().equals(owner.getUUID()),"Clicker persists the placer identity separately from its machine actor");
            var saved=machine.saveWithFullMetadata(level.registryAccess());var restored=(AutomationMachineEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,state,saved,level.registryAccess());require(restored!=null,"Clicker owner survives native block entity serialization");
            require(restored.actorOwner().equals(owner.getUUID()),"Clicker owner UUID roundtrips through native block entity data");
            var actor=AutomationMachineActor.create(level,machine);
            String permissionDiagnostic;
            try{
                boolean mayInteract=level.mayInteract(actor,target);
                boolean restricted=actor.blockActionRestricted(level,target,net.minecraft.world.level.GameType.SURVIVAL);
                permissionDiagnostic="spawn="+spawn+", fixture="+pos+", spawnDistance="+Vec3.atCenterOf(pos).distanceTo(Vec3.atCenterOf(spawn))+", mayInteract="+mayInteract+", blockActionRestricted="+restricted+", powered="+level.hasNeighborSignal(pos)+", bestSignal="+level.getBestNeighborSignal(pos)+", south="+level.getBlockState(pos.south())+", west="+level.getBlockState(pos.west())+", facing="+state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                require(!actor.getUUID().equals(owner.getUUID())&&actor.permissions()==net.minecraft.server.permissions.PermissionSet.NO_PERMISSIONS&&actor.entityTags().contains(AutomationMachineActor.TAG),"Machine actor has a namespaced identity, no permissions and no player tag");
            }finally{actor.discard();}
            var world=WorldState.get(server);int playersBefore=world.playerRecords().size();clickOnce(server,machine);
            require(level.getBlockState(target).isAir()&&machine.getItem(0).getDamageValue()==1,"Server-authoritative left click breaks one front block and stores exact tool damage without a nearby player ["+permissionDiagnostic+", action="+machine.lastClickResult()+", target="+level.getBlockState(target)+", damage="+machine.getItem(0).getDamageValue()+"]");
            require(ItemStack.matches(owner.getInventory().getItem(0),ownerStack)&&world.playerRecords().size()==playersBefore,"Machine actor does not modify the owner's items or create player history");

            ServerVerification.place(level,target,Blocks.STONE.defaultBlockState());
            var off=config.snapshot();off.getAsJsonObject("features").getAsJsonObject("auto_clicker").addProperty("enabled",false);config.update(off,config.revision());AutomationProcessing.tick(server);
            require(level.getBlockState(target).is(Blocks.STONE)&&machine.getItem(0).getDamageValue()==1,"Config OFF stops autonomous operation and keeps the tool untouched");
            config.update(draftForAutomation(config.snapshot()),config.revision());
            level.setBlock(pos.south(),Blocks.AIR.defaultBlockState(),3);level.setBlock(pos.west(),Blocks.AIR.defaultBlockState(),3);for(int i=0;i<Rules.i("auto_clicker","interval_ticks");i++)AutomationProcessing.tick(server);
            level.setBlock(pos.south(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);level.setBlock(pos.west(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);require(level.hasNeighborSignal(pos),"Redstone pulse fixture is powered before the next automated click");AutomationProcessing.tick(server);
            require(level.getBlockState(target).isAir()&&machine.getItem(0).getDamageValue()==2,"Powered clicker automatically repeats after the configured cooldown without a nearby player");

            machine.toggleClickMode();machine.setItem(0,new ItemStack(Items.DIAMOND_HOE));ServerVerification.place(level,target,Blocks.GRASS_BLOCK.defaultBlockState());clickOnce(server,machine);
            require(level.getBlockState(target).is(Blocks.FARMLAND)&&machine.getItem(0).getDamageValue()==1,"Right-click hoe use reaches the block and returns consumed durability to the machine slot");

            machine.setItem(0,new ItemStack(Items.DIAMOND_SHOVEL));ServerVerification.place(level,target,Blocks.GRASS_BLOCK.defaultBlockState());clickOnce(server,machine);
            require(BuiltInRegistries.BLOCK.getKey(level.getBlockState(target).getBlock()).getPath().equals("dirt_path")&&machine.getItem(0).getDamageValue()==1,"Right-click shovel use creates a dirt path and stores exact tool wear");

            machine.setItem(0,new ItemStack(Items.WHEAT_SEEDS));ServerVerification.place(level,target,Blocks.FARMLAND.defaultBlockState());clickOnce(server,machine);
            require(level.getBlockState(target.above()).is(Blocks.WHEAT)&&machine.getItem(0).isEmpty(),"Right-click planting places wheat above farmland and consumes the seed exactly once [action="+machine.lastClickResult()+", farmland="+level.getBlockState(target)+", crop="+level.getBlockState(target.above())+"]");

            machine.setItem(0,new ItemStack(Items.WATER_BUCKET));ServerVerification.place(level,target,Blocks.CAULDRON.defaultBlockState());clickOnce(server,machine);
            require(level.getBlockState(target).is(Blocks.WATER_CAULDRON)&&machine.getItem(0).is(Items.BUCKET),"Right-click bucket use retains its empty-bucket remainder in the one machine slot");

            machine.toggleClickMode();machine.setItem(0,new ItemStack(Items.DIAMOND_PICKAXE));ServerVerification.place(level,target,Blocks.BEDROCK.defaultBlockState());clickOnce(server,machine);
            require(level.getBlockState(target).is(Blocks.BEDROCK)&&machine.getItem(0).getDamageValue()==0&&machine.lastClickResult().equals("destroyBlock=denied_negative_hardness"),
                    "Left click refuses negative-hardness bedrock without damaging or consuming its tool");

            ServerVerification.place(level,target,Blocks.AIR.defaultBlockState());machine.setItem(0,new ItemStack(Items.DIAMOND_SWORD));
            var nearZombie=net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);var farZombie=net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            require(nearZombie!=null&&farZombie!=null,"Native zombie fixtures available for entity click targeting");nearZombie.setNoAi(true);nearZombie.setNoGravity(true);farZombie.setNoAi(true);farZombie.setNoGravity(true);
            var itemDecoy=new net.minecraft.world.entity.item.ItemEntity(level,target.getX()+.5,target.getY()+.2,target.getZ()+.5,new ItemStack(Items.ROTTEN_FLESH));itemDecoy.setNoGravity(true);itemDecoy.setDeltaMovement(Vec3.ZERO);require(level.addFreshEntity(itemDecoy),"Non-pickable item decoy is admitted to the click target cell");ServerVerification.spawned(itemDecoy);level.tickNonPassenger(itemDecoy);
            nearZombie.setPos(target.getX()+.5,target.getY(),target.getZ()+.78);farZombie.setPos(target.getX()+.5,target.getY(),target.getZ()+.22);level.addFreshEntity(nearZombie);level.addFreshEntity(farZombie);ServerVerification.spawned(nearZombie);ServerVerification.spawned(farZombie);
            level.tickNonPassenger(nearZombie);level.tickNonPassenger(farZombie);nearZombie.setDeltaMovement(Vec3.ZERO);farZombie.setDeltaMovement(Vec3.ZERO);
            var forwardCandidates=level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,new net.minecraft.world.phys.AABB(target),entity->entity.isAlive());
            require(forwardCandidates.contains(nearZombie)&&forwardCandidates.contains(farZombie)&&forwardCandidates.contains(itemDecoy)&&level.getBlockState(target).getCollisionShape(level,target).isEmpty(),
                    "Entity target query sees living fixtures and the non-pickable item decoy in the empty forward cell [near="+nearZombie.position()+", far="+farZombie.position()+", candidates="+forwardCandidates.size()+", decoyPickable="+itemDecoy.isPickable()+", target="+level.getBlockState(target)+"]");
            float nearHealth=nearZombie.getHealth(),farHealth=farZombie.getHealth();clickOnce(server,machine);
            require(nearZombie.getHealth()<nearHealth&&farZombie.getHealth()==farHealth&&machine.getItem(0).getDamageValue()==1,
                    "Left entity click attacks the nearest pickable living non-spectator target and ignores item drops [action="+machine.lastClickResult()+", nearHealth="+nearHealth+"->"+nearZombie.getHealth()+", farHealth="+farHealth+"->"+farZombie.getHealth()+", itemPickable="+itemDecoy.isPickable()+", toolDamage="+machine.getItem(0).getDamageValue()+"]");nearZombie.discard();farZombie.discard();itemDecoy.discard();

            machine.toggleClickMode();machine.setItem(0,new ItemStack(Items.SHEARS));ServerVerification.place(level,target,Blocks.AIR.defaultBlockState());
            var sheep=net.minecraft.world.entity.EntityTypes.SHEEP.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);require(sheep!=null,"Native sheep fixture available for right entity interaction");sheep.setNoAi(true);sheep.setNoGravity(true);sheep.setPos(target.getX()+.5,target.getY(),target.getZ()+.5);level.addFreshEntity(sheep);ServerVerification.spawned(sheep);level.tickNonPassenger(sheep);sheep.setDeltaMovement(Vec3.ZERO);
            var sheepCandidates=level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,new net.minecraft.world.phys.AABB(target),entity->entity.isAlive());
            require(Rules.b("auto_clicker","entity_interaction")&&sheepCandidates.contains(sheep)&&level.getBlockState(target).getCollisionShape(level,target).isEmpty(),"Sheep is indexed and targetable in the unblocked front cell [candidates="+sheepCandidates.size()+", option="+Rules.b("auto_clicker","entity_interaction")+"]");
            var whiteWool=BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("white_wool"));int woolDrops=(int)ServerVerification.spawnedIn(level).stream().filter(net.minecraft.world.entity.item.ItemEntity.class::isInstance).map(net.minecraft.world.entity.item.ItemEntity.class::cast).filter(item->item.getItem().is(whiteWool)).count();clickOnce(server,machine);
            int woolDropsAfter=(int)ServerVerification.spawnedIn(level).stream().filter(net.minecraft.world.entity.item.ItemEntity.class::isInstance).map(net.minecraft.world.entity.item.ItemEntity.class::cast).filter(item->item.getItem().is(whiteWool)).count();
            require(sheep.isSheared()&&machine.getItem(0).getDamageValue()==1&&woolDropsAfter>woolDrops,
                    "Right entity interaction shears one nearby sheep, consumes one durability, and preserves the wool drop [action="+machine.lastClickResult()+", sheared="+sheep.isSheared()+", damage="+machine.getItem(0).getDamageValue()+", woolDrops="+woolDrops+"->"+woolDropsAfter+", sheepPosition="+sheep.position()+", tool="+machine.getItem(0)+"]");sheep.discard();

            machine.toggleClickMode();machine.setItem(0,new ItemStack(Items.DIAMOND_PICKAXE));ServerVerification.place(level,target,Blocks.STONE.defaultBlockState());
            var blockedPig=net.minecraft.world.entity.EntityTypes.PIG.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);require(blockedPig!=null,"Native pig fixture available for wall-occlusion test");blockedPig.setNoAi(true);blockedPig.setNoGravity(true);blockedPig.setPos(target.getX()+.5,target.getY(),target.getZ()+.5);level.addFreshEntity(blockedPig);ServerVerification.spawned(blockedPig);level.tickNonPassenger(blockedPig);blockedPig.setDeltaMovement(Vec3.ZERO);float blockedHealth=blockedPig.getHealth();clickOnce(server,machine);
            require(level.getBlockState(target).isAir()&&blockedPig.getHealth()==blockedHealth,
                    "A solid forward block occludes an entity from clicker attack targeting");blockedPig.discard();

            var entityOption=config.snapshot();entityOption.getAsJsonObject("features").getAsJsonObject("auto_clicker").addProperty("entity_interaction",false);config.update(entityOption,config.revision());
            var optionZombie=net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);require(optionZombie!=null,"Native zombie fixture available for entity Config OFF test");optionZombie.setNoAi(true);optionZombie.setNoGravity(true);optionZombie.setPos(target.getX()+.5,target.getY(),target.getZ()+.5);level.addFreshEntity(optionZombie);ServerVerification.spawned(optionZombie);level.tickNonPassenger(optionZombie);optionZombie.setDeltaMovement(Vec3.ZERO);float optionHealth=optionZombie.getHealth();clickOnce(server,machine);
            require(optionZombie.getHealth()==optionHealth,"Entity interaction Config OFF prevents attacking while retaining normal block operation");optionZombie.discard();var entityOptionOn=config.snapshot();entityOptionOn.getAsJsonObject("features").getAsJsonObject("auto_clicker").addProperty("entity_interaction",true);config.update(entityOptionOn,config.revision());

            machine.setItem(0,new ItemStack(Items.DIAMOND_PICKAXE));ServerVerification.place(level,target,Blocks.BEDROCK.defaultBlockState());clickOnce(server,machine);
            require(level.getBlockState(target).is(Blocks.BEDROCK)&&machine.getItem(0).getDamageValue()==0&&machine.lastClickResult().equals("destroyBlock=denied_negative_hardness"),
                    "Entity-targeting remains unable to mine negative-hardness blocks");
            require(ItemStack.matches(owner.getInventory().getItem(0),ownerStack)&&world.playerRecords().size()==playersBefore,"Left/right automation has no owner-inventory, wallet or player-history side effects");
            passed.accept("Autonomous clicker emulates block and nearest-entity input, respects wall/negative-hardness protection, stores tool use and keeps owner data isolated");
        }finally{playerCleanup(owner);config.update(original,config.revision());}
    }

    private static void playerCleanup(net.minecraft.server.level.ServerPlayer player){player.discard();}
    private static void clickOnce(MinecraftServer server,AutomationMachineEntity machine){int wait=machine.cooldown();if(wait==0)AutomationProcessing.tick(server);else for(int i=0;i<wait;i++)AutomationProcessing.tick(server);}

    private static void require(boolean condition, String message) { ServerVerification.require(condition, message); }
}
