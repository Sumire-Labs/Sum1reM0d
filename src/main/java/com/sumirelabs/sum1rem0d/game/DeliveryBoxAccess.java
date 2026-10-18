package com.sumirelabs.sum1rem0d.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/** Short lived server authorization: the player must interact with their box before claiming. */
public final class DeliveryBoxAccess {
    private record Location(String dimension, long pos) {}
    private static final Map<UUID, Location> OPEN = new HashMap<>();
    private DeliveryBoxAccess() {}

    public static void open(ServerPlayer player, BlockPos pos, DeliveryBoxEntity box) {
        if (box.mayAccess(player)) OPEN.put(player.getUUID(), new Location(player.level().dimension().identifier().toString(), pos.asLong()));
    }
    public static boolean canClaim(ServerPlayer player) {
        Location location = OPEN.get(player.getUUID());
        if (location == null || !location.dimension().equals(player.level().dimension().identifier().toString())) return false;
        BlockPos pos = BlockPos.of(location.pos());
        if (!player.level().hasChunkAt(pos) || !(player.level().getBlockEntity(pos) instanceof DeliveryBoxEntity box)
            || !box.mayAccess(player) || !player.getUUID().equals(box.owner()) || !box.registeredDeliveryTarget(player.getUUID())) {
            OPEN.remove(player.getUUID());
            return false;
        }
        return player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) <= 64;
    }
    public static void clear(UUID player) { OPEN.remove(player); }
}
