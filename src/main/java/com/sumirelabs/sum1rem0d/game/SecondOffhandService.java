package com.sumirelabs.sum1rem0d.game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.util.Prediction;
import com.sumirelabs.sum1rem0d.network.PlatformNetwork;
import com.sumirelabs.sum1rem0d.network.SecondOffhandStatePayload;

/** Owns second-offhand transfer, lifecycle protection, Config OFF recovery, and authoritative sync. */
public final class SecondOffhandService {
    private record Snapshot(String dimension, ItemStack stack) {}
    private static final Map<UUID, Snapshot> LAST_SENT = new HashMap<>();
    private SecondOffhandService() {}

    private static SecondOffhandAccess access(ServerPlayer player) { return (SecondOffhandAccess) player; }

    public static void requestSwap(ServerPlayer player) {
        if (!Rules.on("second_offhand") || !(player.containerMenu instanceof InventoryMenu)) return;
        ItemStack carried = player.containerMenu.getCarried();
        ItemStack slot = access(player).sum1rem0d$secondOffhand();
        access(player).sum1rem0d$secondOffhand(carried);
        player.containerMenu.setCarried(slot);
        player.containerMenu.broadcastChanges();
        syncOwner(player);
    }

    public static void restore(ServerPlayer respawned, ServerPlayer oldPlayer) {
        ItemStack stack = access(oldPlayer).sum1rem0d$secondOffhand();
        if (stack.isEmpty()) return;
        access(oldPlayer).sum1rem0d$secondOffhand(ItemStack.EMPTY);
        if (Rules.on("second_offhand") && Rules.b("second_offhand", "death_keep")) {
            access(respawned).sum1rem0d$secondOffhand(stack);
            syncOwner(respawned);
        } else {
            oldPlayer.drop(stack.copy(), true, Prediction.SERVER_ONLY);
            syncOwner(oldPlayer);
        }
    }

    public static void syncAll(ServerPlayer joining) {
        var server = joining.level().getServer();
        if (server == null) return;
        for (ServerPlayer owner : server.getPlayerList().getPlayers()) if (owner.level() == joining.level()) send(joining, owner);
    }

    public static void disconnect(ServerPlayer leaving) {
        LAST_SENT.remove(leaving.getUUID());
        broadcast(leaving, ItemStack.EMPTY);
    }

    public static void syncOwner(ServerPlayer owner) {
        LAST_SENT.put(owner.getUUID(), snapshot(owner));
        broadcast(owner, access(owner).sum1rem0d$secondOffhand());
    }

    public static void tick(MinecraftServer server) {
        var active = new java.util.HashSet<UUID>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            active.add(player.getUUID());
            if (!Rules.on("second_offhand")) returnItemOnDisable(player);
            var now = snapshot(player);
            var previous = LAST_SENT.get(player.getUUID());
            if (!same(previous, now)) {
                LAST_SENT.put(player.getUUID(), now);
                broadcast(player, now.stack());
            }
        }
        for (UUID id : new java.util.HashSet<>(LAST_SENT.keySet())) if (!active.contains(id)) LAST_SENT.remove(id);
    }

    private static Snapshot snapshot(ServerPlayer player) {
        return new Snapshot(player.level().dimension().identifier().toString(), access(player).sum1rem0d$secondOffhand().copy());
    }

    private static boolean same(Snapshot a, Snapshot b) {
        return a != null && a.dimension().equals(b.dimension()) && ItemStack.isSameItemSameComponents(a.stack(), b.stack())
                && a.stack().getCount() == b.stack().getCount();
    }

    private static void returnItemOnDisable(ServerPlayer player) {
        var slot = access(player).sum1rem0d$secondOffhand();
        if (slot.isEmpty()) return;
        ItemStack remainder = slot.copy();
        access(player).sum1rem0d$secondOffhand(ItemStack.EMPTY);
        player.getInventory().add(remainder);
        if (!remainder.isEmpty()) player.drop(remainder, true, Prediction.SERVER_ONLY);
        player.inventoryMenu.broadcastChanges();
    }

    public static void disablePlayer(ServerPlayer player) { returnItemOnDisable(player); }

    private static void broadcast(ServerPlayer owner, ItemStack stack) {
        var server = owner.level().getServer();
        if (server == null) return;
        for (ServerPlayer recipient : server.getPlayerList().getPlayers())
            if (recipient.level() == owner.level()) send(recipient, owner, stack);
    }

    private static void send(ServerPlayer recipient, ServerPlayer owner) {
        send(recipient, owner, access(owner).sum1rem0d$secondOffhand());
    }

    private static void send(ServerPlayer recipient, ServerPlayer owner, ItemStack stack) {
        if (!Rules.on("second_offhand")) stack = ItemStack.EMPTY;
        PlatformNetwork.send(recipient, new SecondOffhandStatePayload(owner.getUUID(), stack.copy()));
    }
}
