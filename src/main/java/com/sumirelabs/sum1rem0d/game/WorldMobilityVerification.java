package com.sumirelabs.sum1rem0d.game;

import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.storage.TagValueInput;

/** Opt-in native checks for capped vehicles and bounded, single-blast TNT clusters. */
public final class WorldMobilityVerification {
    private WorldMobilityVerification() {}

    public static void verify(MinecraftServer server, Consumer<String> passed) throws Exception {
        var config = Sum1reM0d.config();
        var original = config.snapshot();
        var features = original.getAsJsonObject("features");
        for (String id : new String[] {"minecart_speed", "boat_speed", "boat_mob_spawn", "tnt_cluster", "large_cauldron", "copper_chest", "leaf_pass", "seat_blocks", "unbreakable_lead", "crawl"}) {
            if (!features.has(id)) throw new AssertionError("Missing mobility Config feature: " + id);
        }
        var level = server.overworld();
        try {
            var draft = original.deepCopy();
            var rules = draft.getAsJsonObject("features");
            var minecart = rules.getAsJsonObject("minecart_speed");
            minecart.addProperty("enabled", true); minecart.addProperty("unlimited", false); minecart.addProperty("max_speed", 12.5);
            var boat = rules.getAsJsonObject("boat_speed");
            boat.addProperty("enabled", true); boat.addProperty("unlimited", false); boat.addProperty("max_speed", 7.25);
            var tnt = rules.getAsJsonObject("tnt_cluster");
            tnt.addProperty("enabled", true); tnt.addProperty("power_2", 2.0); tnt.addProperty("power_3", 3.0);
            tnt.addProperty("recursive_chain_limit", 4);
            config.update(draft, config.revision());
            require(Math.abs(WorldMobility.minecartCap(level, 0.4) - 12.5) < 1e-9, "Configured minecart cap");
            require(Math.abs(WorldMobility.boatCap(0.4) - 7.25) < 1e-9, "Configured boat cap");
            minecart.addProperty("max_speed", 0.0); boat.addProperty("max_speed", 0.0);
            config.update(draft, config.revision());
            require(WorldMobility.minecartCap(level, 0.4) == 0.0 && WorldMobility.boatCap(0.4) == 0.0, "Zero speed cap stops each vehicle");
            minecart.addProperty("enabled", false); boat.addProperty("enabled", false);
            config.update(draft, config.revision());
            require(Math.abs(WorldMobility.minecartCap(level, 0.4) - 0.4) < 1e-9 && Math.abs(WorldMobility.boatCap(0.4) - 0.4) < 1e-9,
                    "OFF restores the native vehicle cap");
            passed.accept("Minecart/boat native hooks: configured caps, zero-speed stop, and live OFF fallback");

            var boatEntity = (net.minecraft.world.entity.vehicle.boat.AbstractBoat) net.minecraft.world.entity.EntityTypes.OAK_BOAT.create(level, EntitySpawnReason.EVENT);
            require(boatEntity != null, "Native boat factory for unlimited-cap verification");
            minecart.addProperty("enabled", true); minecart.addProperty("unlimited", true);
            boat.addProperty("enabled", true); boat.addProperty("unlimited", true);
            config.update(draft, config.revision());
            require(WorldMobility.minecartCap(level, 0.4) == Double.MAX_VALUE && WorldMobility.boatCap(0.4) == Double.MAX_VALUE,
                    "Unlimited removes the configured hard caps with a finite, overflow-safe sentinel");
            require(Float.isFinite(WorldMobility.boatAcceleration(boatEntity, 0.04f)) && WorldMobility.boatAcceleration(boatEntity, 0.04f) == 1.0f,
                    "Unlimited boat acceleration remains finite and bounded at its native-safe control maximum");
            boatEntity.setDeltaMovement(8, 0, 0); WorldMobility.clampBoat(boatEntity);
            require(boatEntity.getDeltaMovement().x == 8, "Unlimited boat cap does not clamp ordinary high-speed movement");
            boatEntity.setDeltaMovement(1.0E200, 0, 1.0E200); WorldMobility.clampBoat(boatEntity);
            require(Double.isFinite(boatEntity.getDeltaMovement().x) && Double.isFinite(boatEntity.getDeltaMovement().z)
                    && boatEntity.getDeltaMovement().x == 1.0E200,
                    "Unlimited boat sentinel handles very large finite movement without NaN or overflow");
            minecart.addProperty("enabled", false); boat.addProperty("enabled", false); config.update(draft, config.revision());
            require(Math.abs(WorldMobility.minecartCap(level, 0.4) - 0.4) < 1e-9 && Math.abs(WorldMobility.boatCap(0.4) - 0.4) < 1e-9,
                    "Feature OFF returns vanilla caps even after the unlimited branch was active");
            minecart.addProperty("enabled", true); boat.addProperty("enabled", true);
            minecart.addProperty("max_speed", 12.5); boat.addProperty("max_speed", 7.25);
            minecart.addProperty("unlimited", false); boat.addProperty("unlimited", false);
            config.update(draft, config.revision());
            require(Math.abs(WorldMobility.minecartCap(level, 0.4) - 12.5) < 1e-9 && Math.abs(WorldMobility.boatCap(0.4) - 7.25) < 1e-9,
                    "Disabling unlimited re-enables the configured finite caps");
            boatEntity.setDeltaMovement(8, 0, 0); WorldMobility.clampBoat(boatEntity);
            require(Math.abs(boatEntity.getDeltaMovement().x - 7.25) < 1e-9, "Finite boat cap clamps live boat movement");
            boatEntity.setDeltaMovement(1.0E308, 0, 1.0E308); WorldMobility.clampBoat(boatEntity);
            require(Double.isFinite(boatEntity.getDeltaMovement().x) && Double.isFinite(boatEntity.getDeltaMovement().z)
                    && Math.abs(Math.hypot(boatEntity.getDeltaMovement().x, boatEntity.getDeltaMovement().z) - 7.25) < 1e-9,
                    "Finite cap clamps extreme movement without overflowing the vector normalization");
            boatEntity.discard();
            passed.accept("Minecart/boat unlimited cap option removes the hard ceiling safely and restores configured caps when disabled");

            minecart.addProperty("enabled", true); minecart.addProperty("max_speed", 12.5);
            boat.addProperty("enabled", true); boat.addProperty("max_speed", 7.25);
            tnt.addProperty("recursive_chain_limit", 4);
            config.update(draft, config.revision());
            BlockPos small = new BlockPos(2400, 240, 0);
            PrimedTnt entity = (PrimedTnt) net.minecraft.world.entity.EntityTypes.TNT.create(level, EntitySpawnReason.TRIGGERED);
            require(entity != null, "Native primed TNT factory"); entity.setPos(small.getX() + 1.5, small.getY() + 1.5, small.getZ() + 1.5);
            for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
                BlockPos cell = small.offset(x, y, z);
                if (!cell.equals(small.offset(1, 1, 1))) ServerVerification.place(level, cell, Blocks.TNT.defaultBlockState());
            }
            require(WorldMobility.tntPower(entity, 4.0f) == 8.0f, "Complete 2x2x2 cluster doubles power");
            for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
                BlockPos cell = small.offset(x, y, z);
                if (!cell.equals(small.offset(1, 1, 1))) require(level.getBlockState(cell).isAir(), "Amplified cluster cells are consumed exactly once");
            }
            BlockPos large = small.east(8);
            entity = (PrimedTnt) net.minecraft.world.entity.EntityTypes.TNT.create(level, EntitySpawnReason.TRIGGERED);
            require(entity != null, "Native primed TNT factory for large cluster"); entity.setPos(large.getX() + 1.5, large.getY() + 1.5, large.getZ() + 1.5);
            for (int x = 0; x < 3; x++) for (int y = 0; y < 3; y++) for (int z = 0; z < 3; z++) {
                BlockPos cell = large.offset(x, y, z);
                if (!cell.equals(large.offset(1, 1, 1))) ServerVerification.place(level, cell, Blocks.TNT.defaultBlockState());
            }
            require(WorldMobility.tntPower(entity, 4.0f) == 12.0f, "Complete 3x3x3 cluster triples power");
            for (int x = 0; x < 3; x++) for (int y = 0; y < 3; y++) for (int z = 0; z < 3; z++) {
                BlockPos cell = large.offset(x, y, z);
                if (!cell.equals(large.offset(1, 1, 1))) require(level.getBlockState(cell).isAir(), "Amplified large cluster cells are consumed exactly once");
            }
            passed.accept("TNT native hook: exact 2x2x2/3x3x3 matching, configured power, and no duplicate cluster priming");

            tnt.addProperty("recursive_chain_limit", 1); config.update(draft, config.revision());
            BlockPos bounded = small.south(8);
            entity = (PrimedTnt) net.minecraft.world.entity.EntityTypes.TNT.create(level, EntitySpawnReason.TRIGGERED);
            require(entity != null, "Native primed TNT factory for chain limit"); entity.setPos(bounded.getX() + 1.5, bounded.getY() + 1.5, bounded.getZ() + 1.5);
            for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
                BlockPos cell = bounded.offset(x, y, z);
                if (!cell.equals(bounded.offset(1, 1, 1))) ServerVerification.place(level, cell, Blocks.TNT.defaultBlockState());
            }
            require(WorldMobility.tntPower(entity, 4.0f) == 4.0f, "Per-second amplified-chain budget suppresses later clusters");
            require(level.getBlockState(bounded).is(Blocks.TNT), "Rejected amplified cluster is not consumed");
            passed.accept("TNT configured chain bound prevents extra amplification and preserves rejected TNT");

            testBoatMobSpawn(server, level, config, passed);
            testCauldrons(server, level, config, passed);
            testOtherMobility(server, level, config, passed);
        } finally {
            config.update(original, config.revision());
        }
    }

    private static void testBoatMobSpawn(MinecraftServer server, net.minecraft.server.level.ServerLevel level,
            com.sumirelabs.sum1rem0d.config.ConfigStore config, Consumer<String> passed) throws Exception {
        var previous = config.snapshot();
        var draft = previous.deepCopy();
        var rule = draft.getAsJsonObject("features").getAsJsonObject("boat_mob_spawn");
        var player = EconomyVerification.actor(server, level, "SumireBoatSpawn");
        BlockPos floor = new BlockPos(3320, 250, 0);
        BlockPos center = floor.offset(4, 1, 4);
        var bounds = new net.minecraft.world.phys.AABB(center).inflate(24, 12, 24);
        try {
            for (int x = 0; x < 9; x++) for (int z = 0; z < 9; z++)
                ServerVerification.place(level, floor.offset(x, 0, z), Blocks.STONE.defaultBlockState());
            player.setPos(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
            level.addNewPlayer(player);
            rule.addProperty("enabled", true);
            rule.addProperty("interval_ticks", 20);
            rule.addProperty("chance", 100);
            rule.addProperty("radius", 4);
            rule.addProperty("entity_types", "minecraft:pig");
            rule.addProperty("max_boats", 4);
            rule.addProperty("blue_ice_speed", 2.0);
            rule.addProperty("step_height", 1.0);
            config.update(draft, config.revision());

            WorldMobility.resetBoatSpawnRollForVerification(level);
            WorldMobility.trySpawnBoat(level);
            var spawned = level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.boat.AbstractBoat.class, bounds).stream()
                    .filter(boat -> boat instanceof MobilityBoat marker && marker.sum1rem0d$blueIceBoost()).toList();
            require(spawned.size() == 1, "A guaranteed native spawn roll creates exactly one marked mobility boat");
            var boat = spawned.getFirst();
            require(boat.getPassengers().size() == 1
                    && BuiltInRegistries.ENTITY_TYPE.getKey(boat.getPassengers().getFirst().getType()).equals(Identifier.withDefaultNamespace("pig"))
                    && boat.getPassengers().getFirst().getVehicle() == boat,
                    "Configured native passenger type is created and mounted in the spawned boat");
            require(WorldMobility.boatStep(boat, 0.6f) == 1.0f,
                    "Spawned boat receives the configured one-block step height");
            float vanillaFriction = 0.6f;
            require(WorldMobility.boatGroundFriction(boat, vanillaFriction) > vanillaFriction,
                    "Spawned boat receives blue-ice-like acceleration through boosted ground friction");

            rule.addProperty("max_boats", 1);
            config.update(draft, config.revision());
            WorldMobility.resetBoatSpawnRollForVerification(level);
            WorldMobility.trySpawnBoat(level);
            require(level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.boat.AbstractBoat.class, bounds).stream()
                    .filter(value -> value instanceof MobilityBoat marker && marker.sum1rem0d$blueIceBoost()).count() == 1,
                    "The configured special-boat cap prevents another spawn at capacity");

            rule.addProperty("max_boats", 4);
            rule.addProperty("enabled", false);
            config.update(draft, config.revision());
            WorldMobility.resetBoatSpawnRollForVerification(level);
            WorldMobility.trySpawnBoat(level);
            require(level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.boat.AbstractBoat.class, bounds).stream()
                    .filter(value -> value instanceof MobilityBoat marker && marker.sum1rem0d$blueIceBoost()).count() == 1,
                    "Config OFF stops subsequent boat-and-mob spawns");
            require(WorldMobility.boatStep(boat, vanillaFriction) == vanillaFriction
                    && WorldMobility.boatGroundFriction(boat, vanillaFriction) == vanillaFriction,
                    "Config OFF restores vanilla step height and ground friction on already spawned boats");
            passed.accept("Boat/mob spawn native roll, configured pig passenger, mounted state, blue-ice-like boost, step height, max-boats cap, and Config OFF");
        } finally {
            for (var boat : level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.boat.AbstractBoat.class, bounds)) {
                if (!(boat instanceof MobilityBoat marker) || !marker.sum1rem0d$blueIceBoost()) continue;
                for (var passenger : List.copyOf(boat.getPassengers())) passenger.discard();
                boat.discard();
            }
            WorldMobility.resetBoatSpawnRollForVerification(level);
            player.discard();
            config.update(previous, config.revision());
        }
    }

    private static void testOtherMobility(MinecraftServer server, net.minecraft.server.level.ServerLevel level,
            com.sumirelabs.sum1rem0d.config.ConfigStore config, Consumer<String> passed) throws Exception {
        var previous = config.snapshot();
        var draft = previous.deepCopy();
        var features = draft.getAsJsonObject("features");
        var chestRule = features.getAsJsonObject("copper_chest");
        var leafRule = features.getAsJsonObject("leaf_pass");
        var seatRule = features.getAsJsonObject("seat_blocks");
        var leashRule = features.getAsJsonObject("unbreakable_lead");
        var crawlRule = features.getAsJsonObject("crawl");
        var boatSpawnRule = features.getAsJsonObject("boat_mob_spawn");
        var user = EconomyVerification.actor(server, level, "SumireMobility");
        var target = EconomyVerification.actor(server, level, "SumireLeashTarget");
        net.minecraft.server.level.ServerPlayer reloadedPlayer = null;
        level.addNewPlayer(user); level.addNewPlayer(target);
        try {
            chestRule.addProperty("enabled", false); config.update(draft, config.revision());
            var copperBlock = BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath("minecraft", "copper_chest")).orElse(null);
            require(copperBlock instanceof ChestBlock, "Native copper chest registry entry exists");
            var copper = (ChestBlock) copperBlock;
            BlockPos legacyPos = new BlockPos(2800, 240, 0);
            ServerVerification.place(level, legacyPos, copper.defaultBlockState());
            var legacy = (ChestBlockEntity) level.getBlockEntity(legacyPos);
            require(legacy != null && legacy.getContainerSize() == 27, "Config OFF leaves a new copper chest at native size");
            legacy.setItem(26, new ItemStack(Items.DIAMOND, 3));
            chestRule.addProperty("enabled", true); config.update(draft, config.revision());
            require(legacy.getContainerSize() == 72 && legacy.getItem(26).getCount() == 3, "Enabling copper chests expands an old inventory without moving its contents");
            legacy.setItem(70, new ItemStack(Items.EMERALD, 5));
            var singleProvider = level.getBlockState(legacyPos).getMenuProvider(level, legacyPos);
            net.minecraft.world.inventory.ChestMenu singleMenu = singleProvider == null ? null : (net.minecraft.world.inventory.ChestMenu) singleProvider.createMenu(501, user.getInventory(), user);
            require(singleMenu != null && singleMenu.getType() == MobilityContent.COPPER_CHEST_8 && singleMenu.getRowCount() == 8 && singleMenu.slots.size() == 108,
                    "Single copper chest opens an eight-row custom menu with exactly 72 container and 36 player slots");
            var clientSingle = MobilityContent.COPPER_CHEST_8.create(501, user.getInventory());
            require(clientSingle.getType() == singleMenu.getType() && clientSingle.getRowCount() == 8 && clientSingle.slots.size() == singleMenu.slots.size(),
                    "Client factory and server single-chest menu share the registered type and slot layout");
            var chestTag = legacy.saveWithoutMetadata(level.registryAccess());
            chestRule.addProperty("enabled", false); config.update(draft, config.revision());
            var restored = copper.newBlockEntity(legacyPos, level.getBlockState(legacyPos));
            require(restored instanceof ChestBlockEntity, "Native copper chest block entity factory");
            restored.setLevel(level);
            restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), chestTag));
            var restoredChest = (ChestBlockEntity) restored;
            require(restoredChest.getContainerSize() == 72 && restoredChest.getItem(26).getCount() == 3 && restoredChest.getItem(70).getCount() == 5,
                    "Persisted expansion marker keeps both legacy and high slots after Config OFF and reload");

            chestRule.addProperty("enabled", true); config.update(draft, config.revision());
            BlockPos doublePos = new BlockPos(2824, 240, 0);
            var leftState = copper.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT);
            BlockPos partnerPos = ChestBlock.getConnectedBlockPos(doublePos, leftState);
            var rightState = copper.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT);
            ServerVerification.place(level, doublePos, leftState); ServerVerification.place(level, partnerPos, rightState);
            var doubleContainer = ChestBlock.getContainer(copper, level.getBlockState(doublePos), level, doublePos, false);
            require(doubleContainer != null && doubleContainer.getContainerSize() == 144, "Two adjacent expanded copper chest inventories combine to 144 slots");
            var partnerEntity = (ChestBlockEntity) level.getBlockEntity(partnerPos);
            require(partnerEntity != null, "Double copper chest partner block entity exists");
            var doubleProvider = level.getBlockState(doublePos).getMenuProvider(level, doublePos);
            net.minecraft.world.inventory.ChestMenu doubleMenu = doubleProvider == null ? null : (net.minecraft.world.inventory.ChestMenu) doubleProvider.createMenu(502, user.getInventory(), user);
            require(doubleMenu != null && doubleMenu.getType() == MobilityContent.COPPER_CHEST_16 && doubleMenu.getRowCount() == 16 && doubleMenu.slots.size() == 180,
                    "Double copper chest opens a 16-row custom menu with 144 container and 36 player slots");
            var clientDouble = MobilityContent.COPPER_CHEST_16.create(502, user.getInventory());
            require(clientDouble.getType() == doubleMenu.getType() && clientDouble.getRowCount() == 16 && clientDouble.slots.size() == doubleMenu.slots.size(),
                    "Client factory and server double-chest menu agree on type and all 180 slot indexes");
            user.setPos(doublePos.getX() + 0.5, doublePos.getY(), doublePos.getZ() + 0.5);
            require(doubleMenu.stillValid(user), "Native double-chest menu remains valid for a player within the opened chest distance");
            user.containerMenu = doubleMenu;
            var actualMenuContainer = doubleMenu.getContainer();
            var lastMenuSlot = doubleMenu.getSlot(143);
            require(lastMenuSlot.container == actualMenuContainer && lastMenuSlot.getContainerSlot() == 143
                    && actualMenuContainer.getContainerSize() == 144,
                    "Native menu slot 143 targets index 143 of the actual 144-slot combined container");
            doubleMenu.setCarried(new ItemStack(Items.DIAMOND));
            doubleMenu.clicked(143, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, user);
            var originEntity = (ChestBlockEntity) level.getBlockEntity(doublePos);
            var nativeTailEntity = ChestBlock.getBlockType(leftState) == net.minecraft.world.level.block.DoubleBlockCombiner.BlockType.FIRST
                    ? partnerEntity : originEntity;
            var nativeOtherEntity = nativeTailEntity == partnerEntity ? originEntity : partnerEntity;
            require(actualMenuContainer.getItem(143).is(Items.DIAMOND), "Actual ChestMenu container reads diamond from slot 143");
            require(doubleContainer.getItem(143).is(Items.DIAMOND), "Native ChestBlock combined container reads diamond from slot 143");
            require(nativeTailEntity.getItem(71).is(Items.DIAMOND), "Native combine-order tail chest receives its slot 71 item");
            require(nativeOtherEntity.getItem(71).isEmpty(), "The other chest half does not receive the item from combined slot 143");
            require(doubleMenu.getCarried().isEmpty(), "Successful native slot click consumes the carried diamond");
            user.containerMenu = user.inventoryMenu;
            passed.accept("Copper chest native size migration, 72/144 persistence, registered menu types, slot parity, and high-index click routing");

            leafRule.addProperty("enabled", true); leafRule.addProperty("inside_speed", 0.5); config.update(draft, config.revision());
            BlockPos leafPos = new BlockPos(2860, 250, 0);
            ServerVerification.place(level, leafPos, Blocks.OAK_LEAVES.defaultBlockState());
            var leaves = level.getBlockState(leafPos);
            require(leaves.getCollisionShape(level, leafPos, CollisionContext.empty()).isEmpty(), "Enabled leaves expose an empty native collision shape");
            user.setPos(leafPos.getX() + 0.2, leafPos.getY(), leafPos.getZ() + 0.5);
            user.setDeltaMovement(Vec3.ZERO);
            leaves.entityInside(level, leafPos, user, InsideBlockEffectApplier.NOOP, false);
            double before = user.getX(); user.move(MoverType.SELF, new Vec3(1, 0, 0));
            double slowedMove = user.getX() - before;
            require(slowedMove > 0 && slowedMove < 0.95, "Inside-leaf effect slows native entity movement");
            leafRule.addProperty("enabled", false); config.update(draft, config.revision());
            require(!leaves.getCollisionShape(level, leafPos, CollisionContext.empty()).isEmpty(), "Config OFF restores native leaf collision");
            user.setPos(leafPos.getX() + 0.2, leafPos.getY(), leafPos.getZ() + 0.5); user.setDeltaMovement(Vec3.ZERO);
            leaves.entityInside(level, leafPos, user, InsideBlockEffectApplier.NOOP, false);
            before = user.getX(); user.move(MoverType.SELF, new Vec3(1, 0, 0));
            require(user.getX() - before > 0.95, "Config OFF removes leaf slowdown");
            passed.accept("Leaf native collision, inside slowdown, and immediate Config OFF restoration");

            seatRule.addProperty("enabled", true); seatRule.addProperty("allow_stairs", true); config.update(draft, config.revision());
            BlockPos seatPos = new BlockPos(2890, 250, 0);
            ServerVerification.place(level, seatPos, Blocks.OAK_STAIRS.defaultBlockState());
            user.stopRiding(); user.setPos(seatPos.getX() + 0.5, seatPos.getY(), seatPos.getZ() + 0.5);
            require(WorldMobility.trySit(level, seatPos, level.getBlockState(seatPos), user) && user.getVehicle() instanceof SeatEntity,
                    "Native seat interaction mounts the player on a real anchor entity");
            var seat = (SeatEntity) user.getVehicle();
            require(seat.anchorPos().equals(seatPos) && !seat.shouldBeSaved(), "Seat remains anchored to its exact block and is not persisted");
            seatRule.addProperty("enabled", false); config.update(draft, config.revision()); seat.tick();
            require(seat.isRemoved() && user.getVehicle() == null, "Config OFF discards the seat and safely releases its passenger");
            require(!WorldMobility.trySit(level, seatPos, level.getBlockState(seatPos), user), "Config OFF refuses new seats");
            passed.accept("Seat native mount, exact anchor, transient lifecycle and Config OFF release");

            leashRule.addProperty("enabled", true); leashRule.addProperty("allow_players", true); leashRule.addProperty("max_length", 0); config.update(draft, config.revision());
            user.setPos(2920.5, 250, 0.5); target.setPos(2922.5, 250, 0.5); user.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LEAD));
            require(level.getEntity(user.getUUID()) == user && level.getEntity(target.getUUID()) == target,
                    "Original holder and leashed player are registered in native UUID lookup before save: holder="
                            + level.getEntity(user.getUUID()) + ", target=" + level.getEntity(target.getUUID()));
            require(user.interactOn(target, InteractionHand.MAIN_HAND, Vec3.ZERO) == InteractionResult.SUCCESS
                    && ((Leashable) target).getLeashHolder() == user, "Server-side player interaction creates a real player leash");
            var playerTagOut = net.minecraft.world.level.storage.TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            target.saveWithoutId(playerTagOut); var playerTag = playerTagOut.buildResult();
            require(playerTag.contains(Leashable.LEASH_TAG), "Player leash state is written to native player save data");
            reloadedPlayer = EconomyVerification.actor(server, level, "SumireLeashReload");
            reloadedPlayer.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), playerTag));
            var restoredLeashData = ((Leashable) reloadedPlayer).getLeashData();
            require(restoredLeashData != null, "Native player NBT restores the leash record before holder registration");
            var delayedLeashInfo = restoredLeashData.delayedLeashInfo;
            java.util.UUID savedHolderUuid = delayedLeashInfo == null ? null : delayedLeashInfo.left().orElse(null);
            if (savedHolderUuid == null && restoredLeashData.leashHolder != null) savedHolderUuid = restoredLeashData.leashHolder.getUUID();
            require(user.getUUID().equals(savedHolderUuid), "Native leash NBT stores the original holder UUID: expected="
                    + user.getUUID() + ", encoded=" + savedHolderUuid);
            if (reloadedPlayer.getUUID().equals(user.getUUID()) || reloadedPlayer.getUUID().equals(target.getUUID()))
                reloadedPlayer.setUUID(java.util.UUID.randomUUID());
            boatSpawnRule.addProperty("enabled", false); config.update(draft, config.revision());
            level.addNewPlayer(reloadedPlayer);
            require(level.getEntity(user.getUUID()) == user && level.getEntity(target.getUUID()) == target
                    && level.getEntity(reloadedPlayer.getUUID()) == reloadedPlayer,
                    "Original holder, original target and uniquely-identified reloaded player are all registered: holder="
                            + level.getEntity(user.getUUID()) + ", target=" + level.getEntity(target.getUUID())
                            + ", reloaded=" + level.getEntity(reloadedPlayer.getUUID()) + ", reloadedUUID=" + reloadedPlayer.getUUID());
            WorldMobility.tick(server);
            var restoredPlayerLeash = (Leashable) reloadedPlayer;
            var tickedLeashData = restoredPlayerLeash.getLeashData();
            require(restoredPlayerLeash.isLeashed() && restoredPlayerLeash.getLeashHolder() == user,
                    "Registered player's native tick resolves the saved leash UUID to its holder: encoded="
                            + savedHolderUuid + ", holderLookup=" + (level.getEntity(savedHolderUuid) == user)
                            + ", delayed=" + (tickedLeashData == null ? null : tickedLeashData.delayedLeashInfo)
                            + ", resolvedHolder=" + restoredPlayerLeash.getLeashHolder()
                            + ", holderRemoved=" + user.isRemoved() + ", targetRemoved=" + target.isRemoved()
                            + ", reloadedRemoved=" + reloadedPlayer.isRemoved());
            leashRule.addProperty("enabled", false); config.update(draft, config.revision()); WorldMobility.tick(server);
            require(!((Leashable) target).isLeashed() && !((Leashable) reloadedPlayer).isLeashed(),
                    "Turning off unbreakable leads releases original and NBT-restored player-held leashes on the server tick path");
            leashRule.addProperty("enabled", true); config.update(draft, config.revision());
            user.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LEAD));
            leashRule.addProperty("max_length", 1); config.update(draft, config.revision());
            require(user.interactOn(target, InteractionHand.MAIN_HAND, Vec3.ZERO) != InteractionResult.SUCCESS
                    && !((Leashable) target).isLeashed() && user.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
                    "Server rejects a player leash beyond the configured length without consuming the lead");
            passed.accept("Player leash native interaction, deferred NBT holder restoration, Config OFF release, and server length validation");

            crawlRule.addProperty("enabled", true); crawlRule.addProperty("speed_multiplier", 0.4); config.update(draft, config.revision());
            user.stopRiding(); user.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); user.setPos(2960.5, 250, 0.5); user.setPose(Pose.STANDING);
            float vanillaSpeed = user.getSpeed(); WorldMobility.handleCrawl(user, true);
            require(WorldMobility.crawling(user) && user.getPose() == Pose.SWIMMING, "Validated server crawl request sets the native swimming pose");
            float crawlingSpeed = user.getSpeed();
            float expectedCrawlingSpeed = vanillaSpeed * 0.4f;
            require(Math.abs(crawlingSpeed - expectedCrawlingSpeed) < 0.0001f,
                    "Native Player#getSpeed hook applies crawl multiplier: baseline=" + vanillaSpeed
                            + ", expected=" + expectedCrawlingSpeed + ", actual=" + crawlingSpeed
                            + ", active=" + WorldMobility.crawling(user) + ", pose=" + user.getPose());
            user.setGameMode(net.minecraft.world.level.GameType.SPECTATOR); WorldMobility.handleCrawl(user, true);
            require(!WorldMobility.crawling(user), "Server rejects crawl requests from spectators");
            user.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); WorldMobility.handleCrawl(user, true);
            crawlRule.addProperty("enabled", false); config.update(draft, config.revision()); WorldMobility.handleCrawl(user, false);
            require(!WorldMobility.crawling(user) && user.getPose() == Pose.STANDING && Math.abs(user.getSpeed() - vanillaSpeed) < 0.0001f,
                    "Config OFF clears crawl pose and restores vanilla movement speed");
            passed.accept("Crawl server validation, native pose/speed hooks, spectator rejection, and Config OFF reset");
        } finally {
            user.containerMenu = user.inventoryMenu;
            if (user.isPassenger()) user.stopRiding();
            if (reloadedPlayer != null) { WorldMobility.releasePlayerLeashes(reloadedPlayer); reloadedPlayer.discard(); }
            user.discard(); target.discard(); config.update(previous, config.revision());
        }
    }

    private static void testCauldrons(MinecraftServer server, net.minecraft.server.level.ServerLevel level,
            com.sumirelabs.sum1rem0d.config.ConfigStore config, Consumer<String> passed) throws Exception {
        var previous = config.snapshot(); var draft = previous.deepCopy();
        var option = draft.getAsJsonObject("features").getAsJsonObject("large_cauldron");
        option.addProperty("enabled", true); option.addProperty("auto_merge", true); config.update(draft, config.revision());
        var large = MobilityContent.largeCauldron(); var huge = MobilityContent.hugeCauldron();
        require(large != null && huge != null, "Both large-cauldron block variants registered");

        BlockPos pair = new BlockPos(2600, 240, 0);
        ServerVerification.place(level, pair, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL_CAULDRON, 2));
        ServerVerification.place(level, pair.east(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL_CAULDRON, 1));
        require(level.getBlockState(pair).getBlock() == large && level.getBlockState(pair.east()).getBlock() == large,
                "Adjacent native water cauldrons form a 2x1 cauldron");
        require(!level.getBlockState(pair).getValue(LargeCauldronBlock.Z_AXIS)
                && level.getBlockState(pair).getValue(LargeCauldronBlock.PART) == 0
                && level.getBlockState(pair.east()).getValue(LargeCauldronBlock.PART) == 1,
                "East-west 2x1 footprint part order");
        require(level.getBlockState(pair).getValue(LargeCauldronBlock.LEVEL) == 2
                && level.getBlockState(pair.east()).getValue(LargeCauldronBlock.LEVEL) == 1,
                "Native water levels survive 2x1 conversion");
        require(large.isWater(level.getBlockState(pair)) && large.availableWater(level, pair) == 3
                && large.availableWater(level, pair.east()) == 3, "Large cauldron exposes water availability from any footprint cell");
        require(!large.consumeWater(level, pair, 4) && large.availableWater(level, pair) == 3,
                "Large cauldron refuses an overdraw without changing persisted water");
        require(large.consumeWater(level, pair.east(), 1) && large.availableWater(level, pair) == 2
                && level.getBlockState(pair).getValue(LargeCauldronBlock.LEVEL) == 2
                && level.getBlockState(pair.east()).getValue(LargeCauldronBlock.LEVEL) == 0
                && large.isWater(level.getBlockState(pair)) && large.isWater(level.getBlockState(pair.east())),
                "Consuming one automation water unit persists across the complete footprint");
        passed.accept("Native cauldron merge: 2x1 conversion preserves each water cell");

        BlockPos vertical = pair.south(8);
        ServerVerification.place(level, vertical, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL_CAULDRON, 1));
        ServerVerification.place(level, vertical.south(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL_CAULDRON, 3));
        require(level.getBlockState(vertical).getBlock() == large && level.getBlockState(vertical.south()).getBlock() == large
                && level.getBlockState(vertical).getValue(LargeCauldronBlock.Z_AXIS)
                && level.getBlockState(vertical.south()).getValue(LargeCauldronBlock.PART) == 1,
                "North-south vanilla pair forms a z-axis 2x1 footprint");
        require(level.getBlockState(vertical).getValue(LargeCauldronBlock.LEVEL) == 1
                && level.getBlockState(vertical.south()).getValue(LargeCauldronBlock.LEVEL) == 3,
                "Z-axis conversion preserves cell amounts");
        passed.accept("Native cauldron merge: north-south orientation is saved and restored");

        BlockPos square = new BlockPos(2600, 240, 32);
        ServerVerification.place(level, square, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL_CAULDRON, 1));
        ServerVerification.place(level, square.east(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL_CAULDRON, 2));
        ServerVerification.place(level, square.south(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL_CAULDRON, 3));
        ServerVerification.place(level, square.east().south(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL_CAULDRON, 1));
        require(level.getBlockState(square).getBlock() == huge && level.getBlockState(square.east()).getBlock() == huge
                && level.getBlockState(square.south()).getBlock() == huge && level.getBlockState(square.east().south()).getBlock() == huge,
                "Four adjacent native cauldrons form a 2x2 footprint");
        int[] expected = {1, 2, 3, 1};
        for (int index = 0; index < 4; index++) require(level.getBlockState(square.offset(index % 2, 0, index / 2)).getValue(LargeCauldronBlock.LEVEL) == expected[index],
                "2x2 native merge retains cell level " + index);
        passed.accept("Native cauldron merge: 2x2 footprint retains all four cell levels");

        BlockPos partlyEmpty = square.south(8);
        for (int index = 0; index < 4; index++) {
            BlockPos cell = partlyEmpty.offset(index % 2, 0, index / 2);
            var nativeState = index == 0
                    ? Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL_CAULDRON, 2)
                    : Blocks.CAULDRON.defaultBlockState();
            ServerVerification.place(level, cell, nativeState);
        }
        BlockPos drySegment = partlyEmpty.south().east();
        require(level.getBlockState(drySegment).getBlock() == huge && huge.isWater(level, drySegment)
                && huge.availableWater(level, drySegment) == 2,
                "A dry cell of a partially filled 2x2 footprint can query the footprint's water");
        require(huge.consumeWater(level, drySegment, 1) && huge.availableWater(level, partlyEmpty) == 1,
                "Automation consumes water through a dry segment's footprint root");
        passed.accept("Large-cauldron water API resolves complete footprints from empty segments");

        BlockPos incompatible = square.east(8);
        ServerVerification.place(level, incompatible, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL_CAULDRON, 2));
        ServerVerification.place(level, incompatible.east(), Blocks.LAVA_CAULDRON.defaultBlockState());
        require(level.getBlockState(incompatible).is(Blocks.WATER_CAULDRON) && level.getBlockState(incompatible.east()).is(Blocks.LAVA_CAULDRON),
                "Mixed water/lava native cauldrons are not merged");
        passed.accept("Native cauldron merge refuses mixed liquids");

        var actor = EconomyVerification.actor(server, level, "SumireCauldron");
        try {
            BlockPos buckets = new BlockPos(2632, 240, 0);
            ServerVerification.place(level, buckets, large.defaultBlockState()); large.setPlacedBy(level, buckets, level.getBlockState(buckets), actor, new ItemStack(large));
            var hit = new BlockHitResult(new Vec3(buckets.getX()+0.5, buckets.getY()+0.5, buckets.getZ()+0.5), Direction.UP, buckets, false);
            actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            require(large.useItemOn(actor.getItemInHand(InteractionHand.MAIN_HAND), level.getBlockState(buckets), level, buckets, actor, InteractionHand.MAIN_HAND, hit) == InteractionResult.CONSUME
                    && actor.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "One water bucket fills a 2x1 unit and returns its bucket");
            actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
            require(large.useItemOn(actor.getItemInHand(InteractionHand.MAIN_HAND), level.getBlockState(buckets), level, buckets, actor, InteractionHand.MAIN_HAND, hit) == InteractionResult.FAIL
                    && actor.getItemInHand(InteractionHand.MAIN_HAND).is(Items.LAVA_BUCKET), "Different liquid is rejected without consuming the bucket");
            actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            require(large.useItemOn(actor.getItemInHand(InteractionHand.MAIN_HAND), level.getBlockState(buckets), level, buckets, actor, InteractionHand.MAIN_HAND, hit) == InteractionResult.CONSUME
                    && actor.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "A second water bucket fills the remaining 2x1 unit");
            require(level.getBlockState(buckets).getValue(LargeCauldronBlock.LEVEL) == 3
                    && level.getBlockState(buckets.east()).getValue(LargeCauldronBlock.LEVEL) == 3, "Six units fill both 2x1 cells");
            actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            require(large.useItemOn(actor.getItemInHand(InteractionHand.MAIN_HAND), level.getBlockState(buckets), level, buckets, actor, InteractionHand.MAIN_HAND, hit) == InteractionResult.FAIL
                    && actor.getItemInHand(InteractionHand.MAIN_HAND).is(Items.WATER_BUCKET), "A third water bucket is rejected at full capacity");
            actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
            require(large.useItemOn(actor.getItemInHand(InteractionHand.MAIN_HAND), level.getBlockState(buckets), level, buckets, actor, InteractionHand.MAIN_HAND, hit) == InteractionResult.CONSUME
                    && actor.getItemInHand(InteractionHand.MAIN_HAND).is(Items.WATER_BUCKET), "Empty bucket drains exactly one 3-unit water bucket");
            require(level.getBlockState(buckets).getValue(LargeCauldronBlock.LEVEL) == 3
                    && level.getBlockState(buckets.east()).getValue(LargeCauldronBlock.LEVEL) == 0, "Draining preserves the remaining 3 units");

            option.addProperty("enabled", false); config.update(draft, config.revision());
            actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            require(large.useItemOn(actor.getItemInHand(InteractionHand.MAIN_HAND), level.getBlockState(buckets), level, buckets, actor, InteractionHand.MAIN_HAND, hit) == InteractionResult.FAIL
                    && level.getBlockState(buckets).getValue(LargeCauldronBlock.LEVEL) == 3,
                    "Config OFF preserves liquid and refuses new fills");
            passed.accept("Large cauldron bucket transfer, incompatible-liquid/full-capacity safety, and Config OFF conservation");
        } finally { actor.discard(); config.update(previous, config.revision()); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
