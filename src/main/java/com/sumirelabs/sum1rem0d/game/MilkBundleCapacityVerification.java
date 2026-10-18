package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;

import com.sumirelabs.sum1rem0d.Sum1reM0d;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.SimpleContainer;

/** Focused native checks for the configurable capacity of the milk-only bundle. */
final class MilkBundleCapacityVerification {
    private MilkBundleCapacityVerification() {}

    static void verify(MinecraftServer server, java.util.function.Consumer<String> passed) throws Exception {
        var store = Sum1reM0d.config();
        var original = store.snapshot();
        var draft = original.deepCopy();
        var features = draft.getAsJsonObject("features");
        for (var entry : features.entrySet()) entry.getValue().getAsJsonObject().addProperty("enabled", false);
        var milk = features.getAsJsonObject("bottled_milk");
        milk.addProperty("enabled", true);
        milk.addProperty("milk_bundle_enabled", true);
        milk.addProperty("milk_bundle_capacity", 64);
        var owner = EconomyVerification.actor(server, server.overworld(), "MilkCapacityOwner");
        try {
            store.update(draft, store.revision());
            var item = (MilkBundleItem) FoodContent.item("milk_bundle");
            var bottle = FoodContent.item("bottled_milk");
            var bundle = new ItemStack(item);
            var source = new SimpleContainer(1);
            source.setItem(0, new ItemStack(bottle, 16));
            var sourceSlot = new Slot(source, 0, 0, 0);
            for (int batch = 0; batch < 4; batch++) {
                require(item.overrideStackedOnOther(bundle, sourceSlot, ClickAction.PRIMARY, owner),
                        "Milk bundle accepts each bottle batch while below configured capacity");
                if (batch < 3) source.setItem(0, new ItemStack(bottle, 16));
            }
            require(source.getItem(0).isEmpty() && bottleCount(bundle) == 64,
                    "Capacity 64 stores four full bottle stacks rather than stopping at vanilla's 16-item weight");
            require(item.getBarWidth(bundle) == 13, "Milk bundle fullness bar reflects all 64 configured slots");

            source.setItem(0, new ItemStack(bottle, 1));
            require(!item.overrideStackedOnOther(bundle, sourceSlot, ClickAction.PRIMARY, owner)
                            && source.getItem(0).getCount() == 1 && bottleCount(bundle) == 64,
                    "A full milk bundle rejects more bottles without consuming or overflowing them");

            var empty = new SimpleContainer(1);
            require(item.overrideStackedOnOther(bundle, new Slot(empty, 0, 0, 0), ClickAction.SECONDARY, owner)
                            && empty.getItem(0).is(bottle) && empty.getItem(0).getCount() == 16
                            && empty.getItem(0).getMaxStackSize() == 16 && bottleCount(bundle) == 48,
                    "Empty-slot retrieval returns at most one legal 16-bottle stack and retains the remainder");

            final ItemStack[] cursor = {ItemStack.EMPTY};
            SlotAccess cursorAccess = new SlotAccess() {
                @Override public ItemStack get() { return cursor[0]; }
                @Override public boolean set(ItemStack stack) { cursor[0] = stack; return true; }
            };
            var bundleSlot = new Slot(new SimpleContainer(1), 0, 0, 0);
            require(item.overrideOtherStackedOnMe(bundle, ItemStack.EMPTY, bundleSlot, ClickAction.SECONDARY,
                            owner, cursorAccess)
                            && cursor[0].is(bottle) && cursor[0].getCount() == 16
                            && cursor[0].getMaxStackSize() == 16 && bottleCount(bundle) == 32,
                    "Cursor retrieval also returns at most 16 and keeps the remaining 32 inside the bundle");

            var ops = net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,
                    server.overworld().registryAccess());
            var encoded = ItemStack.CODEC.encodeStart(ops, bundle).getOrThrow();
            var decoded = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
            require(bottleCount(decoded) == 32 && decoded.get(DataComponents.BUNDLE_CONTENTS).items().stream()
                            .allMatch(template -> template.create().getMaxStackSize() >= 32),
                    "Saved ItemStack codec roundtrip retains all bottles and their internal capacity");

            milk.addProperty("milk_bundle_capacity", 16);
            store.update(draft, store.revision());
            source.setItem(0, new ItemStack(bottle, 1));
            require(!item.overrideStackedOnOther(decoded, sourceSlot, ClickAction.PRIMARY, owner)
                            && source.getItem(0).getCount() == 1 && bottleCount(decoded) == 32,
                    "Lowering capacity blocks new insertion without deleting already stored bottles");
            passed.accept("Milk bundle capacity 64, full rejection, legal 16-item slot/cursor retrieval, codec persistence, and safe capacity reduction");
        } finally {
            store.update(original, store.revision());
            owner.discard();
        }
    }

    private static int bottleCount(ItemStack bundle) {
        BundleContents contents = bundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        return contents.items().stream().mapToInt(template -> template.count()).sum();
    }
}
