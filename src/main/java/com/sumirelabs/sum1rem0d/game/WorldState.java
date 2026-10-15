package com.sumirelabs.sum1rem0d.game;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

public final class WorldState {
    private static final Map<MinecraftServer, WorldState> ACTIVE = new WeakHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile MinecraftServer currentServer;
    public static final class PlayerData {
        public double cleanliness = 100;
        public long awakeTicks;
        public int sleepTicks, exposureTicks, influenzaTicks, environmentTicks, angerTicks;
        public int explosionCooldown, damageCooldown, bedrockCooldown, bedCooldown;
        public int afkTicks, redTicks, cameraTicks, lightningTicks;
        public int sleepWarningStage;
        public float cameraYaw, cameraPitch, originalYaw, originalPitch;
        public transient double lastX = Double.NaN, lastY, lastZ;
        public transient float lastYaw, lastPitch;
        public transient long lastAction, lastClock = Long.MIN_VALUE;
        public transient boolean wasAlive = true, sleepAwarded;
    }
    private static final class Data {
        int schema = 1;
        long day = Long.MIN_VALUE;
        long moonExchangeDay = Long.MIN_VALUE;
        double fuelMultiplier = 1;
        Map<String, Boolean> daily = new LinkedHashMap<>();
        Map<String, PlayerData> players = new LinkedHashMap<>();
        Map<String, String> aliases = new LinkedHashMap<>();
        int furnacePermutation;
        long nextZeus, nextNames, nextMob;
    }
    private final Path path;
    private final MinecraftServer server;
    private Data data;
    // Package access also lets the native verifier read the actual saved file
    // independently, without replacing the live server's state instance.
    WorldState(MinecraftServer server) {
        this.server = server;
        path = server.getWorldPath(LevelResource.ROOT).resolve("data/sum1rem0d-state.json");
        try {
            data = Files.exists(path) ? GSON.fromJson(Files.readString(path), Data.class) : new Data();
            if (data == null || data.schema != 1 || data.players == null || data.daily == null || data.aliases == null)
                throw new IllegalStateException("Invalid or unsupported world state");
        } catch (IOException | RuntimeException exception) { throw new IllegalStateException("Cannot read world state; original preserved: " + path, exception); }
    }
    public static WorldState get(MinecraftServer server) { currentServer = server; return ACTIVE.computeIfAbsent(server, WorldState::new); }
    public static boolean serverThread() { var server = currentServer; return server != null && server.isSameThread(); }
    public static MinecraftServer validationServer() { var server = currentServer; return server != null && server.isSameThread() ? server : null; }
    public static void saveIfLoaded(MinecraftServer server) { var state = ACTIVE.get(server); if (state != null) state.save(); }
    public static void unload(MinecraftServer server) { RedstoneRefresh.close(server); saveIfLoaded(server); ACTIVE.remove(server); if (currentServer == server) currentServer = null; }
    public PlayerData player(ServerPlayer player) { return data.players.computeIfAbsent(player.getUUID().toString(), key -> { var p = new PlayerData(); p.cleanliness = Rules.n("cleanliness", "maximum"); return p; }); }
    public boolean daily(String feature) { return Rules.on(feature) && data.daily.getOrDefault(feature, false); }
    public double fuelEfficiency() { return Rules.on("daily_fuel") ? data.fuelMultiplier : 1; }
    public double fuelMultiplier() { return FuelAccounting.consumptionMultiplier(fuelEfficiency()); }
    public void fuelEfficiency(double value) { FuelAccounting.consumptionMultiplier(value); data.fuelMultiplier = value; }
    public int furnacePermutation() { return data.furnacePermutation; }
    public Map<String, String> aliases() { return data.aliases; }
    // Native verification restores the complete saved state after temporary fixtures.
    JsonObject checkpoint() { return GSON.toJsonTree(data).getAsJsonObject(); }
    void restore(JsonObject checkpoint) { data = GSON.fromJson(checkpoint, Data.class); }
    public long day() { return data.day; }
    public long exchangedDay() { return data.moonExchangeDay; }
    public void exchangedDay(long day) { data.moonExchangeDay = day; }
    public long next(String event) { return switch(event) { case "zeus" -> data.nextZeus; case "names" -> data.nextNames; default -> data.nextMob; }; }
    public void next(String event, long tick) { switch(event) { case "zeus" -> data.nextZeus = tick; case "names" -> data.nextNames = tick; default -> data.nextMob = tick; } }
    public boolean updateDay() {
        long day = Math.floorDiv(server.overworld().getOverworldClockTime(), 24000);
        if (day == data.day) return false;
        data.day = day;
        for (String feature : List.of("vegan", "upside_down_grass", "diamond_disguise", "hardness_swap")) {
            var random = new Random(server.overworld().getSeed() ^ day * 0x9E3779B97F4A7C15L ^ feature.hashCode());
            data.daily.put(feature, random.nextDouble() * 100 < Rules.n(feature, "chance"));
        }
        var random = new Random(server.overworld().getSeed() ^ day * 0xD1B54A32D192ED03L);
        double min = Rules.n("daily_fuel", "minimum"), max = Math.max(min, Rules.n("daily_fuel", "maximum"));
        data.fuelMultiplier = min + random.nextDouble() * (max - min);
        data.furnacePermutation = random.nextInt(6);
        save();
        return true;
    }
    public void save() {
        try {
            Files.createDirectories(path.getParent());
            Path temp = Files.createTempFile(path.getParent(), "sum1rem0d-state", ".tmp");
            try {
                Files.writeString(temp, GSON.toJson(data) + "\n");
                try { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException exception) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temp); }
        } catch (IOException exception) { Sum1reM0d.LOGGER.error("Cannot save world state {}", path, exception); }
    }
}
