package com.sumirelabs.sum1rem0d.game;

import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bounded world/movement rules shared by the narrow client and server hooks. */
public final class WorldMobility {
    private static final Map<ServerLevel, Long> NEXT_BOAT_ROLL = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<ServerLevel, long[]> TNT_AMPLIFICATIONS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<UUID, Set<UUID>> PLAYER_LEASH_TARGETS = new HashMap<>();
    private static final Map<MinecraftServer, Boolean> PREVIOUS_LEAD_STATE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<UUID> CRAWLING = new HashSet<>();
    private WorldMobility() {}

    public static boolean enabled(String feature) {
        if (!Sum1reM0d.initialized()) return false;
        return Rules.on(feature);
    }

    public static double minecartCap(ServerLevel level, double vanilla) {
        if (!enabled("minecart_speed")) return vanilla;
        if (Rules.b("minecart_speed", "unlimited")) return Double.MAX_VALUE;
        return Math.max(0, Math.min(100, Rules.n("minecart_speed", "max_speed")));
    }

    public static double boatCap(double vanilla) {
        if (!enabled("boat_speed")) return vanilla;
        if (Rules.b("boat_speed", "unlimited")) return Double.MAX_VALUE;
        return Math.max(0, Math.min(100, Rules.n("boat_speed", "max_speed")));
    }

    public static float boatAcceleration(AbstractBoat boat, float vanilla) {
        if (!enabled("boat_speed")) return vanilla;
        // Native water drag is approximately 0.9. This acceleration lets the configured cap be reachable.
        return (float) Math.max(0, Math.min(1, boatCap(vanilla) * 0.1));
    }

    public static void clampBoat(AbstractBoat boat) {
        if (!(boat.level() instanceof ServerLevel) || !enabled("boat_speed")) return;
        double cap = boatCap(0);
        Vec3 movement = boat.getDeltaMovement();
        double horizontal = Math.hypot(movement.x, movement.z);
        if (!Double.isFinite(horizontal)) { boat.setDeltaMovement(0, movement.y, 0); return; }
        if (cap == 0) { boat.setDeltaMovement(0, movement.y, 0); return; }
        if (horizontal > cap) boat.setDeltaMovement(movement.x / horizontal * cap, movement.y, movement.z / horizontal * cap);
    }

    public static float boatStep(AbstractBoat boat, float vanilla) {
        if (!(boat instanceof MobilityBoat marker) || !marker.sum1rem0d$blueIceBoost() || !enabled("boat_mob_spawn")) return vanilla;
        return (float) Math.max(0, Math.min(2, Rules.n("boat_mob_spawn", "step_height")));
    }

    public static float boatGroundFriction(AbstractBoat boat, float vanilla) {
        if (!(boat instanceof MobilityBoat marker) || !marker.sum1rem0d$blueIceBoost() || !enabled("boat_mob_spawn")) return vanilla;
        double multiplier = Math.max(1, Math.min(10, Rules.n("boat_mob_spawn", "blue_ice_speed")));
        return (float) Math.max(vanilla, Math.min(0.9999, 1 - (1 - 0.989) / multiplier));
    }

    public static float tntPower(PrimedTnt tnt, float vanilla) {
        if (!(tnt.level() instanceof ServerLevel level) || !enabled("tnt_cluster")) return vanilla;
        long window = level.getGameTime() / 20;
        long[] counter = TNT_AMPLIFICATIONS.computeIfAbsent(level, ignored -> new long[] {window, 0});
        synchronized (counter) {
            if (counter[0] != window) { counter[0] = window; counter[1] = 0; }
            int limit = Math.max(0, Math.min(256, Rules.i("tnt_cluster", "recursive_chain_limit")));
            if (limit == 0 || counter[1] >= limit) return vanilla;
            BlockPos center = tnt.blockPosition();
            for (int size = 3; size >= 2; size--) {
                for (int dx = -size + 1; dx <= 0; dx++) for (int dy = -size + 1; dy <= 0; dy++) for (int dz = -size + 1; dz <= 0; dz++) {
                    List<BlockPos> cluster = new ArrayList<>(size * size * size);
                    boolean complete = true;
                    for (int x = 0; x < size && complete; x++) for (int y = 0; y < size && complete; y++) for (int z = 0; z < size; z++) {
                        BlockPos pos = center.offset(dx + x, dy + y, dz + z);
                        if (!level.hasChunkAt(pos)) { complete = false; break; }
                        if (!pos.equals(center) && !level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.TNT)) { complete = false; break; }
                        if (!pos.equals(center)) cluster.add(pos.immutable());
                    }
                    if (!complete) continue;
                    counter[1]++;
                    // Consume exactly the matching cluster before the blast can prime its blocks again.
                    // Unrelated TNT outside the selected cube keeps vanilla chain behavior.
                    for (BlockPos pos : cluster) level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    double multiplier = Math.max(1, Math.min(9, Rules.n("tnt_cluster", size == 2 ? "power_2" : "power_3")));
                    return (float) Math.min(64, vanilla * multiplier);
                }
            }
        }
        return vanilla;
    }

    public static void tick(MinecraftServer server) {
        if (!Sum1reM0d.initialized()) return;
        boolean leadsEnabled = enabled("unbreakable_lead");
        boolean wasEnabled = PREVIOUS_LEAD_STATE.getOrDefault(server, leadsEnabled);
        if (wasEnabled && !leadsEnabled) releaseAllPlayerLeashes(server);
        PREVIOUS_LEAD_STATE.put(server, leadsEnabled);
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                tickCrawl(player);
                if (leadsEnabled) {
                    if (player.isAlive()) tickPlayerLeash(level, player);
                    else releasePlayerLeashes(player);
                }
            }
            trySpawnBoat(level);
        }
    }

    static void tickPlayerLeash(ServerLevel level, ServerPlayer player) {
        if (!enabled("unbreakable_lead") || !player.isAlive() || !(player instanceof net.minecraft.world.entity.Leashable leash)
                || leash.getLeashData() == null) return;
        // ServerPlayer overrides Player.tick without calling the base method on these versions.
        // Run vanilla's delayed NBT UUID resolution from the shared server tick path instead.
        net.minecraft.world.entity.Leashable.tickLeash(level, (ServerPlayer & net.minecraft.world.entity.Leashable) player);
    }

    public static boolean crawling(Player player) { return player instanceof ServerPlayer && CRAWLING.contains(player.getUUID()); }
    public static void clearCrawl(ServerPlayer player) { CRAWLING.remove(player.getUUID()); }

    public static synchronized void trackPlayerLeash(ServerPlayer holder, Entity target) {
        PLAYER_LEASH_TARGETS.computeIfAbsent(holder.getUUID(), ignored -> new HashSet<>()).add(target.getUUID());
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (!enabled("unbreakable_lead")) { releasePlayerLeashes(player); return; }
        if (player instanceof net.minecraft.world.entity.Leashable leash && leash.getLeashHolder() instanceof ServerPlayer holder) trackPlayerLeash(holder, player);
        for (net.minecraft.world.entity.Leashable target : net.minecraft.world.entity.Leashable.leashableLeashedTo(player)) trackPlayerLeash(player, (Entity) target);
    }

    public static void releasePlayerLeashes(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        Set<UUID> targets;
        synchronized (WorldMobility.class) { targets = PLAYER_LEASH_TARGETS.remove(player.getUUID()); }
        if (server != null && targets != null) for (UUID targetId : targets) {
            for (ServerLevel level : server.getAllLevels()) {
                Entity target = level.getEntity(targetId);
                if (target instanceof net.minecraft.world.entity.Leashable leash && referencesPlayerLeash(leash, player.getUUID())) clearLeashData(leash);
            }
        }
        for (net.minecraft.world.entity.Leashable leash : net.minecraft.world.entity.Leashable.leashableLeashedTo(player)) clearLeashData(leash);
        if (player instanceof net.minecraft.world.entity.Leashable own && own.getLeashData() != null) clearLeashData(own);
        if (server != null) for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
            if (candidate == player || !(candidate instanceof net.minecraft.world.entity.Leashable leash)) continue;
            if (referencesPlayerLeash(leash, player.getUUID())) clearLeashData(leash);
            synchronized (WorldMobility.class) {
                Set<UUID> held = PLAYER_LEASH_TARGETS.get(candidate.getUUID());
                if (held != null) held.remove(player.getUUID());
            }
        }
    }

    private static void releaseAllPlayerLeashes(MinecraftServer server) {
        Set<ServerPlayer> players = new LinkedHashSet<>(server.getPlayerList().getPlayers());
        for (ServerLevel level : server.getAllLevels()) players.addAll(level.players());
        Set<UUID> holderIds = new HashSet<>();
        for (ServerPlayer player : players) holderIds.add(player.getUUID());
        synchronized (WorldMobility.class) { holderIds.addAll(PLAYER_LEASH_TARGETS.keySet()); }
        // A loaded entity can still have deferred UUID leash data that is invisible to
        // leashableLeashedTo until its next entity tick. Clear those entries as part of
        // the same OFF transition so they cannot restore a leash afterward.
        for (ServerLevel level : server.getAllLevels()) for (Entity entity : level.getAllEntities()) {
            if (entity instanceof net.minecraft.world.entity.Leashable leash && referencesAnyPlayerLeash(leash, holderIds)) clearLeashData(leash);
        }
        for (ServerPlayer player : players) releasePlayerLeashes(player);
        synchronized (WorldMobility.class) { PLAYER_LEASH_TARGETS.clear(); }
    }

    private static boolean referencesPlayerLeash(net.minecraft.world.entity.Leashable leash, UUID holderId) {
        return referencesAnyPlayerLeash(leash, Set.of(holderId));
    }

    private static boolean referencesAnyPlayerLeash(net.minecraft.world.entity.Leashable leash, Set<UUID> holderIds) {
        var data = leash.getLeashData();
        if (data == null) return false;
        if (data.leashHolder != null && holderIds.contains(data.leashHolder.getUUID())) return true;
        return data.delayedLeashInfo != null && data.delayedLeashInfo.left().filter(holderIds::contains).isPresent();
    }

    private static void clearLeashData(net.minecraft.world.entity.Leashable leash) {
        if (leash.getLeashData() == null) return;
        if (leash.isLeashed()) leash.removeLeash();
        else leash.setLeashData(null);
    }

    public static void recordLeash(Entity target, Entity holder) {
        if (holder instanceof ServerPlayer player) trackPlayerLeash(player, target);
    }

    public static synchronized void untrackLeash(Entity target, Entity holder) {
        if (!(holder instanceof ServerPlayer player)) return;
        Set<UUID> targets = PLAYER_LEASH_TARGETS.get(player.getUUID());
        if (targets != null) {
            targets.remove(target.getUUID());
            if (targets.isEmpty()) PLAYER_LEASH_TARGETS.remove(player.getUUID());
        }
    }

    public static void handleCrawl(ServerPlayer player, boolean wanted) {
        if (!enabled("crawl")) {
            clearDisabledCrawl(player);
            sendCrawlState(player);
            return;
        }
        if (!player.isAlive() || player.isSpectator() || player.isSleeping() || player.isFallFlying() || player.isInWater() || player.isPassenger()) {
            if (CRAWLING.contains(player.getUUID())) tryStand(player);
            sendCrawlState(player);
            return;
        }
        if (wanted) {
            Pose previous = player.getPose();
            player.setPose(Pose.SWIMMING);
            if (!player.level().noCollision(player)) { player.setPose(previous); CRAWLING.remove(player.getUUID()); }
            else CRAWLING.add(player.getUUID());
        } else tryStand(player);
        sendCrawlState(player);
    }

    private static void tickCrawl(ServerPlayer player) {
        if (!CRAWLING.contains(player.getUUID())) return;
        if (!enabled("crawl")) { clearDisabledCrawl(player); sendCrawlState(player); return; }
        if (!player.isAlive() || player.isSpectator() || player.isSleeping() || player.isFallFlying() || player.isPassenger()) {
            if (tryStand(player)) sendCrawlState(player);
            return;
        }
        if (!player.isInWater()) player.setPose(Pose.SWIMMING);
    }

    private static boolean tryStand(ServerPlayer player) {
        player.setPose(Pose.STANDING);
        if (player.level().noCollision(player)) { CRAWLING.remove(player.getUUID()); return true; }
        player.setPose(Pose.SWIMMING);
        CRAWLING.add(player.getUUID());
        return false;
    }

    private static void clearDisabledCrawl(ServerPlayer player) {
        CRAWLING.remove(player.getUUID());
        player.setPose(Pose.STANDING);
        if (!player.level().noCollision(player)) player.setPose(Pose.SWIMMING);
    }

    private static void sendCrawlState(ServerPlayer player) {
        com.sumirelabs.sum1rem0d.network.PlatformNetwork.send(player,
                new com.sumirelabs.sum1rem0d.network.CrawlStatePayload(CRAWLING.contains(player.getUUID())));
    }

    public static float crawlSpeed(Player player, float vanilla) {
        if (!enabled("crawl") || !player.isAlive() || player.isInWater()) return vanilla;
        boolean active = player instanceof ServerPlayer ? CRAWLING.contains(player.getUUID())
                : com.sumirelabs.sum1rem0d.client.CrawlClientState.crawling();
        return active ? (float) (vanilla * Math.max(0.05, Math.min(1, Rules.n("crawl", "speed_multiplier")))) : vanilla;
    }

    public static boolean validSeatBlock(net.minecraft.world.level.block.state.BlockState state) {
        return enabled("seat_blocks") && (
                Rules.b("seat_blocks", "allow_stairs") && state.is(net.minecraft.tags.BlockTags.STAIRS)
                || Rules.b("seat_blocks", "allow_slabs") && state.is(net.minecraft.tags.BlockTags.SLABS)
                || Rules.b("seat_blocks", "allow_mushrooms")
                && net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(Identifier.withDefaultNamespace("shelf_mushroom"))
                        .map(state::is).orElse(false));
    }

    public static boolean trySit(net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, Player player) {
        if (!enabled("seat_blocks") || player.isPassenger() || !player.isAlive()) return false;
        if (!validSeatBlock(state)) return false;
        if (level.isClientSide()) return true;
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer)) return false;
        if (!server.hasChunkAt(pos)) return false;
        var existing = server.getEntitiesOfClass(SeatEntity.class, new AABB(pos).inflate(0.5, 1, 0.5))
                .stream().filter(seat -> seat.anchorPos().equals(pos)).findFirst().orElse(null);
        if (existing != null) {
            if (existing.getPassengers().isEmpty()) player.startRiding(existing);
            return true;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ModContent.id("seat")).orElse(null);
        if (type == null) return false;
        Entity entity = type.create(server, EntitySpawnReason.TRIGGERED);
        if (!(entity instanceof SeatEntity seat)) { if (entity != null) entity.discard(); return false; }
        double top = state.getShape(server, pos).max(net.minecraft.core.Direction.Axis.Y);
        seat.anchor(pos, pos.getX() + 0.5, pos.getY() + top, pos.getZ() + 0.5);
        if (!server.addFreshEntity(seat)) { seat.discard(); return false; }
        if (!player.startRiding(seat)) { seat.discard(); return false; }
        return true;
    }

    static void resetBoatSpawnRollForVerification(ServerLevel level) { NEXT_BOAT_ROLL.remove(level); }

    static void trySpawnBoat(ServerLevel level) {
        if (!enabled("boat_mob_spawn")) return;
        long now = level.getGameTime();
        long next = NEXT_BOAT_ROLL.getOrDefault(level, 0L);
        if (now < next) return;
        NEXT_BOAT_ROLL.put(level, now + Math.max(20, Math.min(120000, Rules.i("boat_mob_spawn", "interval_ticks"))));
        var eligiblePlayers = level.players().stream().filter(player -> player.isAlive() && !player.isSpectator()).toList();
        RandomSource random = level.getRandom();
        if (eligiblePlayers.isEmpty() || random.nextDouble() * 100 >= Math.max(0, Math.min(100, Rules.n("boat_mob_spawn", "chance")))) return;
        var player = eligiblePlayers.get(random.nextInt(eligiblePlayers.size()));
        var localBounds = new AABB(player.blockPosition()).inflate(128, 32, 128);
        long current = level.getEntitiesOfClass(AbstractBoat.class, localBounds).stream()
                .filter(boat -> boat instanceof MobilityBoat marker && marker.sum1rem0d$blueIceBoost()).count();
        if (current >= Math.max(0, Math.min(128, Rules.i("boat_mob_spawn", "max_boats")))) return;
        int radius = Math.max(4, Math.min(64, Rules.i("boat_mob_spawn", "radius")));
        for (int attempt = 0; attempt < 8; attempt++) {
            int x = player.blockPosition().getX() + random.nextInt(radius * 2 + 1) - radius;
            int z = player.blockPosition().getZ() + random.nextInt(radius * 2 + 1) - radius;
            // Do not ask the heightmap to generate or synchronously load an unloaded chunk.
            if (!level.hasChunkAt(new BlockPos(x, level.getMinY(), z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            if (!level.isEmptyBlock(feet) || !level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), net.minecraft.core.Direction.UP)) continue;
            AbstractBoat boat = net.minecraft.world.entity.EntityTypes.OAK_BOAT.create(level, EntitySpawnReason.EVENT);
            if (boat == null) return;
            boat.setPos(x + 0.5, y + 0.1, z + 0.5);
            boat.setYRot(random.nextFloat() * 360);
            ((MobilityBoat) boat).sum1rem0d$blueIceBoost(true);
            if (!level.noCollision(boat) || !level.addFreshEntity(boat)) { boat.discard(); continue; }
            Mob passenger = createPassenger(level, random);
            if (passenger == null) { boat.discard(); return; }
            passenger.setPos(boat.getX(), boat.getY() + 0.2, boat.getZ());
            if (!passenger.startRiding(boat) || !level.addFreshEntity(passenger)) { passenger.discard(); boat.discard(); return; }
            return;
        }
    }

    private static Mob createPassenger(ServerLevel level, RandomSource random) {
        List<String> configured = Rules.list("boat_mob_spawn", "entity_types");
        if (configured.isEmpty()) configured = List.of("minecraft:pig", "minecraft:cow", "minecraft:sheep", "minecraft:zombie", "minecraft:skeleton");
        for (int attempt = 0; attempt < Math.min(8, configured.size()); attempt++) {
            Identifier id = Identifier.tryParse(configured.get(random.nextInt(configured.size())));
            if (id == null) continue;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
            if (type == null) continue;
            Entity entity = type.create(level, EntitySpawnReason.EVENT);
            if (entity instanceof Mob mob) return mob;
            if (entity != null) entity.discard();
        }
        return null;
    }
}
