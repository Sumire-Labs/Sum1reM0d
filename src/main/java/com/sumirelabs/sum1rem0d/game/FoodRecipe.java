package com.sumirelabs.sum1rem0d.game;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;

/** Small config-gated recipes for tobacco and bottled milk items. */
public final class FoodRecipe extends CustomRecipe {
    public static final RecipeSerializer<FoodRecipe> SERIALIZER = new RecipeSerializer<>(
            Codec.STRING.fieldOf("kind").xmap(FoodRecipe::new, recipe -> recipe.kind),
            StreamCodec.ofMember((recipe, buffer) -> buffer.writeUtf(recipe.kind, 32), buffer -> new FoodRecipe(buffer.readUtf(32))));
    private final String kind;

    public FoodRecipe(String kind) {
        if (!List.of("cigarette", "bottled_milk", "milk_bundle").contains(kind)) throw new IllegalArgumentException("Unknown food recipe " + kind);
        this.kind = kind;
    }

    @Override public boolean matches(CraftingInput input, Level level) {
        long occupiedSlots = input.items().stream().filter(stack -> !stack.isEmpty()).count();
        int expectedSlots = switch (kind) { case "cigarette" -> 2; case "bottled_milk" -> 4; default -> 2; };
        if (occupiedSlots != expectedSlots) return false;
        if (kind.equals("cigarette")) return Rules.on("cigarette") && hasExactly(input, Items.PAPER, 1, Items.COAL, 1);
        if (kind.equals("bottled_milk")) return Rules.on("bottled_milk") && hasExactly(input, Items.MILK_BUCKET, 1, Items.GLASS_BOTTLE, 3);
        if (!Rules.on("bottled_milk") || !Rules.b("bottled_milk", "milk_bundle_enabled")) return false;
        ItemStack regularBundle = null;
        int bottleCount = 0;
        for (ItemStack stack : input.items()) if (!stack.isEmpty()) {
            if (stack.is(Items.BUNDLE)) regularBundle = stack;
            else if (stack.is(FoodContent.item("bottled_milk"))) bottleCount += stack.getCount();
            else return false;
        }
        if (regularBundle == null || bottleCount != 1) return false;
        BundleContents content = regularBundle.get(DataComponents.BUNDLE_CONTENTS);
        return content == null || content.isEmpty();
    }

    @Override public ItemStack assemble(CraftingInput input) {
        if (kind.equals("cigarette")) return Rules.on("cigarette") ? new ItemStack(FoodContent.item("cigarette"), 4) : ItemStack.EMPTY;
        if (kind.equals("bottled_milk")) return Rules.on("bottled_milk") ? new ItemStack(FoodContent.item("bottled_milk"), 3) : ItemStack.EMPTY;
        if (!Rules.on("bottled_milk") || !Rules.b("bottled_milk", "milk_bundle_enabled")) return ItemStack.EMPTY;
        var bundle = new ItemStack(FoodContent.item("milk_bundle"));
        BundleContents.Mutable contents;
        //? if >=26.3 {
        contents = new BundleContents.Mutable();
        //?} else {
        contents = new BundleContents.Mutable(BundleContents.EMPTY);
        //?}
        input.items().stream().filter(stack -> !stack.isEmpty() && stack.is(FoodContent.item("bottled_milk")))
                .findFirst().ifPresent(stack -> contents.tryInsert(MilkBundleItem.prepareMilkStack(stack.copyWithCount(1), MilkBundleItem.capacity())));
        bundle.set(DataComponents.BUNDLE_CONTENTS, contents.toImmutable());
        return bundle;
    }

    private static boolean hasExactly(CraftingInput input, net.minecraft.world.item.Item first, int firstCount,
            net.minecraft.world.item.Item second, int secondCount) {
        int a = 0, b = 0;
        for (ItemStack stack : input.items()) if (!stack.isEmpty()) {
            if (stack.is(first)) a += stack.getCount(); else if (stack.is(second)) b += stack.getCount(); else return false;
        }
        return a == firstCount && b == secondCount;
    }

    @Override public RecipeSerializer<? extends CustomRecipe> getSerializer() { return SERIALIZER; }
}
