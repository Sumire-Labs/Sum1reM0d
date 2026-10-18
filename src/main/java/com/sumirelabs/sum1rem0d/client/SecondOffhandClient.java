package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.ClientRules;
import com.sumirelabs.sum1rem0d.network.SecondOffhandStatePayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Client cache is display-only; the server owns the actual slot contents. */
public final class SecondOffhandClient {
    private static final Map<UUID, ItemStack> ITEMS = new HashMap<>();
    private static final Map<UUID, ItemStackRenderState> RENDER = new HashMap<>();
    private static long revision;
    private SecondOffhandClient() {}

    public static void accept(SecondOffhandStatePayload payload) {
        if (payload.stack().isEmpty()) ITEMS.remove(payload.player());
        else ITEMS.put(payload.player(), payload.stack().copy());
        RENDER.remove(payload.player());
        revision++;
    }

    public static ItemStack stack(UUID player) { return ITEMS.getOrDefault(player, ItemStack.EMPTY); }
    public static int slotX() { return com.sumirelabs.sum1rem0d.config.PersonalConfig.store().integer("second_offhand", "slot_x"); }
    public static int slotY() { return com.sumirelabs.sum1rem0d.config.PersonalConfig.store().integer("second_offhand", "slot_y"); }
    public static long revision() { return revision; }

    public static void clear() { ITEMS.clear(); RENDER.clear(); com.sumirelabs.sum1rem0d.game.PaxelRecipeMapState.clear(); revision++; }

    public static void requestSwap() {
        if (!ClientRules.enabled("second_offhand")) return;
        PlatformClientNetwork.secondOffhand();
    }

    public static ItemStackRenderState renderState(Player player) {
        ItemStack stack = stack(player.getUUID());
        if (stack.isEmpty()) return null;
        var state = RENDER.computeIfAbsent(player.getUUID(), ignored -> new ItemStackRenderState());
        state.clear();
        Minecraft.getInstance().getItemModelResolver().updateForLiving(state, stack, ItemDisplayContext.HEAD, player);
        return state;
    }
}
