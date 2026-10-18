package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.PersonalConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;

/** Client-local record of the most recently equipped vanilla or modded tool item. */
public final class LastToolBack {
    private static final ItemStackRenderState RENDER_STATE = new ItemStackRenderState();
    private static ItemStack last = ItemStack.EMPTY, observed = ItemStack.EMPTY;
    private static int playerId = Integer.MIN_VALUE;
    private LastToolBack() {}

    public static void tick(Minecraft mc) {
        if (mc.player == null || !enabled()) { clear(); return; }
        if (playerId != mc.player.getId()) { clear(); playerId = mc.player.getId(); }
        observeHeld(mc, mc.player.getMainHandItem());
        refreshRenderState(mc);
    }

    private static void observeHeld(Minecraft mc, ItemStack held) {
        if (isTool(held)) {
            if (!observed.isEmpty() && held.getItem() != observed.getItem()) setLast(mc, observed);
            observed = held.copy();
        } else {
            if (!observed.isEmpty()) setLast(mc, observed);
            observed = ItemStack.EMPTY;
        }
    }

    private static void setLast(Minecraft mc, ItemStack stack) {
        last = stack.copy();
        refreshRenderState(mc);
    }

    private static void refreshRenderState(Minecraft mc) {
        RENDER_STATE.clear();
        if (!last.isEmpty() && mc.player != null)
            mc.getItemModelResolver().updateForNonLiving(RENDER_STATE, last, ItemDisplayContext.FIXED, mc.player);
    }

    private static boolean enabled() {
        return PersonalConfig.store().enabled("last_tool_back");
    }
    private static boolean isTool(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES) || stack.is(ItemTags.SWORDS));
    }
    public static ItemStackRenderState renderState() { return last.isEmpty() ? null : RENDER_STATE; }
    static void observeHeldForVerification(Minecraft mc, ItemStack held) { observeHeld(mc, held); }
    static ItemStack lastForVerification() { return last.copy(); }
    public static void clear() { last = ItemStack.EMPTY; observed = ItemStack.EMPTY; playerId = Integer.MIN_VALUE; RENDER_STATE.clear(); }
}

