package com.sumirelabs.sum1rem0d.game;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.BundleContents.Mutable;
import net.minecraft.world.level.Level;

/** Milk-only bundle with a configurable capacity. Stored bottle stacks carry a private max-stack component
 * so vanilla BundleContents weighting reflects this bundle's capacity; extracted bottles are normalized. */
public final class MilkBundleItem extends BundleItem {
    public MilkBundleItem(Properties properties) { super(properties); }
    private record PendingRemoval(Mutable contents, ItemStack offered, int internalMaximum) {}

    static int capacity() {
        return Math.clamp(Rules.i("bottled_milk", "milk_bundle_capacity"), 16, 99);
    }

    static ItemStack prepareMilkStack(ItemStack stack, int maximum) {
        var copy = stack.copy();
        if (copy.is(FoodContent.item("bottled_milk"))) copy.set(DataComponents.MAX_STACK_SIZE, Math.clamp(maximum, 16, 99));
        return copy;
    }

    @Override public boolean overrideStackedOnOther(ItemStack bundle, Slot slot, ClickAction action, Player player) {
        normalizeContents(bundle);
        ItemStack target = slot.getItem();
        if (target.isEmpty() && action == ClickAction.SECONDARY)
            return removeIntoSlot(bundle, slot, player);
        if (target.isEmpty()) {
            boolean changed = super.overrideStackedOnOther(bundle, slot, action, player);
            if (changed && normalizeExtracted(slot.getItem())) slot.setChanged();
            return changed;
        }
        if (!target.is(FoodContent.item("bottled_milk")) || !bundleEnabled() || !canAccept(bundle)) return false;
        target.set(DataComponents.MAX_STACK_SIZE, capacity());
        boolean changed;
        try {
            changed = super.overrideStackedOnOther(bundle, slot, action, player);
        } finally {
            if (normalizeExtracted(slot.getItem())) slot.setChanged();
        }
        if (changed) normalizeContents(bundle);
        return changed;
    }

    @Override public boolean overrideOtherStackedOnMe(ItemStack bundle, ItemStack other, Slot slot,
            ClickAction action, Player player, net.minecraft.world.entity.SlotAccess access) {
        normalizeContents(bundle);
        if (other.isEmpty() && action == ClickAction.SECONDARY && slot.allowModification(player))
            return removeIntoCursor(bundle, access, player);
        if (other.isEmpty()) {
            boolean changed = super.overrideOtherStackedOnMe(bundle, other, slot, action, player, access);
            if (changed) normalizeAccess(access);
            return changed;
        }
        if (!other.is(FoodContent.item("bottled_milk")) || !bundleEnabled() || !canAccept(bundle)) return false;
        other.set(DataComponents.MAX_STACK_SIZE, capacity());
        boolean changed;
        try {
            changed = super.overrideOtherStackedOnMe(bundle, other, slot, action, player, access);
        } finally {
            normalizeAccess(access);
        }
        if (changed) normalizeContents(bundle);
        return changed;
    }

    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack bundle, int remainingUseDuration) {
        if (!(entity instanceof Player player)) return;
        int duration = getUseDuration(bundle, entity);
        if (remainingUseDuration == duration || remainingUseDuration < duration - 10 && remainingUseDuration % 2 == 0)
            dropNextMilkBatch(level, player, bundle);
    }

    @Override public Optional<TooltipComponent> getTooltipImage(ItemStack bundle) {
        ItemStack view = bundle.copy();
        normalizeContents(view);
        return super.getTooltipImage(view);
    }

    @Override public boolean isBarVisible(ItemStack bundle) {
        ItemStack view = bundle.copy();
        normalizeContents(view);
        return super.isBarVisible(view);
    }

    @Override public int getBarWidth(ItemStack bundle) {
        ItemStack view = bundle.copy();
        normalizeContents(view);
        return super.getBarWidth(view);
    }

    @Override public int getBarColor(ItemStack bundle) {
        ItemStack view = bundle.copy();
        normalizeContents(view);
        return super.getBarColor(view);
    }

    private static boolean bundleEnabled() {
        return Rules.on("bottled_milk") && Rules.b("bottled_milk", "milk_bundle_enabled");
    }

    private static boolean canAccept(ItemStack bundle) {
        BundleContents contents = bundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        return containsOnlyBottledMilk(contents) && bottleCount(contents) < capacity();
    }

    private static int bottleCount(BundleContents contents) {
        return contents.items().stream().mapToInt(stack -> stack.count()).sum();
    }

    private static boolean containsOnlyBottledMilk(BundleContents contents) {
        for (var stack : contents.items()) if (stack.item().value() != FoodContent.item("bottled_milk")) return false;
        return true;
    }

    private static PendingRemoval removeOneBatch(ItemStack bundle) {
        BundleContents contents = bundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        if (contents.isEmpty() || !containsOnlyBottledMilk(contents)) return null;
        Mutable mutable = mutable(contents);
        ItemStack stored = mutable.removeOne();
        if (stored == null || stored.isEmpty()) return null;
        int internalMaximum = stored.getMaxStackSize();
        int amount = Math.min(FoodContent.item("bottled_milk").getDefaultInstance().getMaxStackSize(), stored.getCount());
        ItemStack offered = stored.copyWithCount(amount);
        normalizeExtracted(offered);
        int rest = stored.getCount() - amount;
        if (rest > 0) {
            ItemStack retained = stored.copyWithCount(rest);
            if (mutable.tryInsert(retained) != rest) return null;
        }
        return new PendingRemoval(mutable, offered, internalMaximum);
    }

    private static boolean removeIntoSlot(ItemStack bundle, Slot slot, Player player) {
        PendingRemoval pending = removeOneBatch(bundle);
        if (pending == null) return false;
        int offered = pending.offered().getCount();
        ItemStack rejected = slot.safeInsert(pending.offered());
        int accepted = offered - rejected.getCount();
        if (accepted == 0) return false;
        if (!rejected.isEmpty()) {
            rejected.set(DataComponents.MAX_STACK_SIZE, pending.internalMaximum());
            int rejectedCount=rejected.getCount();
            if (pending.contents().tryInsert(rejected) != rejectedCount) return false;
        }
        bundle.set(DataComponents.BUNDLE_CONTENTS, pending.contents().toImmutable());
        player.level().playSound(null, player.blockPosition(), SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 0.8F, 1.0F);
        player.containerMenu.broadcastChanges();
        return true;
    }

    private static boolean removeIntoCursor(ItemStack bundle, net.minecraft.world.entity.SlotAccess access, Player player) {
        PendingRemoval pending = removeOneBatch(bundle);
        if (pending == null) return false;
        if (!access.set(pending.offered())) {
            ItemStack retained = pending.offered();
            retained.set(DataComponents.MAX_STACK_SIZE, pending.internalMaximum());
            int retainedCount=retained.getCount();
            if (pending.contents().tryInsert(retained) != retainedCount) return false;
            return false;
        }
        bundle.set(DataComponents.BUNDLE_CONTENTS, pending.contents().toImmutable());
        player.level().playSound(null, player.blockPosition(), SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 0.8F, 1.0F);
        player.containerMenu.broadcastChanges();
        return true;
    }

    private static boolean dropNextMilkBatch(Level level, Player player, ItemStack bundle) {
        normalizeContents(bundle);
        PendingRemoval pending = removeOneBatch(bundle);
        if (pending == null) return false;
        bundle.set(DataComponents.BUNDLE_CONTENTS, pending.contents().toImmutable());
        if (player.drop(pending.offered(), true, Prediction.SERVER_ONLY) == null) {
            var restore = mutable(bundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY));
            ItemStack retained = pending.offered();
            retained.set(DataComponents.MAX_STACK_SIZE, pending.internalMaximum());
            restore.tryInsert(retained);
            bundle.set(DataComponents.BUNDLE_CONTENTS, restore.toImmutable());
            return false;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.8F, 0.8F);
        player.awardStat(Stats.ITEM_USED.get(FoodContent.item("milk_bundle")));
        player.containerMenu.broadcastChanges();
        return true;
    }

    private static Mutable mutable(BundleContents contents) {
        //? if >=26.3 {
        return contents.asMutable();
        //?} else {
        /*return new Mutable(contents);
        *///?}
    }

    private static void normalizeContents(ItemStack bundle) {
        BundleContents contents = bundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        if (contents.isEmpty() || !containsOnlyBottledMilk(contents)) return;
        int maximum = Math.clamp(Math.max(capacity(), bottleCount(contents)), 16, 99);
        Mutable normalized = emptyMutable();
        for (var template : contents.items()) {
            ItemStack stack = template.create();
            stack.set(DataComponents.MAX_STACK_SIZE, maximum);
            int count = stack.getCount();
            if (normalized.tryInsert(stack) != count) return;
        }
        BundleContents adjusted = normalized.toImmutable();
        if (!adjusted.equals(contents)) bundle.set(DataComponents.BUNDLE_CONTENTS, adjusted);
    }

    private static Mutable emptyMutable() {
        //? if >=26.3 {
        return new Mutable();
        //?} else {
        /*return new Mutable(BundleContents.EMPTY);
        *///?}
    }

    private static void normalizeAccess(net.minecraft.world.entity.SlotAccess access) {
        ItemStack stack = access.get();
        if (normalizeExtracted(stack)) access.set(stack);
    }

    private static boolean normalizeExtracted(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(FoodContent.item("bottled_milk"))
                || stack.getMaxStackSize() == FoodContent.item("bottled_milk").getDefaultInstance().getMaxStackSize()) return false;
        stack.set(DataComponents.MAX_STACK_SIZE, FoodContent.item("bottled_milk").getDefaultInstance().getMaxStackSize());
        return true;
    }
}
