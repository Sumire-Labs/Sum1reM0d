package com.sumirelabs.sum1rem0d.game;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.network.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public final class Gameplay {
    private static final Identifier CLEAN_HEALTH = ModContent.id("cleanliness_health");
    private static final ThreadLocal<Boolean> APPLYING_DAMAGE_EFFECT = ThreadLocal.withInitial(() -> false);
    private Gameplay() {}
    public static boolean eligible(ServerPlayer player) { return player.isAlive() && !player.isCreative() && !player.isSpectator(); }
    public static void tick(MinecraftServer server) {
        if (!com.sumirelabs.sum1rem0d.Sum1reM0d.initialized()) return;
        var state = WorldState.get(server);
        boolean newDay = state.updateDay();
        RedstoneRefresh.tick(server);
        Economy.tick(server);
        CoinServicesMultiplayerVerification.tick(server);
        EconomyMultiplayerVerification.tick(server);
        long gameTime = server.overworld().getGameTime();
        if (newDay) {
            List<String> active = new ArrayList<>();
            for (String f : DailyEvents.FEATURES)
                if (state.daily(f) && Rules.b("daily_events", "announce") && (!List.of("vegan", "upside_down_grass", "diamond_disguise", "hardness_swap").contains(f) || Rules.b(f, "announce"))) active.add(DailyEvents.label(f));
            if (!active.isEmpty()) server.getPlayerList().broadcastSystemMessage(Component.literal("今日の異変: " + String.join(" / ", active)), false);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) tickPlayer(player, state, gameTime);
        if (gameTime % 20 == 0) {
            if (state.daily("inventory_swap") && state.exchangedDay() != state.day()
                && server.getPlayerList().getPlayers().stream().filter(Gameplay::eligible).count() >= 2) exchangeInventories(server, state);
            scheduled(server, state, gameTime);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) sendState(player);
        }
        if (gameTime % 600 == 0) state.save();
    }
    static void tickPlayer(ServerPlayer player, WorldState state, long gameTime) {
        var p = state.player(player);
        long clock = player.level().getOverworldClockTime();
        boolean moved = p.lastX != player.getX() || p.lastY != player.getY() || p.lastZ != player.getZ()
            || p.lastYaw != player.getYRot() || p.lastPitch != player.getXRot() || p.lastAction != player.getLastActionTime();
        p.afkTicks = moved ? 0 : Math.min(Integer.MAX_VALUE - 1, p.afkTicks + 1);
        p.lastX = player.getX(); p.lastY = player.getY(); p.lastZ = player.getZ();
        p.lastYaw = player.getYRot(); p.lastPitch = player.getXRot(); p.lastAction = player.getLastActionTime();
        if (!player.isAlive() && p.wasAlive && Rules.b("sleep_deprivation", "death_resets")) { p.awakeTicks = 0; p.sleepTicks = 0; p.sleepWarningStage = 0; }
        p.wasAlive = player.isAlive();
        if (eligible(player)) {
            if (p.lastClock != Long.MIN_VALUE) p.awakeTicks += Math.max(0, Math.min(24000L * 365, clock - p.lastClock));
            if (player.isSleeping() && canRest(player)) {
                p.sleepTicks++;
                completeRest(player);
            } else { p.sleepTicks = 0; p.sleepAwarded = false; }
        }
        p.lastClock = clock;
        p.influenzaTicks = Math.max(0, p.influenzaTicks - 1);
        p.environmentTicks = Math.max(0, p.environmentTicks - 1);
        p.angerTicks = Math.max(0, p.angerTicks - 1);
        p.explosionCooldown = Math.max(0, p.explosionCooldown - 1);
        p.damageCooldown = Math.max(0, p.damageCooldown - 1);
        p.bedrockCooldown = Math.max(0, p.bedrockCooldown - 1);
        p.bedCooldown = Math.max(0, p.bedCooldown - 1);
        p.redTicks = Rules.on("sheep_red") ? Math.max(0, p.redTicks - 1) : 0;
        p.cameraTicks = Rules.on("camera_lock") ? Math.max(0, p.cameraTicks - 1) : 0;
        if (!Rules.on("zeus")) p.lightningTicks = 0;
        else if (p.lightningTicks > 0 && --p.lightningTicks == 0 && eligible(player)) strike(player);
        if (!eligible(player)) { if (gameTime % 20 == 0) cleanup(player); return; }
        if (p.explosionCooldown == 0) {
            String trigger = player.isInLava() && Rules.on("lava_explosion") ? "lava_explosion" :
                Rules.on("fire_explosion") && BlockPos.betweenClosedStream(player.getBoundingBox().deflate(1.0E-7))
                    .anyMatch(pos -> Rules.matches(player.level().getBlockState(pos), Rules.list("fire_explosion", "target_blocks"))) ? "fire_explosion" : null;
            if (trigger != null) {
                p.explosionCooldown = Rules.i(trigger, "cooldown") * 20;
                player.level().explode(player, player.getX(), player.getY(), player.getZ(), (float) Rules.n(trigger, "power"), Rules.b(trigger, "break_blocks") ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
            }
        }
        if (gameTime % 20 != 0) return;
        maintain(player, "vegan", state.daily("vegan"), 40, 0);
        boolean rainy = player.level().isRaining() && Rules.b("headache", "rain") || player.level().isThundering() && Rules.b("headache", "thunder");
        maintain(player, "headache", Rules.on("headache") && rainy, 40, Rules.i("headache", "level") - 1);
        if (Rules.on("influenza") && p.influenzaTicks == 0) {
            double radius = Rules.n("influenza", "radius");
            long close = player.level().players().stream().filter(Gameplay::eligible).filter(other -> player.distanceToSqr(other) <= radius * radius).count();
            p.exposureTicks = close >= Rules.i("influenza", "players") ? p.exposureTicks + 20 : 0;
            if (p.exposureTicks >= Rules.i("influenza", "exposure") * 20) { p.influenzaTicks = Rules.i("influenza", "duration") * 20; p.exposureTicks = 0; }
        }
        maintain(player, "influenza", Rules.on("influenza") && p.influenzaTicks > 0, Math.max(1, p.influenzaTicks), Rules.i("influenza", "level") - 1);
        maintain(player, "environment_group", Rules.on("environment_group") && p.environmentTicks > 0, Math.max(1, p.environmentTicks), 0);
        maintain(player, "environment_anger", Rules.on("environment_anger") && p.angerTicks > 0, Math.max(1, p.angerTicks), Rules.i("environment_anger", "level") - 1);
        long days = p.awakeTicks / 24000;
        boolean tired = Rules.on("sleep_deprivation");
        boolean hungry = tired && Rules.b("sleep_deprivation", "hunger_enabled") && days >= Rules.i("sleep_deprivation", "hunger_days");
        boolean slow = tired && Rules.b("sleep_deprivation", "slow_enabled") && days >= Rules.i("sleep_deprivation", "slow_days");
        boolean fatal = tired && Rules.b("sleep_deprivation", "death_enabled") && days >= Rules.i("sleep_deprivation", "death_days");
        int stage = fatal ? 3 : slow ? 2 : hungry ? 1 : 0;
        if (stage > p.sleepWarningStage && Rules.b("sleep_deprivation", "warnings")) player.sendSystemMessage(Component.literal(
                "睡眠不足: " + days + "日間眠っていません。" + switch (stage) {
                    case 1 -> "空腹が止まりません。ベッドで休んでください。";
                    case 2 -> "身体が重く、動きが鈍くなっています。早く休んでください。";
                    default -> "身体が限界に達しました。";
                }));
        p.sleepWarningStage = stage;
        maintain(player, "sleep_hunger", hungry, 40, Rules.i("sleep_deprivation", "hunger_level") - 1);
        maintain(player, "sleep_slowness", slow, 40, Rules.i("sleep_deprivation", "slow_level") - 1);
        if (fatal) player.kill(player.level());
        var health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            if (Rules.on("cleanliness")) {
                p.cleanliness = Math.max(0, Math.min(Rules.n("cleanliness", "maximum"), p.cleanliness - Rules.n("cleanliness", "decay_per_minute") / 60));
                if (player.isInWater()) p.cleanliness = Math.min(Rules.n("cleanliness", "maximum"), p.cleanliness + Rules.n("cleanliness", "water_per_second"));
                double threshold = Rules.n("cleanliness", "health_threshold");
                double lost = p.cleanliness >= threshold ? 0 : Math.ceil((threshold - p.cleanliness) / Math.max(1, threshold / 5)) * 2;
                double cap = Math.max(0, health.getBaseValue() - Rules.n("cleanliness", "minimum_health"));
                health.addOrUpdateTransientModifier(new AttributeModifier(CLEAN_HEALTH, -Math.min(cap, lost), AttributeModifier.Operation.ADD_VALUE));
                if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
            } else health.removeModifier(CLEAN_HEALTH);
        }
        if (gameTime % (Rules.i("fake_creeper", "interval") * 20L) == 0 && Rules.on("fake_creeper") && Rules.roll(player.getRandom(), "fake_creeper")) {
            var behind = player.position().subtract(player.getLookAngle().scale(Rules.n("fake_creeper", "distance")));
            player.connection.send(new ClientboundSoundPacket(SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, behind.x, behind.y, behind.z, (float) Rules.n("fake_creeper", "volume"), 1, player.getRandom().nextLong()));
        }
        if (p.cameraTicks == 0 && gameTime % (Rules.i("camera_lock", "interval") * 20L) == 0 && Rules.on("camera_lock") && Rules.roll(player.getRandom(), "camera_lock")) {
            p.cameraTicks = Rules.i("camera_lock", "seconds") * 20;
            p.originalYaw = player.getYRot(); p.originalPitch = player.getXRot();
            p.cameraYaw = player.getYRot() + (float) Rules.n("camera_lock", "angle"); p.cameraPitch = player.getXRot();
        }
    }
    public static boolean canRest(ServerPlayer player) { return com.sumirelabs.sum1rem0d.Sum1reM0d.initialized() && Rules.list("sleep_deprivation", "dimensions").contains(player.level().dimension().identifier().toString()); }
    public static boolean timedSleep(ServerPlayer player) { return (Rules.on("sleep_deprivation") || Rules.on("sleep_advancement")) && eligible(player) && canRest(player); }
    private static int requiredSleepTicks(ServerPlayer player) { return timedSleep(player) ? Rules.i("sleep_deprivation", "sleep_seconds") * 20 : Player.SLEEP_DURATION; }
    public static boolean sleepingLongEnough(ServerPlayer player) {
        return player.isSleeping() && Math.max(player.getSleepTimer(), WorldState.get(player.level().getServer()).player(player).sleepTicks) >= requiredSleepTicks(player);
    }
    public static void completeRest(ServerPlayer player) {
        if (!eligible(player) || !canRest(player) || !sleepingLongEnough(player)) return;
        var data = WorldState.get(player.level().getServer()).player(player); data.awakeTicks = 0; data.sleepWarningStage = 0;
        data.lastClock = player.level().getOverworldClockTime();
        if (!data.sleepAwarded) { data.sleepAwarded = true; sleepAdvancement(player); }
    }
    private static void cleanup(ServerPlayer player) {
        for (String effect : ModContent.EFFECTS) player.removeEffect(ModContent.effect(effect));
        var health = player.getAttribute(Attributes.MAX_HEALTH); if (health != null) health.removeModifier(CLEAN_HEALTH);
    }
    private static void maintain(ServerPlayer player, String name, boolean enabled, int ticks, int amplifier) {
        var effect = ModContent.effect(name);
        if (enabled) player.addEffect(new MobEffectInstance(effect, ticks, amplifier, false, false, true));
        else player.removeEffect(effect);
    }
    public static void ate(ServerPlayer player, ItemStack eaten) {
        var state = WorldState.get(player.level().getServer());
        boolean forbidden = Rules.matches(eaten, Rules.list("vegan", "forbidden_items"));
        boolean unknown = eaten.has(net.minecraft.core.component.DataComponents.FOOD) && !Rules.matches(eaten, Rules.list("vegan", "allowed_items"));
        if (state.daily("vegan") && eligible(player) && (forbidden || unknown && !Rules.b("vegan", "unknown_food_allowed"))) player.kill(player.level());
        if (eaten.is(Items.MILK_BUCKET) && Rules.b("influenza", "milk_cures")) { state.player(player).influenzaTicks = 0; state.player(player).exposureTicks = 0; }
    }
    public static void damaged(ServerPlayer player) {
        if (!eligible(player) || !Rules.on("damage_effects") || APPLYING_DAMAGE_EFFECT.get()) return;
        var p = WorldState.get(player.level().getServer()).player(player);
        if (p.damageCooldown > 0 || !Rules.roll(player.getRandom(), "damage_effects")) return;
        p.damageCooldown = Rules.i("damage_effects", "cooldown") * 20;
        var pool = new ArrayList<Holder<MobEffect>>();
        for (String entry : Rules.list("damage_effects", player.getRandom().nextDouble() * 100 < Rules.n("damage_effects", "buff_chance") ? "buff_effects" : "debuff_effects")) {
            boolean tag = entry.startsWith("#"); var id = Rules.id(tag ? entry.substring(1) : entry); if (id == null) continue;
            if (tag) BuiltInRegistries.MOB_EFFECT.getTagOrEmpty(net.minecraft.tags.TagKey.create(Registries.MOB_EFFECT, id)).forEach(pool::add);
            else BuiltInRegistries.MOB_EFFECT.get(id).ifPresent(pool::add);
        }
        if (pool.isEmpty()) return;
        var effect = pool.get(player.getRandom().nextInt(pool.size()));
        APPLYING_DAMAGE_EFFECT.set(true);
        try { player.addEffect(new MobEffectInstance(effect, Rules.i("damage_effects", "duration") * 20, Rules.i("damage_effects", "level") - 1)); }
        finally { APPLYING_DAMAGE_EFFECT.set(false); }
    }
    public static void broke(ServerPlayer player, BlockState block, ItemStack tool) {
        if (!eligible(player)) return;
        var p = WorldState.get(player.level().getServer()).player(player);
        if (Rules.on("environment_group") && Rules.matches(block, Rules.list("environment_group", "target_blocks")))
            p.environmentTicks = Math.min(Rules.i("environment_group", "max_seconds") * 20, p.environmentTicks + Rules.i("environment_group", "seconds_per_block") * 20);
        if (Rules.on("environment_anger") && Rules.matches(block, Rules.list("environment_anger", "target_blocks")) && Rules.roll(player.getRandom(), "environment_anger"))
            p.angerTicks = Rules.i("environment_anger", "duration") * 20;
        if (Rules.on("glass_damage") && Rules.matches(block, Rules.list("glass_damage", "target_blocks")) && !(Rules.b("glass_damage", "silk_safe") && silk(player.level(), tool)))
            player.hurtServer(player.level(), player.damageSources().generic(), (float) Rules.n("glass_damage", "damage"));
    }
    public static boolean silk(ServerLevel level, ItemStack tool) { return EnchantmentHelper.getItemEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), tool) > 0; }
    public static void bedrock(ServerPlayer attacker, BlockState block) {
        if (!eligible(attacker) || !Rules.on("bedrock_damage") || !Rules.matches(block, Rules.list("bedrock_damage", "target_blocks"))) return;
        var p = WorldState.get(attacker.level().getServer()).player(attacker);
        if (p.bedrockCooldown > 0) return;
        var candidates = attacker.level().getServer().getPlayerList().getPlayers().stream().filter(Gameplay::eligible)
            .filter(player -> player != attacker || Rules.b("bedrock_damage", "include_attacker")).toList();
        if (candidates.isEmpty()) return;
        p.bedrockCooldown = Rules.i("bedrock_damage", "cooldown") * 20;
        var target = candidates.get(attacker.getRandom().nextInt(candidates.size()));
        target.hurtServer(target.level(), target.damageSources().generic(), (float) Rules.n("bedrock_damage", "damage"));
    }
    public static void sheepKilled(LivingEntity sheep, net.minecraft.world.damagesource.DamageSource source) {
        if (!Rules.on("sheep_red") || !(sheep instanceof net.minecraft.world.entity.animal.sheep.Sheep)) return;
        Entity killer = source.getEntity();
        if (killer instanceof TamableAnimal animal && Rules.b("sheep_red", "include_pets")) killer = animal.getOwner();
        if (killer instanceof ServerPlayer player) { WorldState.get(player.level().getServer()).player(player).redTicks = Rules.i("sheep_red", "seconds") * 20; sendState(player); }
    }
    static net.minecraft.advancements.AdvancementHolder sleepAdvancement(ServerPlayer player) {
        if (!Rules.on("sleep_advancement") || !Rules.roll(player.getRandom(), "sleep_advancement")) return null;
        var candidates = player.level().getServer().getAdvancements().getAllAdvancements().stream()
            .filter(adv -> Rules.list("sleep_advancement", "namespaces").contains(adv.id().getNamespace()) && !adv.id().getPath().startsWith("recipes/"))
            .filter(adv -> adv.value().display().isPresent())
            .filter(adv -> !player.getAdvancements().getOrStartProgress(adv).isDone()).toList();
        if (candidates.isEmpty()) return null;
        var selected = candidates.get(player.getRandom().nextInt(candidates.size()));
        if (Rules.b("sleep_advancement", "fake_only")) {
            PlatformNetwork.send(player, new FakeAdvancementPayload(selected.id(), selected.value().display().orElseThrow()));
            return selected;
        }
        List<String> remaining = new ArrayList<>();
        player.getAdvancements().getOrStartProgress(selected).getRemainingCriteria().forEach(remaining::add);
        for (String criterion : remaining) player.getAdvancements().award(selected, criterion);
        return selected;
    }
    private static void exchangeInventories(MinecraftServer server, WorldState state) {
        Map<String, List<ServerPlayer>> groups = new LinkedHashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) if (eligible(player))
            groups.computeIfAbsent(Rules.b("inventory_swap", "same_dimension") ? player.level().dimension().identifier().toString() : "all", key -> new ArrayList<>()).add(player);
        if (groups.values().stream().noneMatch(group -> group.size() >= 2)) return;
        state.exchangedDay(state.day());
        for (var group : groups.values()) {
            exchangeRows(group, new Random(server.overworld().getSeed() ^ state.day()));
        }
        server.getPlayerList().saveAll(); state.save();
    }
    static void exchangeRows(List<ServerPlayer> group, Random random) {
            Collections.shuffle(group, random);
            for (int index = 0; index + 1 < group.size(); index += 2) {
                var a = group.get(index); var b = group.get(index + 1);
                a.closeContainer(); b.closeContainer();
                int first = Rules.i("inventory_swap", "start_slot"), count = Math.min(Rules.i("inventory_swap", "slots"), 36 - first);
                for (int slot = first; slot < first + count; slot++) {
                    var oldA = a.getInventory().getItem(slot); var oldB = b.getInventory().getItem(slot);
                    a.getInventory().setItem(slot, oldB); b.getInventory().setItem(slot, oldA);
                }
                a.getInventory().setChanged(); b.getInventory().setChanged();
                a.inventoryMenu.broadcastChanges(); b.inventoryMenu.broadcastChanges();
                a.sendSystemMessage(Component.literal("今日の異変で持ち物が入れ替わりました。")); b.sendSystemMessage(Component.literal("今日の異変で持ち物が入れ替わりました。"));
            }
    }
    static void shuffleNames(List<ServerPlayer> players, WorldState state, Random random) {
        var originalNames = new HashSet<String>(); for (var player : players) originalNames.add(player.getGameProfile().name());
        var currentNames = new HashMap<UUID, String>();
        for (var player : players) currentNames.put(player.getUUID(), state.aliases().getOrDefault(player.getUUID().toString(), player.getGameProfile().name()));
        if (!new HashSet<>(currentNames.values()).equals(originalNames)) for (var player : players) currentNames.put(player.getUUID(), player.getGameProfile().name());
        state.aliases().clear(); Collections.shuffle(players, random);
        if (players.size() > 1) for (int index = 0; index < players.size(); index++) state.aliases().put(players.get(index).getUUID().toString(), currentNames.get(players.get((index + 1) % players.size()).getUUID()));
    }
    private static void scheduled(MinecraftServer server, WorldState state, long now) {
        if (!Rules.on("name_swap")) state.aliases().clear();
        for (String event : List.of("zeus", "names", "mob")) {
            String feature = switch(event) { case "names" -> "name_swap"; case "mob" -> "knockback_mob"; default -> "zeus"; };
            if (state.next(event) == 0) state.next(event, now + Rules.i(feature, "interval") * 20L);
            if (now < state.next(event)) continue;
            state.next(event, now + Rules.i(feature, "interval") * 20L);
            if (!Rules.on(feature)) continue;
            var players = new ArrayList<>(server.getPlayerList().getPlayers().stream().filter(Gameplay::eligible).toList());
            if (players.isEmpty()) continue;
            var random = server.overworld().getRandom();
            if (event.equals("names")) {
                shuffleNames(players, state, new Random(random.nextLong()));
            } else if (Rules.roll(random, feature)) {
                var player = players.get(random.nextInt(players.size()));
                if (event.equals("zeus")) {
                    if (!hasCeiling(player) && state.player(player).lightningTicks == 0) {
                        var p = state.player(player);
                        p.lightningTicks = (Rules.b(feature, "warning") ? Rules.i(feature, "warning_seconds") * 20 : 0) + 1;
                        if (Rules.b(feature, "warning")) { player.sendSystemMessage(Component.literal("ゼウスがあなたを見ています。天井の下へ！")); player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.LIGHTNING_BOLT_THUNDER), SoundSource.WEATHER, player.getX(), player.getY(), player.getZ(), 1, 1, player.getRandom().nextLong())); }
                    }
                } else spawnKnockback(server, player);
            }
        }
    }
    private static boolean hasCeiling(ServerPlayer player) {
        int distance = Rules.i("zeus", "ceiling_distance");
        var start = player.getEyePosition();
        double top = distance == 0 ? player.level().getMaxY() : Math.min(player.level().getMaxY(), start.y + distance);
        if (top <= start.y) return false;
        return player.level().clip(new ClipContext(start, new net.minecraft.world.phys.Vec3(start.x, top, start.z), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == net.minecraft.world.phys.HitResult.Type.BLOCK;
    }
    private static void strike(ServerPlayer player) {
        if (hasCeiling(player)) return;
        var bolt = EntityTypes.LIGHTNING_BOLT.create(player.level(), EntitySpawnReason.TRIGGERED);
        if (bolt != null) { bolt.setPos(player.getX(), player.getY(), player.getZ()); bolt.setVisualOnly(true); player.level().addFreshEntity(bolt); }
        player.kill(player.level());
    }
    static void spawnKnockback(MinecraftServer server, ServerPlayer player) {
        if (!Rules.on("knockback_mob") || !eligible(player)) return;
        if (Rules.b("knockback_mob", "night_only") && Math.floorMod(player.level().getOverworldClockTime(), 24000) < 13000) return;
        int alive = 0;
        for (ServerLevel level : server.getAllLevels()) for (Entity entity : level.getAllEntities()) if (entity.isAlive() && entity.entityTags().contains("sum1rem0d.knockback")) alive++;
        if (alive >= Rules.i("knockback_mob", "world_limit")) return;
        var id = Rules.id(Rules.s("knockback_mob", "mob_type"));
        if (id == null) return;
        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null); if (type == null) return;
        double minimum = Rules.n("knockback_mob", "minimum_distance"), maximum = Rules.n("knockback_mob", "maximum_distance");
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2;
            double radius = minimum + player.getRandom().nextDouble() * (maximum - minimum);
            int x = net.minecraft.util.Mth.floor(player.getX() + Math.cos(angle) * radius), z = net.minecraft.util.Mth.floor(player.getZ() + Math.sin(angle) * radius);
            double distanceSquared = (x + .5 - player.getX()) * (x + .5 - player.getX()) + (z + .5 - player.getZ()) * (z + .5 - player.getZ());
            if (distanceSquared < minimum * minimum || distanceSquared > maximum * maximum) continue;
            if (!player.level().hasChunkAt(new BlockPos(x, 0, z))) continue;
            BlockPos pos = player.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!player.level().getBlockState(pos).isAir() || !player.level().getFluidState(pos).isEmpty()) continue;
            if (player.level().getBlockState(pos.below()).getCollisionShape(player.level(), pos.below()).isEmpty()) continue;
            var entity = type.create(player.level(), EntitySpawnReason.TRIGGERED);
            if (!(entity instanceof Mob mob)) return;
            mob.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            if (!player.level().noCollision(mob)) continue;
            var stick = new ItemStack(Items.STICK);
            stick.enchant(player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.KNOCKBACK), Rules.i("knockback_mob", "knockback"));
            mob.setItemSlot(EquipmentSlot.MAINHAND, stick);
            mob.setDropChance(EquipmentSlot.MAINHAND, (float) Rules.n("knockback_mob", "drop_chance") / 100);
            mob.addTag("sum1rem0d.knockback"); mob.setTarget(player); player.level().addFreshEntity(mob); return;
        }
    }
    public static boolean useBed(ServerPlayer player) {
        if (!Rules.on("warden_bed") || !Rules.list("warden_bed", "dimensions").contains(player.level().dimension().identifier().toString())) return false;
        var p = WorldState.get(player.level().getServer()).player(player);
        if (p.bedCooldown == 0) {
            p.bedCooldown = Rules.i("warden_bed", "cooldown") * 20;
            int radius = Rules.i("warden_bed", "radius");
            var positions = new ArrayList<BlockPos>();
            for (int x = -radius; x <= radius; x++) for (int y = -2; y <= 2; y++) for (int z = -radius; z <= radius; z++) positions.add(player.blockPosition().offset(x, y, z));
            Collections.shuffle(positions, new Random(player.getRandom().nextLong()));
            int spawned = 0;
            for (BlockPos pos : positions) {
                if (spawned >= Rules.i("warden_bed", "count")) break;
                if (!player.level().hasChunkAt(pos) || !player.level().getBlockState(pos).isAir() || !player.level().getBlockState(pos.above()).isAir() || !player.level().getBlockState(pos.above(2)).isAir() || player.level().getBlockState(pos.below()).getCollisionShape(player.level(), pos.below()).isEmpty()) continue;
                var preview = EntityTypes.WARDEN.create(player.level(), EntitySpawnReason.TRIGGERED);
                if (preview == null) break;
                preview.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
                if (player.level().noCollision(preview) && EntityTypes.WARDEN.spawn(player.level(), pos, EntitySpawnReason.TRIGGERED) != null) spawned++;
            }
        }
        return true;
    }
    public static void sendState(ServerPlayer player) {
        var state = WorldState.get(player.level().getServer()); state.updateDay();
        var p = state.player(player);
        var json = new JsonObject(); json.addProperty("day", state.day()); json.addProperty("fuel", state.fuelMultiplier()); json.addProperty("fuelEfficiency", state.fuelEfficiency()); json.addProperty("furnacePermutation", state.furnacePermutation());
        var daily = new JsonObject(); for (String name : DailyEvents.FEATURES) daily.addProperty(name, state.daily(name)); json.add("daily", daily);
        var white = new JsonObject();
        if (Rules.on("afk_white")) for (ServerPlayer other : player.level().getServer().getPlayerList().getPlayers()) if (state.player(other).afkTicks >= Rules.i("afk_white", "after") * 20) white.addProperty(other.getUUID().toString(), true);
        json.add("white", white); var aliases = new JsonObject(); if (Rules.on("name_swap")) state.aliases().forEach(aliases::addProperty); json.add("aliases", aliases);
        json.addProperty("cleanliness", p.cleanliness); json.addProperty("awakeDays", p.awakeTicks / 24000.0);
        json.addProperty("headache", player.hasEffect(ModContent.effect("headache")));
        json.addProperty("redTicks", p.redTicks); json.addProperty("cameraTicks", p.cameraTicks); json.addProperty("cameraYaw", p.cameraYaw); json.addProperty("cameraPitch", p.cameraPitch);
        json.addProperty("originalYaw", p.originalYaw); json.addProperty("originalPitch", p.originalPitch);
        json.add("economy", Economy.state(player));
        PlatformNetwork.send(player, new GameStatePayload(json.toString()));
    }
}
