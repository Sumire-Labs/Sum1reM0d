package com.sumirelabs.sum1rem0d.game;

import com.mojang.authlib.GameProfile;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;

/** Invoked only by the opt-in native verifier in an empty disposable server world. */
final class PlayerVerification {
    private PlayerVerification() {}
    static void verify(MinecraftServer server, ServerLevel level, WorldState state, Consumer<String> passed) throws Exception {
        var player = new TestPlayer(server, level, "SumireVerifyA");
        var peer = new TestPlayer(server, level, "SumireVerifyB");
        player.setPos(0.5, 245, 0.5); peer.setPos(2.5, 245, .5);
        level.addNewPlayer(player); level.addNewPlayer(peer);
        var config = Sum1reM0d.config(); var original = config.snapshot();
        try {
            require(player.fall(1.99) <= 0 && player.fall(2) == 1, "Fall damage begins at two blocks");
            require(new ItemStack(Items.WOODEN_PICKAXE).getMaxDamage() == 1, "Wooden pickaxe durability is one");
            passed.accept("native player fall threshold and wooden pickaxe durability");
            effectTag(level, state, player);
            passed.accept("registered effect tags expand to actual effects after native player damage");
            var fakeConfig = config.snapshot();
            fakeConfig.getAsJsonObject("features").getAsJsonObject("sleep_advancement").addProperty("fake_only", true);
            fakeConfig.getAsJsonObject("features").getAsJsonObject("sleep_advancement").addProperty("chance", 100);
            config.update(fakeConfig, config.revision());
            var fakeAdvancement = Gameplay.sleepAdvancement(player);
            require(fakeAdvancement != null && !player.getAdvancements().getOrStartProgress(fakeAdvancement).isDone(), "Fake advancement does not award actual progress");
            var payload = new com.sumirelabs.sum1rem0d.network.FakeAdvancementPayload(fakeAdvancement.id(), fakeAdvancement.value().display().orElseThrow());
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), server.registryAccess());
            try {
                com.sumirelabs.sum1rem0d.network.FakeAdvancementPayload.CODEC.encode(buffer, payload);
                var decoded = com.sumirelabs.sum1rem0d.network.FakeAdvancementPayload.CODEC.decode(buffer);
                require(decoded.id().equals(payload.id()) && decoded.display().title().equals(payload.display().title()) && decoded.display().icon().equals(payload.display().icon()), "Fake toast display payload round-trips with registry data");
            } finally { buffer.release(); }
            fakeConfig.getAsJsonObject("features").getAsJsonObject("sleep_advancement").addProperty("fake_only", false);
            config.update(fakeConfig, config.revision());
            var actualAdvancement = Gameplay.sleepAdvancement(player);
            require(actualAdvancement != null && player.getAdvancements().getOrStartProgress(actualAdvancement).isDone() && !actualAdvancement.id().getPath().startsWith("recipes/"), "Real mode awards a non-recipe advancement");
            passed.accept("fake advancement leaves progress unchanged, payload round-trip and real award");
            sleep(server, level, state, player);
            passed.accept("real bed sleep, early wake, vanilla wake boundary and configurable night-skip timing");
            events(server, level, state, passed);
            cameraAndSheep(server, level, state, passed);
            explosions(server, level, state, passed);
            miningAndWeather(server, level, state, passed);
            knockbackMob(server, level, passed);
            bedrockAndSound(server, level, state, passed);
            exchange(server, level, state, player, peer);
            scheduledZeus(server, level, state, passed);
            sleepWarnings(server, level, state, passed);
            dailyPersistence(server, level, state, passed);
            scheduledKnockback(server, level, state, passed);
            periodicExchanges(server, level, state, passed);
            passed.accept("inventory row exchange conserves items, preserves hotbar and leaves an odd player unchanged");
            passed.accept("name permutation preserves identities and original-name hover/insertion");
            var p = state.player(player); p.cleanliness = 0;
            Gameplay.tickPlayer(player, state, 20);
            require(player.getMaxHealth() == 10 && player.getHealth() == 10, "Dirt caps health at five hearts");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModContent.soap(), 2));
            ModContent.soap().use(level, player, InteractionHand.MAIN_HAND);
            Gameplay.tickPlayer(player, state, 40);
            require(p.cleanliness > 49 && player.getMaxHealth() == 18 && player.getHealth() == 10 && player.getMainHandItem().getCount() == 1, "Soap consumes one; restoring maximum health does not heal");
            ModContent.soap().use(level, player, InteractionHand.MAIN_HAND); Gameplay.tickPlayer(player, state, 60);
            require(player.getMaxHealth() == 20 && player.getHealth() == 10, "Cleanliness restores full maximum without healing");
            passed.accept("cleanliness health cap, soap consumption and no automatic healing");
            var candidate = config.snapshot(); var features = candidate.getAsJsonObject("features");
            features.getAsJsonObject("vegan").addProperty("enabled", false);
            features.getAsJsonObject("environment_anger").addProperty("chance", 100);
            features.getAsJsonObject("damage_effects").addProperty("buff_chance", 100);
            features.getAsJsonObject("damage_effects").addProperty("buff_effects", "minecraft:speed");
            features.getAsJsonObject("damage_effects").addProperty("cooldown", 0);
            features.getAsJsonObject("influenza").addProperty("exposure", 1);
            config.update(candidate, config.revision());
            p.environmentTicks = 0; p.angerTicks = 0;
            Gameplay.broke(player, Blocks.OAK_LEAVES.defaultBlockState(), ItemStack.EMPTY);
            Gameplay.broke(player, Blocks.SHORT_GRASS.defaultBlockState(), ItemStack.EMPTY);
            require(p.environmentTicks == 200 && p.angerTicks == 100, "Environment warnings add five seconds per block; grass causes poison timer");
            Gameplay.tickPlayer(player, state, 80);
            require(player.hasEffect(ModContent.effect("environment_group")) && player.hasEffect(ModContent.effect("environment_anger")), "Environment timers apply custom effects");
            player.setInvulnerableTime(0); player.setHealth(20); p.damageCooldown = 0;
            player.hurtServer(level, player.damageSources().generic(), 1);
            require(player.getHealth() < 20 && player.hasEffect(MobEffects.SPEED), "Real damage grants configured buff");
            passed.accept("environment warning accumulation, anger effect and buff on real damage");
            p.influenzaTicks = 0; p.exposureTicks = 0;
            Gameplay.tickPlayer(player, state, 100);
            require(p.influenzaTicks > 0 && player.hasEffect(ModContent.effect("influenza")), "Two nearby players trigger influenza after exposure");
            Gameplay.ate(player, new ItemStack(Items.MILK_BUCKET));
            require(p.influenzaTicks == 0 && p.exposureTicks == 0, "Milk clears influenza timers");
            peer.setPos(100, 245, .5); Gameplay.tickPlayer(player, state, 120);
            require(p.exposureTicks == 0 && !player.hasEffect(ModContent.effect("influenza")), "Leaving range clears continuous exposure");
            passed.accept("influenza proximity, milk cure and exposure reset");
            p.awakeTicks = 24000 * 3L; Gameplay.tickPlayer(player, state, 140);
            require(player.hasEffect(ModContent.effect("sleep_hunger")) && !player.hasEffect(ModContent.effect("sleep_slowness")), "Three days cause hunger only");
            p.awakeTicks = 24000 * 4L; Gameplay.tickPlayer(player, state, 160);
            require(player.hasEffect(ModContent.effect("sleep_slowness")), "Four days cause slowness");
            candidate = config.snapshot(); features = candidate.getAsJsonObject("features");
            for (String feature : java.util.List.of("sleep_deprivation", "cleanliness", "influenza", "environment_group", "environment_anger")) features.getAsJsonObject(feature).addProperty("enabled", false);
            config.update(candidate, config.revision()); Gameplay.tickPlayer(player, state, 180);
            require(player.getMaxHealth() == 20 && !player.hasEffect(ModContent.effect("sleep_hunger")) && !player.hasEffect(ModContent.effect("sleep_slowness")) && !player.hasEffect(ModContent.effect("environment_group")) && !player.hasEffect(ModContent.effect("environment_anger")), "Disabling rules clears active effects and health cap");
            passed.accept("sleep hunger and slowness thresholds; disabled effects are removed");
            int afkThreshold = Rules.i("afk_white", "after") * 20;
            p.afkTicks = afkThreshold - 1; Gameplay.tickPlayer(player, state, 181);
            require(p.afkTicks == afkThreshold, "Idle timer crosses the configured AFK threshold");
            player.setYRot(player.getYRot() + 1); Gameplay.tickPlayer(player, state, 182);
            require(p.afkTicks == 0, "Camera movement resets AFK immediately");
            p.afkTicks = afkThreshold; player.resetLastActionTime(); Gameplay.tickPlayer(player, state, 183);
            require(p.afkTicks == 0, "Input activity resets AFK");
            p.afkTicks = afkThreshold; player.setPos(player.getX() + .25, player.getY(), player.getZ()); Gameplay.tickPlayer(player, state, 184);
            require(p.afkTicks == 0, "Position changes reset AFK");
            passed.accept("AFK threshold, camera, input activity and movement reset");
            candidate = config.snapshot(); candidate.getAsJsonObject("features").getAsJsonObject("sleep_deprivation").addProperty("enabled", true); config.update(candidate, config.revision());
            state.player(peer).awakeTicks = 24000 * 5L; Gameplay.tickPlayer(peer, state, 200);
            require(!peer.isAlive(), "Five sleepless days kill the player");
            Gameplay.tickPlayer(peer, state, 201);
            require(state.player(peer).awakeTicks == 0, "Death resets sleeplessness");
            passed.accept("five sleepless days kill and death resets sleep timer");
            candidate = config.snapshot(); candidate.getAsJsonObject("features").getAsJsonObject("vegan").addProperty("enabled", true); config.update(candidate, config.revision());
            var fixture = state.checkpoint(); fixture.getAsJsonObject("daily").addProperty("vegan", true); state.restore(fixture);
            player.removeAllEffects(); player.setHealth(20); Gameplay.ate(player, new ItemStack(Items.BREAD));
            require(player.isAlive(), "Plant food is safe on vegan days");
            Gameplay.ate(player, new ItemStack(Items.COOKED_BEEF)); require(!player.isAlive(), "Meat kills on vegan days");
            passed.accept("vegan day allows bread and kills after eating meat");
        } finally {
            config.update(original, config.revision());
            level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED); level.removePlayerImmediately(peer, Entity.RemovalReason.DISCARDED);
        }
    }
    private static void effectTag(ServerLevel level, WorldState state, TestPlayer player) throws Exception {
        var config = Sum1reM0d.config(); var previous = config.snapshot();
        try {
            for (String group : java.util.List.of("buffs", "debuffs")) {
                var tag = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getTags().filter(entries -> entries.key().location().equals(ModContent.id("random_" + group)) && entries.size() == 5).findFirst().orElseThrow(() -> new AssertionError("Bundled effect tag must load through the data pack: " + group));
                var draft = config.snapshot(); var options = draft.getAsJsonObject("features").getAsJsonObject("damage_effects"); options.addProperty("enabled", true); options.addProperty("chance", 100); options.addProperty("buff_chance", group.equals("buffs") ? 100 : 0); options.addProperty(group.equals("buffs") ? "buff_effects" : "debuff_effects", "#" + tag.key().location()); options.addProperty("cooldown", 0); config.update(draft, config.revision());
                player.removeAllEffects(); player.damageCooldownTime = 0; player.setHealth(20); state.player(player).damageCooldown = 0;
                require(player.hurtServer(level, player.damageSources().generic(), 1), "Native damage for effect-tag fixture");
                require(tag.stream().anyMatch(player::hasEffect), "Damage selects and applies an effect from the configured registered tag: " + group);
            }
        } finally { player.removeAllEffects(); player.setHealth(20); player.damageCooldownTime = 0; config.update(previous, config.revision()); }
    }
    private static void sleep(MinecraftServer server, ServerLevel level, WorldState state, TestPlayer player) throws Exception {
        var config = Sum1reM0d.config(); var previous = config.snapshot(); var position = player.position(); long clock = level.getOverworldClockTime();
        var bed = new net.minecraft.core.BlockPos(80, 240, 8);
        try {
            var candidate = previous.deepCopy(); var features = candidate.getAsJsonObject("features");
            features.getAsJsonObject("sleep_deprivation").addProperty("enabled", true); features.getAsJsonObject("sleep_deprivation").addProperty("sleep_seconds", 5); features.getAsJsonObject("sleep_deprivation").addProperty("dimensions", "minecraft:overworld");
            features.getAsJsonObject("sleep_advancement").addProperty("enabled", true); features.getAsJsonObject("sleep_advancement").addProperty("fake_only", true); features.getAsJsonObject("sleep_advancement").addProperty("chance", 100);
            features.getAsJsonObject("vegan").addProperty("enabled", false); config.update(candidate, config.revision());
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set midnight");
            level.environmentAttributes().invalidateTickCache();
            level.updateSkyBrightness();
            ServerVerification.place(level, bed.below(), Blocks.STONE.defaultBlockState()); ServerVerification.place(level, bed.east().below(), Blocks.STONE.defaultBlockState());
            var foot = Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, net.minecraft.core.Direction.EAST).setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT);
            var head = foot.setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD);
            ServerVerification.place(level, bed, foot); ServerVerification.place(level, bed.east(), head);
            player.setPos(bed.getX() + 1.5, bed.getY() + 1, bed.getZ() + .5);
            var data = state.player(player); data.awakeTicks = 24000 * 4L; data.sleepWarningStage = 2;
            var block = (net.minecraft.world.level.block.AbstractBedBlock) Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED);
            var start = player.startSleepInBed(block, head, block.getBedRule(level, bed.east()), bed.east());
            require(start.right().isPresent(), "Real overworld bed accepts sleeping at midnight: " + start);
            for (int tick = 1; tick < 100; tick++) { player.doTick(); Gameplay.tickPlayer(player, state, tick); }
            require(player.isSleeping() && !player.isSleepingLongEnough() && data.awakeTicks >= 24000 * 4L, "Ninety-nine ticks do not count as full rest");
            player.stopSleepInBed(true, true); require(data.awakeTicks >= 24000 * 4L && !data.sleepAwarded && data.sleepWarningStage == 2, "Early wake does not clear deprivation, its warning stage or award a random advancement");
            require(player.startSleepInBed(block, head, block.getBedRule(level, bed.east()), bed.east()).right().isPresent(), "Bed can be used again");
            for (int tick = 1; tick < 100; tick++) { player.doTick(); Gameplay.tickPlayer(player, state, tick); }
            require(data.awakeTicks >= 24000 * 4L && !data.sleepAwarded, "Interrupted sleep is not combined when re-entering bed in the same server tick");
            player.doTick(); require(player.getSleepTimer() == 100 && player.isSleepingLongEnough(), "Vanilla hundred-tick wake boundary qualifies");
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time add 6000");
            player.stopSleepInBed(false, true); require(data.awakeTicks == 0 && data.sleepWarningStage == 0 && data.sleepAwarded, "Wake hook grants rest and resets warnings before vanilla clears the sleep timer"); Gameplay.tickPlayer(player, state, 101);
            require(data.awakeTicks == 0, "Skipped night is not added back to a rested player's awake timer");
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set midnight"); level.environmentAttributes().invalidateTickCache(); level.updateSkyBrightness(); data.lastClock = level.getOverworldClockTime();
            candidate.getAsJsonObject("features").getAsJsonObject("sleep_deprivation").addProperty("sleep_seconds", 10); config.update(candidate, config.revision()); data.awakeTicks = 24000 * 4L;
            require(player.startSleepInBed(block, head, block.getBedRule(level, bed.east()), bed.east()).right().isPresent(), "Bed accepts extended sleep");
            for (int tick = 1; tick <= 100; tick++) { player.doTick(); Gameplay.tickPlayer(player, state, tick); }
            require(!player.isSleepingLongEnough() && data.awakeTicks >= 24000 * 4L, "Ten-second setting prevents vanilla night skip at five seconds");
            for (int tick = 101; tick <= 200; tick++) { player.doTick(); Gameplay.tickPlayer(player, state, tick); }
            require(player.isSleepingLongEnough() && data.awakeTicks == 0 && data.sleepAwarded, "Ten seconds qualify despite vanilla's capped sleep counter"); player.stopSleepInBed(true, true);
        } finally {
            if (player.isSleeping()) player.stopSleepInBed(true, true);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set " + clock);
            level.environmentAttributes().invalidateTickCache();
            level.updateSkyBrightness();
            player.setPos(position); state.player(player).lastClock = level.getOverworldClockTime(); config.update(previous, config.revision());
        }
    }
    private static void events(MinecraftServer server, ServerLevel level, WorldState state, Consumer<String> passed) throws Exception {
        var config = Sum1reM0d.config(); var previous = config.snapshot();
        var lightningPlayer = new TestPlayer(server, level, "SumireVerifyZeus");
        var roof = new net.minecraft.core.BlockPos(120, 245, 32); lightningPlayer.setPos(120.5, 245, 32.5); level.addNewPlayer(lightningPlayer);
        try {
            var draft = config.snapshot(); draft.getAsJsonObject("features").getAsJsonObject("zeus").addProperty("enabled", true); draft.getAsJsonObject("features").getAsJsonObject("zeus").addProperty("ceiling_distance", 0); config.update(draft, config.revision());
            ServerVerification.place(level, roof.above(2), Blocks.STONE.defaultBlockState());
            state.player(lightningPlayer).lightningTicks = 1; Gameplay.tickPlayer(lightningPlayer, state, 211); require(lightningPlayer.isAlive(), "Standing player is protected by a roof");
            ServerVerification.place(level, roof.above(2), Blocks.AIR.defaultBlockState()); lightningPlayer.setPose(net.minecraft.world.entity.Pose.SWIMMING);
            ServerVerification.place(level, roof.above(), Blocks.STONE.defaultBlockState()); state.player(lightningPlayer).lightningTicks = 1; Gameplay.tickPlayer(lightningPlayer, state, 212);
            require(lightningPlayer.isAlive(), "Crawling player is protected by a one-block-high roof");
            ServerVerification.place(level, roof.above(), Blocks.AIR.defaultBlockState()); lightningPlayer.setPose(net.minecraft.world.entity.Pose.STANDING); state.player(lightningPlayer).lightningTicks = 1; Gameplay.tickPlayer(lightningPlayer, state, 213);
            require(!lightningPlayer.isAlive(), "Exposed player dies to Zeus"); passed.accept("Zeus protects standing and crawling players under roofs and kills exposed players");
        } finally {
            level.removePlayerImmediately(lightningPlayer, Entity.RemovalReason.DISCARDED);
            removeFixtureEntities(level, new net.minecraft.world.phys.AABB(110, 235, 22, 130, 255, 42));
            config.update(previous, config.revision());
        }
        for (var dimension : java.util.List.of(net.minecraft.world.level.Level.NETHER, net.minecraft.world.level.Level.END)) {
            var target = server.getLevel(dimension); require(target != null, "Verification dimension exists");
            var player = new TestPlayer(server, target, dimension == net.minecraft.world.level.Level.NETHER ? "SumireBedNether" : "SumireBedEnd"); var origin = new net.minecraft.core.BlockPos(160, 241, 32); player.setPos(160.5, 241, 32.5); target.addNewPlayer(player); state.player(player).bedCooldown = 0;
            try {
                var draft = config.snapshot(); var options = draft.getAsJsonObject("features").getAsJsonObject("warden_bed"); options.addProperty("enabled", true); options.addProperty("dimensions", "minecraft:the_nether,minecraft:the_end"); options.addProperty("radius", 3); options.addProperty("count", 2); options.addProperty("cooldown", 60); config.update(draft, config.revision());
                for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) ServerVerification.place(target, origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                var foot = Blocks.BED.pick(DyeColor.RED).defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, net.minecraft.core.Direction.EAST).setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT);
                ServerVerification.place(target, origin, foot); ServerVerification.place(target, origin.east(), foot.setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD));
                var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(origin), net.minecraft.core.Direction.UP, origin, false);
                require(target.getBlockState(origin).useWithoutItem(target, player, hit).consumesAction(), "Colored bed use is handled");
                require(wardens(target) == 2 && target.getBlockState(origin).getBlock() instanceof net.minecraft.world.level.block.BedBlock && !player.isSleeping(), "Bed remains and spawns configured wardens without sleeping or explosion: " + dimension.identifier() + " count=" + wardens(target) + " block=" + target.getBlockState(origin) + " cooldown=" + state.player(player).bedCooldown);
                target.getBlockState(origin).useWithoutItem(target, player, hit); require(wardens(target) == 2, "Bed cooldown prevents repeated summons");
                removeFixtureEntities(target, new net.minecraft.world.phys.AABB(150, 230, 22, 170, 255, 42)); state.player(player).bedCooldown = 0;
                //? if >=26.3 {
                target.setBlock(origin.east(), Blocks.AIR.defaultBlockState(), 2); target.setBlock(origin, Blocks.STRAW_BED.defaultBlockState(), 2);
                target.getBlockState(origin).useWithoutItem(target, player, hit); require(wardens(target) == 2 && target.getBlockState(origin).is(Blocks.STRAW_BED), "Straw bed also summons wardens and remains intact");
                //?}
                passed.accept("supported bed variants in " + dimension.identifier() + ": wardens, cooldown, preserved bed and no sleep");
            } finally {
                target.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED); removeFixtureEntities(target, new net.minecraft.world.phys.AABB(150, 230, 22, 170, 255, 42)); config.update(previous, config.revision());
            }
        }
        var bucketPlayer = new TestPlayer(server, level, "SumireVerifyBucket"); bucketPlayer.setPos(192.5, 241, 32.5); bucketPlayer.setYRot(0); bucketPlayer.setXRot(20); level.addNewPlayer(bucketPlayer);
        var source = new net.minecraft.core.BlockPos(192, 241, 35); var foot = new net.minecraft.core.BlockPos(192, 240, 32);
        try {
            ServerVerification.place(level, foot, Blocks.STONE.defaultBlockState()); ServerVerification.place(level, source, Blocks.LAVA.defaultBlockState());
            bucketPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
            require(Items.BUCKET.use(level, bucketPlayer, InteractionHand.MAIN_HAND).consumesAction(), "Lava bucket use handled");
            require(bucketPlayer.getMainHandItem().isEmpty() && level.getFluidState(source).isEmpty() && level.getFluidState(foot).getType() == net.minecraft.world.level.material.Fluids.LAVA, "Bucket melts, original source disappears and lava appears below feet");
            ServerVerification.place(level, source, Blocks.WATER.defaultBlockState()); bucketPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
            require(!BlockGameplay.collectLava(bucketPlayer, InteractionHand.MAIN_HAND) && bucketPlayer.getMainHandItem().is(Items.BUCKET), "Water does not melt bucket");
            passed.accept("lava collection melts bucket, moves source below player and leaves water unaffected");
        } finally { level.removePlayerImmediately(bucketPlayer, Entity.RemovalReason.DISCARDED); removeFixtureEntities(level, new net.minecraft.world.phys.AABB(185, 230, 25, 200, 255, 42)); }
    }
    private static int wardens(ServerLevel level) { int count = 0; for (var entity : ServerVerification.spawnedIn(level)) if (entity instanceof net.minecraft.world.entity.monster.warden.Warden && entity.getY() > 230 && entity.getX() > 150 && entity.getX() < 170 && entity.getZ() > 22 && entity.getZ() < 42) count++; return count; }
    private static void cameraAndSheep(MinecraftServer server, ServerLevel level, WorldState state, Consumer<String> passed) throws Exception {
        var config = Sum1reM0d.config(); var previous = config.snapshot();
        var player = new TestPlayer(server, level, "SumireVisualEvents"); player.setPos(220.5, 245, 32.5);
        var data = state.player(player);
        try {
            var draft = config.snapshot(); var features = draft.getAsJsonObject("features");
            for (var feature : features.entrySet()) feature.getValue().getAsJsonObject().addProperty("enabled", false);
            var camera = features.getAsJsonObject("camera_lock"); camera.addProperty("enabled", true); camera.addProperty("interval", 1); camera.addProperty("chance", 100); camera.addProperty("seconds", 5); camera.addProperty("angle", 180);
            config.update(draft, config.revision()); player.setYRot(37); player.setXRot(12);
            Gameplay.tickPlayer(player, state, 20);
            require(data.cameraTicks == 100 && data.originalYaw == 37 && data.originalPitch == 12 && data.cameraYaw == 217 && data.cameraPitch == 12, "Camera event uses configured duration, angle and original orientation");
            player.setYRot(217); Gameplay.tickPlayer(player, state, 40);
            require(data.cameraTicks == 99 && data.originalYaw == 37 && data.cameraYaw == 217, "Overlapping camera interval must not replace the original orientation or restart duration");
            camera.addProperty("chance", 0); config.update(draft, config.revision()); data.cameraTicks = 1; Gameplay.tickPlayer(player, state, 60);
            require(data.cameraTicks == 0, "Zero chance does not retrigger expired camera event");
            data.cameraTicks = 20; camera.addProperty("enabled", false); config.update(draft, config.revision()); Gameplay.tickPlayer(player, state, 61);
            require(data.cameraTicks == 0, "Disabled camera clears server event");
            passed.accept("camera event angle, duration, overlapping intervals, zero chance and disable");
            var red = features.getAsJsonObject("sheep_red"); red.addProperty("enabled", true); red.addProperty("seconds", 3); config.update(draft, config.revision());
            var sheep = net.minecraft.world.entity.EntityTypes.SHEEP.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            require(sheep != null, "Native sheep fixture"); sheep.setPos(player.position());
            require(sheep.hurtServer(level, player.damageSources().playerAttack(player), 1000) && sheep.isDeadOrDying(), "Actual player damage kills sheep");
            require(data.redTicks == 60, "Sheep death hook gives the player three seconds of red screen");
            Gameplay.tickPlayer(player, state, 62); require(data.redTicks == 59, "Red screen counts down on server ticks");
            data.redTicks = 0;
            var cow = net.minecraft.world.entity.EntityTypes.COW.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND); require(cow != null, "Native cow fixture"); cow.setPos(player.position()); cow.hurtServer(level, player.damageSources().playerAttack(player), 1000);
            require(data.redTicks == 0, "Other animal kills do not trigger sheep red screen");
            var natural = net.minecraft.world.entity.EntityTypes.SHEEP.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND); require(natural != null, "Natural death fixture"); natural.setPos(player.position()); natural.hurtServer(level, natural.damageSources().generic(), 1000);
            require(data.redTicks == 0, "Sheep dying without a player source does not select an unrelated player");
            level.addNewPlayer(player);
            var wolf = net.minecraft.world.entity.EntityTypes.WOLF.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND); require(wolf != null, "Tamed wolf fixture"); wolf.tame(player);
            red.addProperty("include_pets", true); red.addProperty("seconds", 5); config.update(draft, config.revision());
            var petSheep = net.minecraft.world.entity.EntityTypes.SHEEP.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND); require(petSheep != null, "Pet kill sheep fixture"); petSheep.setPos(player.position()); petSheep.hurtServer(level, wolf.damageSources().mobAttack(wolf), 1000);
            require(data.redTicks == 100, "Tamed wolf kill applies configured red screen duration to its actual owner"); data.redTicks = 0;
            red.addProperty("include_pets", false); config.update(draft, config.revision());
            var excludedSheep = net.minecraft.world.entity.EntityTypes.SHEEP.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND); require(excludedSheep != null, "Excluded pet kill fixture"); excludedSheep.setPos(player.position()); excludedSheep.hurtServer(level, wolf.damageSources().mobAttack(wolf), 1000);
            require(data.redTicks == 0, "Disabling pet inclusion suppresses owner red screen");
            passed.accept("native tamed wolf sheep kills resolve the owner, apply configured red-screen duration and respect pet exclusion");
            data.redTicks = 60; red.addProperty("enabled", false); config.update(draft, config.revision()); Gameplay.tickPlayer(player, state, 63); require(data.redTicks == 0, "Disabling red screen clears event");
            passed.accept("native sheep death starts red screen, other deaths do not and disabling clears it");
        } finally {
            config.update(previous, config.revision());
            removeFixtureEntities(level, new net.minecraft.world.phys.AABB(215, 235, 25, 226, 255, 40));
            level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
    }
    private static void explosions(MinecraftServer server, ServerLevel level, WorldState state, Consumer<String> passed) throws Exception {
        var config = Sum1reM0d.config(); var previous = config.snapshot();
        var player = new TestPlayer(server, level, "SumireExplosions");
        var position = new net.minecraft.core.BlockPos(256, 241, 48); var data = state.player(player);
        try {
            var draft = config.snapshot(); var features = draft.getAsJsonObject("features");
            for (var feature : features.entrySet()) feature.getValue().getAsJsonObject().addProperty("enabled", false);
            var fire = features.getAsJsonObject("fire_explosion"); fire.addProperty("enabled", true); fire.addProperty("power", 4); fire.addProperty("break_blocks", false); fire.addProperty("cooldown", 10);
            var lava = features.getAsJsonObject("lava_explosion"); lava.addProperty("enabled", true); lava.addProperty("power", 2); lava.addProperty("break_blocks", false); lava.addProperty("cooldown", 7);
            config.update(draft, config.revision());
            ServerVerification.place(level, position.below(), Blocks.STONE.defaultBlockState());
            ServerVerification.place(level, position, Blocks.FIRE.defaultBlockState());
            ServerVerification.place(level, position.north(), Blocks.WOOL.pick(DyeColor.WHITE).defaultBlockState());
            player.setPos(position.getX() + .5, position.getY(), position.getZ() + .5);
            int count = ServerVerification.explosions().size(); Gameplay.tickPlayer(player, state, 1);
            require(ServerVerification.explosions().size() == count + 1 && data.explosionCooldown == 200, "Fire contact creates one native explosion and starts configured cooldown");
            var explosion = ServerVerification.explosions().getLast();
            require(explosion.radius() == 4 && explosion.getDirectSourceEntity() == player && level.getBlockState(position.north()).is(Blocks.WOOL.pick(DyeColor.WHITE)), "Configured power and source apply; terrain remains with breaking disabled");
            Gameplay.tickPlayer(player, state, 2); require(ServerVerification.explosions().size() == count + 1 && data.explosionCooldown == 199, "Continued fire contact cannot retrigger during cooldown");
            ServerVerification.place(level, position, Blocks.AIR.defaultBlockState()); data.explosionCooldown = 0; player.igniteForSeconds(10); Gameplay.tickPlayer(player, state, 3);
            require(ServerVerification.explosions().size() == count + 1, "Burning without fire-block contact does not trigger"); player.clearFire();
            ServerVerification.place(level, position.east().below(), Blocks.STONE.defaultBlockState()); ServerVerification.place(level, position.east(), Blocks.FIRE.defaultBlockState());
            player.setPos(position.getX() + .9, position.getY(), position.getZ() + .5); Gameplay.tickPlayer(player, state, 4);
            require(ServerVerification.explosions().size() == count + 2, "Side of the player's body touching adjacent fire counts as contact");
            fire.addProperty("enabled", false); config.update(draft, config.revision()); data.explosionCooldown = 0; Gameplay.tickPlayer(player, state, 5);
            require(ServerVerification.explosions().size() == count + 2, "Disabled fire explosion does not trigger");
            fire.addProperty("enabled", true); fire.addProperty("break_blocks", true); config.update(draft, config.revision()); Gameplay.tickPlayer(player, state, 6);
            require(ServerVerification.explosions().size() == count + 3 && level.getBlockState(position.north()).isAir(), "Enabled block destruction breaks the nearby wool");
            passed.accept("native fire explosion power, terrain toggle, shared cooldown, side contact and burning-only exclusion");
            ServerVerification.place(level, position.east(), Blocks.AIR.defaultBlockState()); ServerVerification.place(level, position, Blocks.LAVA.defaultBlockState());
            player.setPos(position.getX() + .5, position.getY(), position.getZ() + .5); player.refreshFluid(); require(player.isInLava(), "Native fluid update recognizes lava contact");
            int before = ServerVerification.explosions().size(); Gameplay.tickPlayer(player, state, 7);
            require(ServerVerification.explosions().size() == before, "Fire cooldown also prevents lava explosion");
            data.explosionCooldown = 0; Gameplay.tickPlayer(player, state, 8);
            require(ServerVerification.explosions().size() == before + 1 && ServerVerification.explosions().getLast().radius() == 2 && data.explosionCooldown == 140, "Lava contact uses its own configured power and cooldown");
            lava.addProperty("enabled", false); config.update(draft, config.revision()); data.explosionCooldown = 0; Gameplay.tickPlayer(player, state, 9);
            require(ServerVerification.explosions().size() == before + 1, "Disabled lava explosion does not trigger");
            passed.accept("native lava contact, individual configuration, disable and cooldown shared with fire");
        } finally { config.update(previous, config.revision()); }
    }
    private static void bedrockAndSound(MinecraftServer server, ServerLevel level, WorldState state, Consumer<String> passed) throws Exception {
        var config = Sum1reM0d.config(); var previous = config.snapshot(); var players = ((com.sumirelabs.sum1rem0d.mixin.PlayerListVerificationAccess) server.getPlayerList()).sum1rem0d$verificationPlayers();
        require(players.isEmpty(), "Bedrock verification requires no real connections");
        var actor = new TestPlayer(server, level, "SumireBedrockActor", true); var peer = new TestPlayer(server, level, "SumireBedrockTarget", true);
        var position = new net.minecraft.core.BlockPos(358, 241, 86); actor.setPos(358.5, 241, 86.5); peer.setPos(360.5, 241, 86.5);
        players.add(actor); players.add(peer);
        try {
            var draft = config.snapshot(); var features = draft.getAsJsonObject("features"); for (var feature : features.entrySet()) feature.getValue().getAsJsonObject().addProperty("enabled", false);
            var bedrock = features.getAsJsonObject("bedrock_damage"); bedrock.addProperty("enabled", true); bedrock.addProperty("damage", 3); bedrock.addProperty("cooldown", 1); bedrock.addProperty("include_attacker", false); config.update(draft, config.revision());
            ServerVerification.place(level, position, Blocks.BEDROCK.defaultBlockState()); attackBlock(actor, position);
            require(peer.getHealth() == 17 && actor.getHealth() == 20 && level.getBlockState(position).is(Blocks.BEDROCK), "Actual bedrock attack damages the other player and preserves bedrock");
            peer.damageCooldownTime = 0; attackBlock(actor, position); require(peer.getHealth() == 17, "Bedrock event cooldown prevents a repeated attack even without vanilla damage cooldown");
            state.player(actor).bedrockCooldown = 0; actor.setPos(400, 241, 86.5); attackBlock(actor, position); require(peer.getHealth() == 17 && state.player(actor).bedrockCooldown == 0, "Out-of-reach attacks cannot activate bedrock event"); actor.setPos(358.5, 241, 86.5);
            ServerVerification.place(level, position, Blocks.STONE.defaultBlockState()); attackBlock(actor, position); require(peer.getHealth() == 17, "Other blocks do not activate bedrock damage");
            ServerVerification.place(level, position, Blocks.BEDROCK.defaultBlockState()); players.remove(peer); attackBlock(actor, position); require(actor.getHealth() == 20 && state.player(actor).bedrockCooldown == 0, "Solo player is safe when attacker is excluded");
            bedrock.addProperty("include_attacker", true); config.update(draft, config.revision()); actor.damageCooldownTime = 0; attackBlock(actor, position); require(actor.getHealth() == 17, "Solo player is selected when attacker inclusion is enabled");
            bedrock.addProperty("enabled", false); config.update(draft, config.revision()); state.player(actor).bedrockCooldown = 0; actor.damageCooldownTime = 0; attackBlock(actor, position); require(actor.getHealth() == 17, "Disabled bedrock rule does not damage the attacker");
            passed.accept("native bedrock attack: target selection, configured damage, cooldown, reach, solo inclusion and disable");
            players.add(peer); actor.removeAllEffects(); actor.setHealth(20); actor.setYRot(0); actor.setXRot(0); actor.recorded.clear(); peer.recorded.clear();
            var sound = features.getAsJsonObject("fake_creeper"); sound.addProperty("enabled", true); sound.addProperty("chance", 100); sound.addProperty("interval", 1); sound.addProperty("distance", 3); sound.addProperty("volume", .4); config.update(draft, config.revision());
            Gameplay.tickPlayer(actor, state, 40); var packets = fakeSounds(actor);
            require(packets.size() == 1 && fakeSounds(peer).isEmpty(), "Fake explosion sound is sent only to the selected listener"); var packet = packets.getFirst();
            require(packet.getSource() == net.minecraft.sounds.SoundSource.HOSTILE && packet.getX() == 358.5 && packet.getY() == 241 && packet.getZ() == 83.5 && Math.abs(packet.getVolume() - .4) < 1e-6, "Native sound packet uses configured volume and position behind the player");
            sound.addProperty("distance", 5); config.update(draft, config.revision()); Gameplay.tickPlayer(actor, state, 60); require(fakeSounds(actor).getLast().getZ() == 81.5, "Changing sound distance changes native spatial position");
            sound.addProperty("chance", 0); config.update(draft, config.revision()); Gameplay.tickPlayer(actor, state, 80); require(fakeSounds(actor).size() == 2, "Zero sound chance never triggers");
            sound.addProperty("chance", 100); sound.addProperty("enabled", false); config.update(draft, config.revision()); Gameplay.tickPlayer(actor, state, 100); require(fakeSounds(actor).size() == 2, "Disabled sound event never triggers");
            sound.addProperty("enabled", true); sound.addProperty("interval", 3); config.update(draft, config.revision()); Gameplay.tickPlayer(actor, state, 140); require(fakeSounds(actor).size() == 2, "Sound event waits for configured interval"); Gameplay.tickPlayer(actor, state, 180); require(fakeSounds(actor).size() == 3, "Configured sound interval triggers at its boundary");
            passed.accept("native private creeper sound packet: listener, behind position, volume, distance, interval, chance and disable");
        } finally { players.remove(actor); players.remove(peer); config.update(previous, config.revision()); }
    }
    private static void attackBlock(TestPlayer player, net.minecraft.core.BlockPos pos) { player.gameMode.handleBlockBreakAction(pos, net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, net.minecraft.core.Direction.UP, player.level().getMaxY(), 0); }
    private static java.util.List<net.minecraft.network.protocol.game.ClientboundSoundPacket> fakeSounds(TestPlayer player) {
        return player.recorded.stream().filter(packet -> packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket sound && sound.getSound().equals(net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE)).map(packet -> (net.minecraft.network.protocol.game.ClientboundSoundPacket) packet).toList();
    }
    private static void knockbackMob(MinecraftServer server, ServerLevel level, Consumer<String> passed) throws Exception {
        var config = Sum1reM0d.config(); var previous = config.snapshot(); long clock = level.getOverworldClockTime();
        var player = new TestPlayer(server, level, "SumireKnockback"); var center = new net.minecraft.core.BlockPos(-374, 240, 86); player.setPos(center.getX() + .5, 241, center.getZ() + .5);
        var spawned = new java.util.ArrayList<net.minecraft.world.entity.Mob>();
        try {
            for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) ServerVerification.place(level, center.offset(x, 0, z), Blocks.STONE.defaultBlockState());
            var draft = config.snapshot(); var features = draft.getAsJsonObject("features"); for (var feature : features.entrySet()) feature.getValue().getAsJsonObject().addProperty("enabled", false);
            var options = features.getAsJsonObject("knockback_mob"); options.addProperty("enabled", true); options.addProperty("mob_type", "minecraft:zombie"); options.addProperty("minimum_distance", 2); options.addProperty("maximum_distance", 4); options.addProperty("night_only", true); options.addProperty("world_limit", 1); options.addProperty("knockback", 100); options.addProperty("drop_chance", 0); config.update(draft, config.revision());
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set 6000"); Gameplay.spawnKnockback(server, player);
            require(knockbackFixtures(level).isEmpty(), "Night-only mob does not spawn during the day");
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set 18000"); player.getRandom().setSeed(81); Gameplay.spawnKnockback(server, player);
            spawned.addAll(knockbackFixtures(level)); require(spawned.size() == 1, "One configured mob spawns at night on a clear platform");
            var mob = spawned.getFirst(); var stick = mob.getMainHandItem();
            double distanceSquared = Math.pow(mob.getX() - player.getX(), 2) + Math.pow(mob.getZ() - player.getZ(), 2);
            require(distanceSquared >= 4 && distanceSquared <= 16 && mob.getY() == 241, "Actual negative-coordinate spawn lies within configured horizontal distance and on support");
            require(stick.is(Items.STICK) && net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK), stick) == 100 && mob.getTarget() == player, "Mob holds knockback 100 stick and targets the selected player");
            Gameplay.spawnKnockback(server, player); require(knockbackFixtures(level).size() == 1, "World cap prevents another spawn before visibility activation");
            player.damageCooldownTime = 0; mob.setYRot(0); require(mob.doHurtTarget(level, player), "Native mob melee attack succeeds");
            require(player.getDeltaMovement().horizontalDistance() > 10, "Native melee uses the extreme knockback enchantment");
            var sword = new ItemStack(Items.DIAMOND_SWORD); sword.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), 3); player.setItemInHand(InteractionHand.MAIN_HAND, sword);
            require(mob.hurtServer(level, player.damageSources().playerAttack(player), 1000) && mob.isDeadOrDying(), "Player with Looting III kills the mob");
            require(ServerVerification.spawnedIn(level).stream().noneMatch(entity -> entity instanceof net.minecraft.world.entity.item.ItemEntity item && item.getItem().is(Items.STICK) && entity.getX() < -360), "Zero drop chance does not leak the knockback stick even with Looting III");
            passed.accept("native knockback mob: night gate, distance, cap, enchanted melee and zero-drop policy with Looting");
            options.addProperty("night_only", false); options.addProperty("drop_chance", 100); options.addProperty("knockback", 5); options.addProperty("mob_type", "minecraft:husk"); config.update(draft, config.revision()); server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set 6000"); Gameplay.spawnKnockback(server, player);
            var next = knockbackFixtures(level).stream().filter(Entity::isAlive).findFirst().orElseThrow(); spawned.add(next);
            require(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(next.getType()).toString().equals("minecraft:husk"), "Configured mob type changes the spawned entity");
            require(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK), next.getMainHandItem()) == 5, "Configured enchantment level is applied to later mobs");
            next.hurtServer(level, player.damageSources().playerAttack(player), 1000);
            require(ServerVerification.spawnedIn(level).stream().anyMatch(entity -> entity instanceof net.minecraft.world.entity.item.ItemEntity item && item.getItem().is(Items.STICK) && entity.getX() < -360), "Configured 100 percent equipment drop produces a stick");
            options.addProperty("enabled", false); config.update(draft, config.revision()); int before = knockbackFixtures(level).size(); Gameplay.spawnKnockback(server, player); require(knockbackFixtures(level).size() == before, "Disabled feature does not spawn");
            passed.accept("native knockback mob: daytime override, adjustable enchantment and equipment drop, death releases cap and disable");
            options.addProperty("enabled", true); config.update(draft, config.revision()); player.setGameMode(GameType.CREATIVE); Gameplay.spawnKnockback(server, player);
            require(knockbackFixtures(level).size() == before, "Creative players are not selected for hostile event"); player.setGameMode(GameType.SURVIVAL);
            for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) ServerVerification.place(level, center.offset(x, 0, z), Blocks.WATER.defaultBlockState());
            Gameplay.spawnKnockback(server, player); require(knockbackFixtures(level).size() == before, "Water surface without collision support cannot spawn the mob");
            passed.accept("native knockback mob: configurable species, creative exclusion and unsupported water-surface exclusion");
        } finally {
            spawned.forEach(Entity::discard); knockbackFixtures(level).forEach(Entity::discard);
            removeFixtureEntities(level, new net.minecraft.world.phys.AABB(center).inflate(8));
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set " + clock); level.environmentAttributes().invalidateTickCache(); level.updateSkyBrightness(); config.update(previous, config.revision());
        }
    }
    private static java.util.List<net.minecraft.world.entity.Mob> knockbackFixtures(ServerLevel level) {
        return ServerVerification.spawnedIn(level).stream().filter(entity -> entity instanceof net.minecraft.world.entity.Mob && entity.entityTags().contains("sum1rem0d.knockback") && entity.getX() < -360).map(entity -> (net.minecraft.world.entity.Mob) entity).toList();
    }
    private static void miningAndWeather(MinecraftServer server, ServerLevel level, WorldState state, Consumer<String> passed) throws Exception {
        var config = Sum1reM0d.config(); var previous = config.snapshot();
        float rain = level.getRainLevel(1), thunder = level.getThunderLevel(1);
        var player = new TestPlayer(server, level, "SumireMiningWeather"); var data = state.player(player);
        var position = new net.minecraft.core.BlockPos(294, 241, 70); player.setPos(position.getX() + .5, position.getY(), position.getZ() + 1.5);
        try {
            var draft = config.snapshot(); var features = draft.getAsJsonObject("features");
            for (var feature : features.entrySet()) feature.getValue().getAsJsonObject().addProperty("enabled", false);
            var glass = features.getAsJsonObject("glass_damage"); glass.addProperty("enabled", true); glass.addProperty("damage", 1); glass.addProperty("silk_safe", true);
            config.update(draft, config.revision());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
            ServerVerification.place(level, position, Blocks.GLASS.defaultBlockState());
            require(player.gameMode.destroyBlock(position) && level.getBlockState(position).isAir() && player.getHealth() == 19, "Actual glass mining inflicts the configured half-heart damage");
            var silk = new ItemStack(Items.DIAMOND_PICKAXE); silk.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, silk); player.setHealth(20); player.setInvulnerableTime(0);
            ServerVerification.place(level, position, Blocks.GLASS.defaultBlockState()); require(player.gameMode.destroyBlock(position) && player.getHealth() == 20, "Silk-touch glass mining is safe");
            glass.addProperty("silk_safe", false); glass.addProperty("damage", 3); config.update(draft, config.revision());
            player.damageCooldownTime = 0;
            ServerVerification.place(level, position, Blocks.GLASS_PANE.defaultBlockState()); boolean paneBroken = player.gameMode.destroyBlock(position); require(paneBroken && player.getHealth() == 17, "Disabling silk safety applies configured damage to panes: broken=" + paneBroken + ", health=" + player.getHealth());
            glass.addProperty("enabled", false); config.update(draft, config.revision()); player.setInvulnerableTime(0); player.setHealth(20);
            ServerVerification.place(level, position, Blocks.GLASS.defaultBlockState()); require(player.gameMode.destroyBlock(position) && player.getHealth() == 20, "Disabled glass rule is harmless");
            passed.accept("actual glass and pane mining, configurable damage, silk safety and disable");
            var logs = features.getAsJsonObject("log_loss"); logs.addProperty("enabled", true); logs.addProperty("chance", 100); config.update(draft, config.revision());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));
            removeFixtureEntities(level, new net.minecraft.world.phys.AABB(position).inflate(4));
            ServerVerification.place(level, position, Blocks.OAK_LOG.defaultBlockState()); require(player.gameMode.destroyBlock(position), "Actual log mining succeeds with loss enabled");
            require(ServerVerification.spawnedIn(level).stream().noneMatch(entity -> entity instanceof net.minecraft.world.entity.item.ItemEntity && entity.distanceToSqr(player) < 16), "100 percent log loss removes player-mined drops");
            var environmental = net.minecraft.world.level.block.Block.getDrops(Blocks.OAK_LOG.defaultBlockState(), level, position, null, null, player.getMainHandItem());
            require(environmental.size() == 1 && environmental.getFirst().is(Items.OAK_LOG), "Log loss does not affect non-player drops");
            logs.addProperty("chance", 0); config.update(draft, config.revision());
            ServerVerification.place(level, position, Blocks.OAK_LOG.defaultBlockState()); require(player.gameMode.destroyBlock(position), "Actual log mining succeeds at zero loss");
            require(ServerVerification.spawnedIn(level).stream().anyMatch(entity -> entity instanceof net.minecraft.world.entity.item.ItemEntity item && item.getItem().is(Items.OAK_LOG) && entity.distanceToSqr(player) < 16), "Zero percent log loss keeps actual mined item");
            logs.addProperty("chance", 100); logs.addProperty("enabled", false); config.update(draft, config.revision());
            var disabled = net.minecraft.world.level.block.Block.getDrops(Blocks.OAK_LOG.defaultBlockState(), level, position, null, player, player.getMainHandItem());
            require(disabled.size() == 1 && disabled.getFirst().is(Items.OAK_LOG), "Disabled log loss restores player drops");
            passed.accept("actual log mining loss at 100 and zero percent, non-player drops and disable");
            var headache = features.getAsJsonObject("headache"); headache.addProperty("enabled", true); headache.addProperty("rain", true); headache.addProperty("thunder", true); headache.addProperty("level", 2); config.update(draft, config.revision());
            level.setRainLevel(1); level.setThunderLevel(0); Gameplay.tickPlayer(player, state, 20);
            require(player.hasEffect(ModContent.effect("headache")) && player.getEffect(ModContent.effect("headache")).getAmplifier() == 1, "Rain applies configured headache level");
            headache.addProperty("rain", false); config.update(draft, config.revision()); Gameplay.tickPlayer(player, state, 40); require(!player.hasEffect(ModContent.effect("headache")), "Rain-only headache can be disabled independently");
            level.setThunderLevel(1); Gameplay.tickPlayer(player, state, 60); require(player.hasEffect(ModContent.effect("headache")), "Thunder applies headache when rain trigger is disabled");
            headache.addProperty("enabled", false); config.update(draft, config.revision()); Gameplay.tickPlayer(player, state, 80); require(!player.hasEffect(ModContent.effect("headache")), "Headache disappears when disabled");
            headache.addProperty("enabled", true); config.update(draft, config.revision()); level.setRainLevel(0); level.setThunderLevel(0); Gameplay.tickPlayer(player, state, 100); require(!player.hasEffect(ModContent.effect("headache")), "Clear weather does not apply headache");
            passed.accept("native rain and thunder headache, effect level, independent rain trigger and clear-weather restoration");
            var clean = features.getAsJsonObject("cleanliness"); clean.addProperty("enabled", true); clean.addProperty("decay_per_minute", 0); clean.addProperty("water_per_second", 7); config.update(draft, config.revision());
            ServerVerification.place(level, position, Blocks.WATER.defaultBlockState()); player.setPos(position.getX() + .5, position.getY(), position.getZ() + .5); player.refreshFluid(); require(player.isInWater(), "Native fluid update recognizes water");
            data.cleanliness = 40; Gameplay.tickPlayer(player, state, 120); require(data.cleanliness == 47, "Water restores the configured cleanliness amount");
            data.cleanliness = 99; Gameplay.tickPlayer(player, state, 140); require(data.cleanliness == 100, "Water recovery caps at configured maximum");
            clean.addProperty("enabled", false); config.update(draft, config.revision()); data.cleanliness = 40; Gameplay.tickPlayer(player, state, 160); require(data.cleanliness == 40, "Disabled cleanliness does not recover in water");
            passed.accept("native water contact restores configured cleanliness, caps at maximum and respects disable");
        } finally {
            level.setRainLevel(rain); level.setThunderLevel(thunder); config.update(previous, config.revision());
            removeFixtureEntities(level, new net.minecraft.world.phys.AABB(position).inflate(4));
        }
    }
    private static void removeFixtureEntities(ServerLevel level, net.minecraft.world.phys.AABB bounds) {
        var entities = new java.util.ArrayList<Entity>();
        for (var entity : ServerVerification.spawnedIn(level)) if (bounds.contains(entity.position()) && (entity instanceof net.minecraft.world.entity.monster.warden.Warden || entity instanceof net.minecraft.world.entity.LightningBolt || entity instanceof net.minecraft.world.entity.item.ItemEntity)) entities.add(entity);
        entities.forEach(Entity::discard);
    }
    private static void scheduledKnockback(MinecraftServer server, ServerLevel level, WorldState state, Consumer<String> passed) throws Exception {
        var config = Sum1reM0d.config(); var previous = config.snapshot(); var checkpoint = state.checkpoint();
        long clock = level.getOverworldClockTime(), gameTime = level.getGameTime();
        var players = ((com.sumirelabs.sum1rem0d.mixin.PlayerListVerificationAccess) server.getPlayerList()).sum1rem0d$verificationPlayers(); require(players.isEmpty(), "Knockback scheduling verification requires no real connections");
        var player = new TestPlayer(server, level, "SumireScheduledMob", true); var center = new net.minecraft.core.BlockPos(-450, 240, 140); player.setPos(center.getX() + .5, 241, center.getZ() + .5); players.add(player);
        try {
            for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) ServerVerification.place(level, center.offset(x, 0, z), Blocks.STONE.defaultBlockState());
            var draft = config.snapshot(); var features = draft.getAsJsonObject("features"); for (var feature : features.entrySet()) feature.getValue().getAsJsonObject().addProperty("enabled", false);
            var mob = features.getAsJsonObject("knockback_mob"); mob.addProperty("enabled", true); mob.addProperty("interval", 1); mob.addProperty("chance", 0); mob.addProperty("minimum_distance", 2); mob.addProperty("maximum_distance", 4); mob.addProperty("world_limit", 1); mob.addProperty("night_only", true); config.update(draft, config.revision());
            state.next("mob", 0); gameplayAt(server, level, 18000, 200); require(state.next("mob") == 220 && scheduledMobs(level, player).isEmpty(), "Mob scheduler initializes without an immediate spawn");
            gameplayAt(server, level, 18000, 220); require(scheduledMobs(level, player).isEmpty(), "Zero chance suppresses a due night-time mob draw");
            mob.addProperty("chance", 100); config.update(draft, config.revision()); gameplayAt(server, level, 6000, 240); require(scheduledMobs(level, player).isEmpty(), "Night-only setting suppresses a successful daytime draw");
            player.getRandom().setSeed(81); gameplayAt(server, level, 18000, 260); require(scheduledMobs(level, player).size() == 1, "Actual scheduled draw spawns a mob at night targeting the eligible player");
            gameplayAt(server, level, 18000, 280); require(scheduledMobs(level, player).size() == 1, "World cap also applies to subsequent scheduled draws"); scheduledMobs(level, player).forEach(Entity::discard);
            mob.addProperty("enabled", false); config.update(draft, config.revision()); gameplayAt(server, level, 18000, 300); require(scheduledMobs(level, player).isEmpty(), "Disabled scheduling does not spawn a replacement");
            mob.addProperty("enabled", true); config.update(draft, config.revision()); player.setGameMode(GameType.CREATIVE); gameplayAt(server, level, 18000, 320); require(scheduledMobs(level, player).isEmpty(), "Scheduler excludes creative players before choosing a target");
            player.setGameMode(GameType.SURVIVAL); mob.addProperty("night_only", false); config.update(draft, config.revision()); player.getRandom().setSeed(81); gameplayAt(server, level, 6000, 340); require(scheduledMobs(level, player).size() == 1, "Daytime override applies through the actual scheduled spawn path");
            passed.accept("actual mob scheduler honors initial interval, chance, night gate, cap, disable, creative exclusion and daytime override");
        } finally {
            scheduledMobs(level, player).forEach(Entity::discard); players.remove(player); server.getWorldData().overworldData().setGameTime(gameTime); server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set " + clock); level.environmentAttributes().invalidateTickCache(); level.updateSkyBrightness(); state.restore(checkpoint); config.update(previous, config.revision());
        }
    }
    private static java.util.List<net.minecraft.world.entity.Mob> scheduledMobs(ServerLevel level, ServerPlayer player) {
        return ServerVerification.spawnedIn(level).stream().filter(entity -> entity instanceof net.minecraft.world.entity.Mob mob && mob.isAlive() && mob.entityTags().contains("sum1rem0d.knockback") && mob.getTarget() == player).map(entity -> (net.minecraft.world.entity.Mob) entity).toList();
    }
    private static void dailyPersistence(MinecraftServer server, ServerLevel level, WorldState state, Consumer<String> passed) throws Exception {
        var config = Sum1reM0d.config(); var previous = config.snapshot(); var checkpoint = state.checkpoint();
        long clock = level.getOverworldClockTime(), gameTime = level.getGameTime();
        var players = ((com.sumirelabs.sum1rem0d.mixin.PlayerListVerificationAccess) server.getPlayerList()).sum1rem0d$verificationPlayers(); require(players.isEmpty(), "Daily persistence verification requires no real connections");
        var player = new TestPlayer(server, level, "SumireDailyPersistence", true); players.add(player);
        var daily = java.util.List.of("vegan", "upside_down_grass", "diamond_disguise", "hardness_swap");
        try {
            var draft = config.snapshot(); var features = draft.getAsJsonObject("features"); for (var feature : features.entrySet()) feature.getValue().getAsJsonObject().addProperty("enabled", false);
            for (String feature : daily) { var options = features.getAsJsonObject(feature); options.addProperty("enabled", true); options.addProperty("chance", 100); options.addProperty("announce", true); }
            var fuel = features.getAsJsonObject("daily_fuel"); fuel.addProperty("enabled", true); fuel.addProperty("minimum", .2); fuel.addProperty("maximum", .2); config.update(draft, config.revision());
            var initial = state.checkpoint(); initial.addProperty("day", Long.MIN_VALUE); state.restore(initial);
            gameplayAt(server, level, 23999, 20);
            require(state.day() == 0 && daily.stream().allMatch(state::daily) && state.fuelEfficiency() == .2, "Actual daily update enables all four events at one hundred percent and samples the configured fixed fuel range");
            require(dailyAnnouncementCount(player) == 1, "New day broadcasts one combined daily announcement");
            int permutation = state.furnacePermutation(); require(permutation >= 0 && permutation < 6, "Daily furnace appearance is a valid permutation");
            for (String feature : daily) features.getAsJsonObject(feature).addProperty("chance", 0);
            fuel.addProperty("minimum", 1.8); fuel.addProperty("maximum", 1.8); config.update(draft, config.revision());
            gameplayAt(server, level, 23999, 21);
            require(!state.updateDay() && daily.stream().allMatch(state::daily) && state.fuelEfficiency() == .2 && state.furnacePermutation() == permutation && dailyAnnouncementCount(player) == 1, "Same-day Config changes do not reroll events, fuel, appearance or announcements");
            for (String feature : daily) features.getAsJsonObject(feature).addProperty("enabled", false); fuel.addProperty("enabled", false); config.update(draft, config.revision());
            require(daily.stream().noneMatch(state::daily) && state.fuelEfficiency() == 1, "Disabled daily features stop applying while their saved draw remains intact");
            for (String feature : daily) features.getAsJsonObject(feature).addProperty("enabled", true); fuel.addProperty("enabled", true); config.update(draft, config.revision());
            require(daily.stream().allMatch(state::daily) && state.fuelEfficiency() == .2, "Reenable restores the saved same-day draw without using new probabilities");
            var data = state.player(player); data.awakeTicks = 4 * 24000L; data.sleepWarningStage = 2; state.save();
            var loaded = new WorldState(server);
            require(loaded.day() == 0 && daily.stream().allMatch(loaded::daily) && loaded.fuelEfficiency() == .2 && loaded.furnacePermutation() == permutation, "Independent world-state construction reads actual disk values for day, events, fuel and appearance");
            require(!loaded.updateDay() && loaded.player(player).awakeTicks == 4 * 24000L && loaded.player(player).sleepWarningStage == 2, "Reload does not reroll the current day and preserves the per-player sleep warning stage");
            passed.accept("actual daily draws, combined announcement, same-day Config changes and disable/reenable preserve saved outcomes; native disk reload preserves fuel, appearance and sleep stage");
            gameplayAt(server, level, 24000, 40);
            require(state.day() == 1 && daily.stream().noneMatch(state::daily) && state.fuelEfficiency() == 1.8, "Exact next-day boundary applies new zero-percent probabilities and fixed fuel range");
            require(dailyAnnouncementCount(player) == 1 && !state.updateDay(), "Inactive next day emits no event announcement and repeated updates do not reroll");
            var nextLoaded = new WorldState(server); require(nextLoaded.day() == 1 && daily.stream().noneMatch(nextLoaded::daily) && nextLoaded.fuelEfficiency() == 1.8 && !nextLoaded.updateDay(), "Next-day outcomes are saved and survive another independent disk reload");
            passed.accept("exact day boundary applies new daily probabilities and fuel limits once, saves them and does not announce inactive events");
        } finally {
            players.remove(player); server.getWorldData().overworldData().setGameTime(gameTime); server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set " + clock); level.environmentAttributes().invalidateTickCache(); level.updateSkyBrightness(); state.restore(checkpoint); state.save(); config.update(previous, config.revision());
        }
    }
    private static long dailyAnnouncementCount(TestPlayer player) {
        return player.recorded.stream().filter(packet -> packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket chat && chat.content().getString().startsWith("今日の異変:")).count();
    }
    private static void sleepWarnings(MinecraftServer server, ServerLevel level, WorldState state, Consumer<String> passed) throws Exception {
        var config = Sum1reM0d.config(); var previous = config.snapshot();
        var player = new TestPlayer(server, level, "SumireSleepWarnings", true); var data = state.player(player);
        try {
            var draft = config.snapshot(); var features = draft.getAsJsonObject("features"); for (var feature : features.entrySet()) feature.getValue().getAsJsonObject().addProperty("enabled", false);
            var sleep = features.getAsJsonObject("sleep_deprivation"); sleep.addProperty("enabled", true); sleep.addProperty("warnings", true); sleep.addProperty("hunger_days", 3); sleep.addProperty("slow_days", 4); sleep.addProperty("death_days", 5); sleep.addProperty("death_enabled", false); config.update(draft, config.revision());
            data.awakeTicks = 3 * 24000L; Gameplay.tickPlayer(player, state, 20); require(sleepWarningCount(player) == 1 && data.sleepWarningStage == 1, "Hunger threshold emits one warning");
            Gameplay.tickPlayer(player, state, 40); require(sleepWarningCount(player) == 1, "The same sleep stage does not repeat its warning every second");
            data.awakeTicks = 4 * 24000L; Gameplay.tickPlayer(player, state, 60); require(sleepWarningCount(player) == 2 && data.sleepWarningStage == 2, "Slowness threshold emits its own warning");
            data.awakeTicks = 0; Gameplay.tickPlayer(player, state, 80); require(data.sleepWarningStage == 0, "Restored awake time resets the warning stage");
            sleep.addProperty("warnings", false); config.update(draft, config.revision()); data.awakeTicks = 3 * 24000L; Gameplay.tickPlayer(player, state, 100); require(sleepWarningCount(player) == 2 && data.sleepWarningStage == 1, "Warning toggle suppresses notifications while still tracking the stage");
            sleep.addProperty("warnings", true); config.update(draft, config.revision()); Gameplay.tickPlayer(player, state, 120); require(sleepWarningCount(player) == 2, "Reenabling warnings does not replay the current stage");
            data.awakeTicks = 4 * 24000L; Gameplay.tickPlayer(player, state, 140); require(sleepWarningCount(player) == 3, "Later stages still warn after reenabling");
            data.awakeTicks = 0; Gameplay.tickPlayer(player, state, 160); data.awakeTicks = 3 * 24000L; Gameplay.tickPlayer(player, state, 180); require(sleepWarningCount(player) == 4, "A new sleep-deprivation cycle can warn again");
            sleep.addProperty("death_enabled", true); config.update(draft, config.revision()); data.awakeTicks = 5 * 24000L; Gameplay.tickPlayer(player, state, 200); require(sleepWarningCount(player) == 5 && !player.isAlive(), "Final threshold warns and then kills");
            Gameplay.tickPlayer(player, state, 201); require(data.awakeTicks == 0 && data.sleepWarningStage == 0, "Death reset also resets the warning cycle");
            passed.accept("sleep stages warn once at hunger, slowness and death, respect notification toggle and reset after rest or death");
        } finally { config.update(previous, config.revision()); }
    }
    private static long sleepWarningCount(TestPlayer player) {
        return player.recorded.stream().filter(packet -> packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket chat && chat.content().getString().startsWith("睡眠不足:")).count();
    }
    private static void scheduledZeus(MinecraftServer server, ServerLevel level, WorldState state, Consumer<String> passed) throws Exception {
        var config = Sum1reM0d.config(); var previous = config.snapshot(); var checkpoint = state.checkpoint();
        long clock = level.getOverworldClockTime(), gameTime = level.getGameTime();
        var players = ((com.sumirelabs.sum1rem0d.mixin.PlayerListVerificationAccess) server.getPlayerList()).sum1rem0d$verificationPlayers(); require(players.isEmpty(), "Zeus scheduling verification requires no real connections");
        var actor = new TestPlayer(server, level, "SumireZeusSchedule", true); var creative = new TestPlayer(server, level, "SumireZeusCreative", true);
        actor.setPos(300.5, 245, 32.5); creative.setGameMode(GameType.CREATIVE); players.add(actor); players.add(creative);
        var roof = actor.blockPosition().above(2);
        try {
            var draft = config.snapshot(); var features = draft.getAsJsonObject("features"); for (var feature : features.entrySet()) feature.getValue().getAsJsonObject().addProperty("enabled", false);
            var zeus = features.getAsJsonObject("zeus"); zeus.addProperty("enabled", true); zeus.addProperty("interval", 1); zeus.addProperty("chance", 0); zeus.addProperty("warning", true); zeus.addProperty("warning_seconds", 2); config.update(draft, config.revision());
            state.next("zeus", 0); gameplayAt(server, level, 6000, 200); require(state.next("zeus") == 220 && state.player(actor).lightningTicks == 0, "Zeus initializes its timer without immediate lightning");
            gameplayAt(server, level, 6000, 220); require(state.player(actor).lightningTicks == 0, "Zero percent Zeus chance suppresses a due event");
            zeus.addProperty("chance", 100); config.update(draft, config.revision()); ServerVerification.place(level, roof, Blocks.STONE.defaultBlockState());
            gameplayAt(server, level, 6000, 240); require(state.player(actor).lightningTicks == 0 && state.player(creative).lightningTicks == 0, "A roof makes the selected eligible player immune without targeting creative players"); ServerVerification.place(level, roof, Blocks.AIR.defaultBlockState());
            gameplayAt(server, level, 6000, 260); var data = state.player(actor); require(data.lightningTicks == 41 && state.player(creative).lightningTicks == 0, "Due Zeus event chooses the eligible player and starts the configured warning");
            long warnings = actor.recorded.stream().filter(packet -> packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket chat && chat.content().getString().contains("ゼウス")).count();
            require(warnings == 1 && actor.recorded.stream().anyMatch(packet -> packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket), "Zeus sends both its warning message and sound");
            passed.accept("actual Zeus scheduler honors interval, zero chance, roofs and creative exclusion and sends warning message and sound");
            for (int tick = 261; tick < 280; tick++) Gameplay.tickPlayer(actor, state, tick);
            gameplayAt(server, level, 6000, 280); require(data.lightningTicks == 21, "An overlapping Zeus draw must not restart the existing warning countdown");
            zeus.addProperty("enabled", false); config.update(draft, config.revision()); Gameplay.tickPlayer(actor, state, 281); require(data.lightningTicks == 0 && actor.isAlive(), "Disabling Zeus immediately cancels a pending warning");
            zeus.addProperty("enabled", true); zeus.addProperty("warning", false); config.update(draft, config.revision()); Gameplay.tickPlayer(actor, state, 282); require(data.lightningTicks == 0 && actor.isAlive(), "Reenable does not revive a cancelled strike");
            actor.recorded.clear(); gameplayAt(server, level, 6000, 300); require(data.lightningTicks == 1 && actor.recorded.stream().noneMatch(packet -> packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket || packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket), "Warning-off schedules next-tick lightning without warning packets");
            Gameplay.tickPlayer(actor, state, 301); require(!actor.isAlive() && creative.isAlive(), "Scheduled lightning kills only its eligible target");
            passed.accept("Zeus overlapping intervals preserve warning duration, disable cancels pending lightning and warning-off strikes without warning packets");
        } finally {
            players.remove(actor); players.remove(creative);
            server.getWorldData().overworldData().setGameTime(gameTime); server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set " + clock); level.environmentAttributes().invalidateTickCache(); level.updateSkyBrightness(); state.restore(checkpoint); config.update(previous, config.revision());
        }
    }
    private static void periodicExchanges(MinecraftServer server, ServerLevel level, WorldState state, Consumer<String> passed) throws Exception {
        var config = Sum1reM0d.config(); var previous = config.snapshot(); var checkpoint = state.checkpoint();
        long clock = level.getOverworldClockTime(), gameTime = level.getGameTime();
        var players = ((com.sumirelabs.sum1rem0d.mixin.PlayerListVerificationAccess) server.getPlayerList()).sum1rem0d$verificationPlayers(); require(players.isEmpty(), "Periodic exchange verification requires no real connections");
        var a = new TestPlayer(server, level, "SumireMoonA", true); var b = new TestPlayer(server, level, "SumireMoonB", true);
        var remote = new TestPlayer(server, server.getLevel(net.minecraft.world.level.Level.NETHER), "SumireMoonRemote", true);
        players.add(a); players.add(b); a.setPos(0.5, 245, .5); b.setPos(2.5, 245, .5);
        try {
            var draft = config.snapshot(); var features = draft.getAsJsonObject("features"); for (var feature : features.entrySet()) feature.getValue().getAsJsonObject().addProperty("enabled", false);
            var swap = features.getAsJsonObject("inventory_swap"); swap.addProperty("enabled", true); swap.addProperty("moon_phase", 0); swap.addProperty("same_dimension", true); swap.addProperty("start_slot", 18); swap.addProperty("slots", 9); config.update(draft, config.revision());
            for (int slot = 18; slot < 27; slot++) { a.getInventory().setItem(slot, new ItemStack(Items.DIAMOND)); b.getInventory().setItem(slot, new ItemStack(Items.EMERALD)); remote.getInventory().setItem(slot, new ItemStack(Items.GOLD_INGOT)); }
            state.exchangedDay(Long.MIN_VALUE); gameplayAt(server, level, 6000, 20);
            require(a.getInventory().getItem(18).is(Items.DIAMOND) && state.exchangedDay() == Long.MIN_VALUE, "Full moon daytime does not exchange inventory");
            gameplayAt(server, level, 13000, 21); require(a.getInventory().getItem(18).is(Items.DIAMOND), "Exchange waits for the once-per-second event check");
            gameplayAt(server, level, 13000, 40); require(a.getInventory().getItem(18).is(Items.EMERALD) && b.getInventory().getItem(18).is(Items.DIAMOND) && state.exchangedDay() == 0, "Full moon night triggers actual scheduled inventory exchange");
            gameplayAt(server, level, 14000, 60); require(a.getInventory().getItem(18).is(Items.EMERALD), "Same full moon night cannot exchange twice");
            gameplayAt(server, level, 37000, 80); require(a.getInventory().getItem(18).is(Items.EMERALD) && state.exchangedDay() == 0, "Other moon phases cannot exchange inventory");
            gameplayAt(server, level, 205000, 100); require(a.getInventory().getItem(18).is(Items.DIAMOND) && b.getInventory().getItem(18).is(Items.EMERALD) && state.exchangedDay() == 8, "Next full moon night grants a new exchange");
            players.remove(b); players.add(remote); gameplayAt(server, level, 397000, 120); require(a.getInventory().getItem(18).is(Items.DIAMOND) && remote.getInventory().getItem(18).is(Items.GOLD_INGOT), "Same-dimension setting prevents cross-world exchange");
            swap.addProperty("same_dimension", false); config.update(draft, config.revision()); gameplayAt(server, level, 589000, 140); require(a.getInventory().getItem(18).is(Items.GOLD_INGOT) && remote.getInventory().getItem(18).is(Items.DIAMOND), "Config permits cross-world pairing on a later full moon");
            swap.addProperty("enabled", false); config.update(draft, config.revision()); gameplayAt(server, level, 781000, 160); require(a.getInventory().getItem(18).is(Items.GOLD_INGOT), "Disabled full moon exchange does not trigger");
            passed.accept("actual gameplay tick dispatches full moon exchange only at night, once per night, across configured dimensions and respects disable");
            players.remove(remote); players.add(b); var names = features.getAsJsonObject("name_swap"); names.addProperty("enabled", true); names.addProperty("interval", 1); names.addProperty("chat", true); config.update(draft, config.revision());
            state.aliases().clear(); state.next("names", 0); var aId = a.getUUID(); var bId = b.getUUID();
            gameplayAt(server, level, 6000, 200); require(state.aliases().isEmpty() && state.next("names") == 220, "Name schedule initializes without an immediate swap");
            gameplayAt(server, level, 6000, 219); require(state.aliases().isEmpty(), "Name event does not fire before its interval");
            gameplayAt(server, level, 6000, 220); require(a.getDisplayName().getString().equals(b.getGameProfile().name()) && state.next("names") == 240, "Scheduled name event swaps current players at interval boundary");
            gameplayAt(server, level, 6000, 240); require(a.getDisplayName().getString().equals(a.getGameProfile().name()), "Second scheduled name event exchanges current aliases again");
            b.setGameMode(GameType.CREATIVE); gameplayAt(server, level, 6000, 260); require(state.aliases().isEmpty(), "Creative exclusion leaves a lone eligible player with their real name"); b.setGameMode(GameType.SURVIVAL);
            state.aliases().put(aId.toString(), "TemporaryAlias"); names.addProperty("enabled", false); config.update(draft, config.revision()); gameplayAt(server, level, 6000, 280); require(state.aliases().isEmpty() && a.getUUID().equals(aId) && b.getUUID().equals(bId), "Disabling scheduled names clears aliases without changing identities");
            passed.accept("actual gameplay tick dispatches periodic names at configured boundaries, repeats exchanges, excludes creative players and clears aliases on disable");
        } finally {
            players.remove(a); players.remove(b); players.remove(remote);
            server.getWorldData().overworldData().setGameTime(gameTime); server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set " + clock); level.environmentAttributes().invalidateTickCache(); level.updateSkyBrightness(); state.restore(checkpoint); config.update(previous, config.revision());
        }
    }
    private static void gameplayAt(MinecraftServer server, ServerLevel level, long clock, long gameTime) {
        server.getWorldData().overworldData().setGameTime(gameTime); server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set " + clock); level.environmentAttributes().invalidateTickCache(); level.updateSkyBrightness(); Gameplay.tick(server);
    }
    private static void exchange(MinecraftServer server, ServerLevel level, WorldState state, TestPlayer a, TestPlayer b) throws Exception {
        var c = new TestPlayer(server, level, "SumireVerifyC"); var config = Sum1reM0d.config(); var previous = config.snapshot();
        var aliases = new java.util.LinkedHashMap<>(state.aliases()); var aItems = new java.util.ArrayList<ItemStack>(); var bItems = new java.util.ArrayList<ItemStack>();
        for (int slot = 0; slot < a.getInventory().getContainerSize(); slot++) aItems.add(a.getInventory().getItem(slot).copy());
        for (int slot = 0; slot < b.getInventory().getContainerSize(); slot++) bItems.add(b.getInventory().getItem(slot).copy());
        try {
            var draft = config.snapshot(); var features = draft.getAsJsonObject("features"); features.getAsJsonObject("inventory_swap").addProperty("start_slot", 18); features.getAsJsonObject("inventory_swap").addProperty("slots", 9); features.getAsJsonObject("name_swap").addProperty("enabled", true); features.getAsJsonObject("name_swap").addProperty("chat", true); config.update(draft, config.revision());
            var players = new java.util.ArrayList<ServerPlayer>(java.util.List.of(a, b, c));
            for (int index = 0; index < 3; index++) { var inventory = players.get(index).getInventory(); inventory.setItem(0, new ItemStack(Items.EMERALD, index + 1)); for (int slot = 18; slot < 27; slot++) inventory.setItem(slot, new ItemStack(Items.DIAMOND, index + 1)); }
            Gameplay.exchangeRows(new java.util.ArrayList<>(players), new java.util.Random(42));
            int unchanged = 0, total = 0; var rows = new java.util.HashSet<Integer>();
            for (int index = 0; index < 3; index++) { var inventory = players.get(index).getInventory(); int row = inventory.getItem(18).getCount(); rows.add(row); if (row == index + 1) unchanged++; require(inventory.getItem(0).is(Items.EMERALD) && inventory.getItem(0).getCount() == index + 1, "Hotbar stays with original owner"); for (int slot = 18; slot < 27; slot++) { require(inventory.getItem(slot).is(Items.DIAMOND) && inventory.getItem(slot).getCount() == row, "Nine slots move as a complete row"); total += inventory.getItem(slot).getCount(); } }
            require(unchanged == 1 && total == 54 && rows.size() == 3, "Exactly one odd player is unchanged and no row is duplicated or lost");
            Gameplay.shuffleNames(new java.util.ArrayList<>(players), state, new java.util.Random(42));
            var names = new java.util.HashSet<String>();
            for (var player : players) { String alias = state.aliases().get(player.getUUID().toString()); require(alias != null && !alias.equals(player.getGameProfile().name()), "Name shuffle has no fixed points"); names.add(alias); require(player.getDisplayName().getString().equals(alias), "Chat displays assigned alias"); require(player.getDisplayName().getStyle().getHoverEvent() != null && player.getDisplayName().getStyle().getInsertion().equals(player.getGameProfile().name()), "Original name remains available in rich display style"); }
            require(names.size() == 3, "Name shuffle is a permutation");
            state.aliases().clear();
            Gameplay.shuffleNames(new java.util.ArrayList<>(java.util.List.of(a, b)), state, new java.util.Random(42));
            require(a.getDisplayName().getString().equals(b.getGameProfile().name()), "First two-player event exchanges names");
            Gameplay.shuffleNames(new java.util.ArrayList<>(java.util.List.of(a, b)), state, new java.util.Random(42));
            require(a.getDisplayName().getString().equals(a.getGameProfile().name()) && b.getDisplayName().getString().equals(b.getGameProfile().name()), "Next two-player event exchanges current aliases instead of leaving names stuck");
        } finally {
            for (int slot = 0; slot < aItems.size(); slot++) a.getInventory().setItem(slot, aItems.get(slot));
            for (int slot = 0; slot < bItems.size(); slot++) b.getInventory().setItem(slot, bItems.get(slot));
            state.aliases().clear(); state.aliases().putAll(aliases); config.update(previous, config.revision());
        }
    }
    static final class TestPlayer extends ServerPlayer {
        final java.util.List<net.minecraft.network.protocol.Packet<?>> recorded = new java.util.ArrayList<>();
        TestPlayer(MinecraftServer server, ServerLevel level, String name) {
            this(server, level, name, false);
        }
        TestPlayer(MinecraftServer server, ServerLevel level, String name, boolean record) {
            super(server, level, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)), name), ClientInformation.createDefault());
            connection = record ? new RecordingListener(server, this) : new ServerGamePacketListenerImpl(server, new net.minecraft.network.Connection(PacketFlow.SERVERBOUND), this, CommonListenerCookie.createInitial(getGameProfile(), false));
            setGameMode(GameType.SURVIVAL);
        }
        int fall(double distance) { return calculateFallDamage(distance, 1); }
        void refreshFluid() { baseTick(); }
        @Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
    }
    private static final class RecordingListener extends ServerGamePacketListenerImpl {
        private final TestPlayer target;
        RecordingListener(MinecraftServer server, TestPlayer player) { super(server, new net.minecraft.network.Connection(PacketFlow.SERVERBOUND), player, CommonListenerCookie.createInitial(player.getGameProfile(), false)); target = player; }
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { target.recorded.add(packet); }
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet, io.netty.channel.ChannelFutureListener listener) { target.recorded.add(packet); }
    }
}
