package com.sumirelabs.sum1rem0d.game;

import com.mojang.serialization.Codec;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Recipe inputs and outputs come from the same live, synchronized config as the UI. */
public final class ConfiguredCraftingRecipe extends CustomRecipe {
    public static final RecipeSerializer<ConfiguredCraftingRecipe> SERIALIZER = new RecipeSerializer<>(
        Codec.STRING.fieldOf("feature").xmap(ConfiguredCraftingRecipe::new, recipe -> recipe.feature),
        StreamCodec.ofMember((recipe, buffer) -> buffer.writeUtf(recipe.feature, 64), buffer -> new ConfiguredCraftingRecipe(buffer.readUtf(64))));
    private final String feature;
    public ConfiguredCraftingRecipe(String feature) {
        if (!List.of("grass_coal","cleanliness","mud_ball","sumzon_delivery_box","automated_crafting","jukebox_grinder","washing_cauldron","tesla_coil","auto_clicker","sulfur_gunpowder").contains(feature)) throw new IllegalArgumentException("Unknown configured recipe");
        this.feature = feature;
    }
    /** Reject impossible outputs instead of silently changing the configured quantity. */
    public static void validateConfig(com.google.gson.JsonObject candidate) {
        var features = candidate.getAsJsonObject("features");
        var grass = features.getAsJsonObject("grass_coal");
        if (grass.get("enabled").getAsBoolean()) {
            requireItem(grass.get("input_item").getAsString());
            var output = requireItem(grass.get("output_item").getAsString());
            // 26.3 binds item components when world data packs load. The title
            // screen can validate registered IDs, but not the final stack cap.
            // ServerLifecycleMixin validates again after the world is loaded.
            if (output.builtInRegistryHolder().areComponentsBound()) {
                int maximum = output.getDefaultInstance().getMaxStackSize();
                if (grass.get("output_count").getAsInt() > maximum)
                    throw new IllegalArgumentException("草から石炭: 出力数はこのアイテムのスタック上限 " + maximum + " 以下にしてください。");
            }
        }
        var mud = features.getAsJsonObject("mud_ball");
        if (mud.get("enabled").getAsBoolean() && mud.get("recipe_enabled").getAsBoolean()) {
            for (String material : com.sumirelabs.sum1rem0d.config.ConfigSchema.split(mud.get("mud_materials").getAsString())) requireItem(material);
            var item = ModContent.mudBall();
            if (item.builtInRegistryHolder().areComponentsBound() && mud.get("recipe_output").getAsInt() > item.getDefaultInstance().getMaxStackSize()) throw new IllegalArgumentException("泥だんごの出力数がスタック上限を超えています");
        }
        var soap = features.getAsJsonObject("cleanliness");
        if (soap.get("enabled").getAsBoolean())
            for (String material : com.sumirelabs.sum1rem0d.config.ConfigSchema.split(soap.get("soap_materials").getAsString())) requireItem(material);
    }
    private static Item requireItem(String name) {
        var id = Rules.id(name);
        var item = id == null ? Items.AIR : BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
        if (item == Items.AIR) throw new IllegalArgumentException("クラフト材料／出力のアイテムが存在しません: " + name);
        return item;
    }
    @Override public boolean matches(CraftingInput input, Level level) {
        if (!Rules.on(feature) || assemble(input).isEmpty()) return false;
        if (feature.equals("sumzon_delivery_box")) {
            if(!Rules.b(feature,"recipe_enabled")||input.width()!=3||input.height()!=3)return false;
            for(int i=0;i<9;i++)if(!input.getItem(i).is(i==4?Items.CHEST:Items.IRON_INGOT))return false;
            return true;
        }
        if(List.of("automated_crafting","jukebox_grinder","washing_cauldron","tesla_coil","auto_clicker").contains(feature))return machineRecipeMatches(input,feature);
        if(feature.equals("sulfur_gunpowder")){
            var counts=new java.util.HashMap<String,Integer>();for(var stack:input.items())if(!stack.isEmpty())counts.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),1,Integer::sum);
            String sulfur=BuiltInRegistries.ITEM.getKey(AutomationContent.sulfur()).toString();return counts.size()==3&&counts.getOrDefault(sulfur,0)==1&&counts.getOrDefault("minecraft:charcoal",0)==1&&counts.getOrDefault("sum1rem0d:dust",0)==1;
        }
        List<String> required;
        if (feature.equals("mud_ball")) required = new ArrayList<>(Rules.list(feature, "mud_materials"));
        else if (feature.equals("cleanliness")) required = new ArrayList<>(Rules.list(feature, "soap_materials"));
        else required = new ArrayList<>(Collections.nCopies(Rules.i(feature, "input_count"), Rules.s(feature, "input_item")));
        if (required.isEmpty() || input.ingredientCount() != required.size()) return false;
        for (ItemStack stack : input.items()) if (!stack.isEmpty()) {
            String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (!required.remove(item)) return false;
        }
        return required.isEmpty();
    }
    @Override public ItemStack assemble(CraftingInput input) {
        if (!Rules.on(feature)) return ItemStack.EMPTY;
        if(feature.equals("sumzon_delivery_box"))return Rules.b(feature,"recipe_enabled")?new ItemStack(DeliveryBoxContent.item()):ItemStack.EMPTY;
        if(feature.equals("sulfur_gunpowder"))return Rules.b(feature,"enabled")?new ItemStack(Items.GUNPOWDER,4):ItemStack.EMPTY;
        String automationBlock=switch(feature){case "automated_crafting"->"automated_crafting_table";case "jukebox_grinder"->"jukebox_grinder";case "washing_cauldron"->"washing_cauldron";case "tesla_coil"->"tesla_coil";case "auto_clicker"->"auto_clicker";default->null;};
        if(automationBlock!=null)return new ItemStack(AutomationContent.block(automationBlock));
        if (feature.equals("mud_ball")) {
            if (!Rules.on("cleanliness") || !Rules.b(feature,"recipe_enabled")) return ItemStack.EMPTY;
            int count = Rules.i(feature,"recipe_output");
            return count > ModContent.mudBall().getDefaultInstance().getMaxStackSize() ? ItemStack.EMPTY : new ItemStack(ModContent.mudBall(),count);
        }
        if (feature.equals("cleanliness")) return new ItemStack(ModContent.soap(), Rules.i(feature, "soap_output"));
        var id = Rules.id(Rules.s(feature, "output_item"));
        if (id == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
        int count = Rules.i(feature, "output_count");
        return item == Items.AIR || count > item.getDefaultInstance().getMaxStackSize() ? ItemStack.EMPTY : new ItemStack(item, count);
    }
    private static boolean machineRecipeMatches(CraftingInput input,String feature){
        String[] pattern=switch(feature){case "automated_crafting"->new String[]{"minecraft:iron_ingot","minecraft:redstone","minecraft:iron_ingot","minecraft:redstone","minecraft:crafting_table","minecraft:redstone","minecraft:iron_ingot","minecraft:redstone","minecraft:iron_ingot"};case "jukebox_grinder"->new String[]{"","minecraft:flint","","","minecraft:jukebox","","","",""};case "washing_cauldron"->new String[]{"minecraft:iron_ingot","minecraft:iron_ingot","minecraft:iron_ingot","minecraft:iron_ingot","minecraft:cauldron","minecraft:iron_ingot","minecraft:iron_ingot","minecraft:iron_ingot","minecraft:iron_ingot"};case "tesla_coil"->new String[]{"","minecraft:redstone","","","minecraft:lightning_rod","","","minecraft:heavy_core",""};case "auto_clicker"->new String[]{"","minecraft:netherite_ingot","","","minecraft:dispenser","","","",""};default->new String[0];};
        if(input.width()!=3||input.height()!=3)return false;for(int i=0;i<9;i++){var stack=input.getItem(i);String expected=pattern[i];if(expected.isEmpty()){if(!stack.isEmpty())return false;}else if(stack.isEmpty()||!BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(expected))return false;}return true;
    }
    @Override public RecipeSerializer<? extends CustomRecipe> getSerializer() { return SERIALIZER; }
}
