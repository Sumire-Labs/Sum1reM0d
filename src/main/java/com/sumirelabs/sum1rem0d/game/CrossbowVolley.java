package com.sumirelabs.sum1rem0d.game;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.tags.ItemTags;

/** Persisted projectile queue for C09. It stores projectiles, never live entities or refunded ammo. */
public final class CrossbowVolley {
    private static final String KEY = "sum1rem0d_crossbow_queue";
    private static final ThreadLocal<ItemStack> FORCE_VANILLA_SHOT = new ThreadLocal<>();
    private CrossbowVolley() {}

    /** Sneak-use releases a partial saved volley through vanilla's projectile path. */
    public static boolean releaseOnShiftUse(Level level, LivingEntity user, ItemStack crossbow) {
        if (!Rules.on("crossbow_circle") || !Rules.b("crossbow_circle", "manual_release_enabled")
                || !user.isShiftKeyDown() || read(level, crossbow).isEmpty()) return false;
        if (level.isClientSide()) return true;
        List<ItemStack> queued = read(level, crossbow);
        ChargedProjectiles charged = crossbow.get(DataComponents.CHARGED_PROJECTILES);
        if (charged != null) queued.addAll(projectileCopies(charged));
        if (queued.isEmpty()) return false;
        crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(queued));
        write(level, crossbow, List.of());
        FORCE_VANILLA_SHOT.set(crossbow);
        return true;
    }

    public static boolean intercept(Level level, LivingEntity shooter, ItemStack crossbow) {
        if (level.isClientSide()) return false;
        ItemStack forceVanilla = FORCE_VANILLA_SHOT.get();
        if (forceVanilla != null) {
            FORCE_VANILLA_SHOT.remove();
            if (forceVanilla == crossbow) return false;
        }
        List<ItemStack> queued = read(level, crossbow);
        ChargedProjectiles charged = crossbow.get(DataComponents.CHARGED_PROJECTILES);
        List<ItemStack> current = charged == null ? new ArrayList<>() : projectileCopies(charged);
        boolean enabled = Rules.on("crossbow_circle");

        // Config OFF is a safe recovery path: materialize saved projectiles back into the
        // vanilla charged component. They remain paid-for projectiles; nothing is refunded.
        if (!enabled) {
            if (queued.isEmpty()) return false;
            queued.addAll(current);
            crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(queued));
            write(level, crossbow, List.of());
            return false;
        }

        if (current.isEmpty()) return false;
        // Fireworks and other supported projectiles stay on the vanilla path. A mixed
        // or unusual payload must not be converted into an arrow queue.
        if (current.stream().anyMatch(stack -> !stack.is(ItemTags.ARROWS))) return false;
        queued.addAll(current);
        int cap = Math.max(1, Rules.i("crossbow_circle", "max_stored_arrows"));
        if (queued.size() < cap) {
            crossbow.remove(DataComponents.CHARGED_PROJECTILES);
            write(level, crossbow, queued);
            return true;
        }

        int fireCount = Math.min(cap, queued.size());
        List<ItemStack> volley = new ArrayList<>(queued.subList(0, fireCount));
        List<ItemStack> remainder = new ArrayList<>(queued.subList(fireCount, queued.size()));
        crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(volley));
        write(level, crossbow, remainder);
        return false;
    }

    /** Flushes the saved queue before a Config-OFF use can start loading or firing. */
    public static void prepareUse(Level level, ItemStack crossbow) {
        if (level.isClientSide() || Rules.on("crossbow_circle")) return;
        List<ItemStack> queued = read(level, crossbow);
        if (queued.isEmpty()) return;
        ChargedProjectiles charged = crossbow.get(DataComponents.CHARGED_PROJECTILES);
        if (charged != null) queued.addAll(projectileCopies(charged));
        crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(queued));
        write(level, crossbow, List.of());
    }

    public static int queuedCount(Level level, ItemStack stack) { return read(level, stack).size(); }

    private static List<ItemStack> projectileCopies(ChargedProjectiles charged) {
        //? if >=26.3 {
        return charged.itemCopies().toList();
        //?} else {
        return charged.itemCopies();
        //?}
    }

    private static List<ItemStack> read(Level level, ItemStack crossbow) {
        CustomData data = crossbow.get(DataComponents.CUSTOM_DATA);
        if (data == null) return new ArrayList<>();
        ListTag list = data.copyTag().getListOrEmpty(KEY);
        var ops = RegistryOps.create(NbtOps.INSTANCE, level.registryAccess());
        List<ItemStack> result = new ArrayList<>();
        for (var tag : list) ItemStack.CODEC.parse(ops, tag).result().filter(stack -> !stack.isEmpty()).ifPresent(result::add);
        return result;
    }

    private static void write(Level level, ItemStack crossbow, List<ItemStack> stacks) {
        CustomData old = crossbow.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = old == null ? new CompoundTag() : old.copyTag();
        if (stacks.isEmpty()) tag.remove(KEY);
        else {
            var ops = RegistryOps.create(NbtOps.INSTANCE, level.registryAccess());
            var encoded = ItemStack.CODEC.listOf().encodeStart(ops, stacks).result().orElse(null);
            if (encoded instanceof ListTag list) tag.put(KEY, list);
        }
        if (tag.isEmpty()) crossbow.remove(DataComponents.CUSTOM_DATA);
        else crossbow.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
