package com.sumirelabs.sum1rem0d.game;

import com.mojang.serialization.Codec;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

/** Shapelessly combines the matching axe, shovel and pickaxe of every registry material. */
public final class PaxelRecipe extends CustomRecipe {
    public static final RecipeSerializer<PaxelRecipe> SERIALIZER = new RecipeSerializer<>(
            Codec.STRING.fieldOf("material").xmap(PaxelRecipe::new, recipe -> recipe.material),
            StreamCodec.ofMember((recipe, buffer) -> buffer.writeUtf(recipe.material, 32), buffer -> new PaxelRecipe(buffer.readUtf(32))));
    private record Selection(String axe, String shovel, String pickaxe) {
        String key() { return axe + "|" + shovel + "|" + pickaxe; }
    }
    private static final class ToolCandidates {
        final List<String> axes = new ArrayList<>(), shovels = new ArrayList<>(), pickaxes = new ArrayList<>();
        ToolCandidates() {
            for (var item : BuiltInRegistries.ITEM) {
                String id = BuiltInRegistries.ITEM.getKey(item).toString();
                String path = BuiltInRegistries.ITEM.getKey(item).getPath();
                if (path.endsWith("_axe")) axes.add(id);
                if (path.endsWith("_shovel")) shovels.add(id);
                if (path.endsWith("_pickaxe")) pickaxes.add(id);
            }
            axes.sort(String::compareTo); shovels.sort(String::compareTo); pickaxes.sort(String::compareTo);
        }
        Map<String, Selection> bySeed(long seed) {
            if (axes.isEmpty() || shovels.isEmpty() || pickaxes.isEmpty()) return Map.of();
            var used = new HashSet<String>();
            var result = new LinkedHashMap<String, Selection>();
            for (String material : PaxelContent.MATERIALS) {
                var random = new Random(seed ^ (long) material.hashCode() * 0x9E3779B97F4A7C15L);
                Selection choice = null;
                for (int tries = 0; tries < 64; tries++) {
                    var candidate = new Selection(axes.get(random.nextInt(axes.size())), shovels.get(random.nextInt(shovels.size())), pickaxes.get(random.nextInt(pickaxes.size())));
                    if (used.add(candidate.key())) { choice = candidate; break; }
                }
                if (choice == null) choice = new Selection(axes.get(result.size() % axes.size()), shovels.get(result.size() % shovels.size()), pickaxes.get(result.size() % pickaxes.size()));
                used.add(choice.key());
                result.put(material, choice);
            }
            return result;
        }
    }
    private static volatile ToolCandidates candidates;
    private final String material;

    public PaxelRecipe(String material) {
        if (!PaxelContent.MATERIALS.contains(material)) throw new IllegalArgumentException("Unknown paxel material " + material);
        this.material = material;
    }

    static List<String> selectedIds(long worldSeed, String material) {
        var selection = candidates().bySeed(worldSeed).get(material);
        return selection == null ? List.of() : List.of(selection.axe(), selection.shovel(), selection.pickaxe());
    }

    static List<String> selectionTable(long worldSeed) {
        var result = new ArrayList<String>();
        for (String material : PaxelContent.MATERIALS) result.addAll(selectedIds(worldSeed, material));
        return List.copyOf(result);
    }

    private static ToolCandidates candidates() {
        ToolCandidates current = candidates;
        if (current != null) return current;
        synchronized (PaxelRecipe.class) {
            if (candidates == null) candidates = new ToolCandidates();
            return candidates;
        }
    }

    @Override public boolean matches(CraftingInput input, Level level) {
        if (!Rules.on("material_paxel") || input.items().stream().filter(stack -> !stack.isEmpty()).count() != 3) return false;
        long seed;
        if (level instanceof ServerLevel serverLevel) seed = serverLevel.getSeed();
        else {
            if (!PaxelRecipeMapState.received()) return false;
            return matchesIds(input, PaxelRecipeMapState.ids(material));
        }
        var choice = candidates().bySeed(seed).get(material);
        if (choice == null) return false;
        return matchesIds(input, List.of(choice.axe(), choice.shovel(), choice.pickaxe()));
    }

    private static boolean matchesIds(CraftingInput input, List<String> expected) {
        if (expected.size() != 3) return false;
        var found = new HashSet<String>();
        for (ItemStack stack : input.items()) if (!stack.isEmpty()) found.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        return found.size() == 3 && found.containsAll(expected);
    }

    @Override public ItemStack assemble(CraftingInput input) {
        return Rules.on("material_paxel") ? new ItemStack(PaxelContent.item(material)) : ItemStack.EMPTY;
    }

    @Override public RecipeSerializer<? extends CustomRecipe> getSerializer() { return SERIALIZER; }
}
