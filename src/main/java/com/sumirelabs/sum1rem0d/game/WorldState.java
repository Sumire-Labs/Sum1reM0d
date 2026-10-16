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
        public long coins;
        public boolean walletInitialized;
        public JsonObject services=new JsonObject();
        public transient long lastServiceRequest=Long.MIN_VALUE,lastServiceFailure=Long.MIN_VALUE;
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
        Set<String> coinBlocks = new HashSet<>();
        Map<String, JsonObject> coinInventories = new LinkedHashMap<>();
        int eventVersion;
        long day = Long.MIN_VALUE;
        long moonExchangeDay = Long.MIN_VALUE;
        double fuelMultiplier = 1;
        Map<String, Boolean> daily = new LinkedHashMap<>();
        Map<String, PlayerData> players = new LinkedHashMap<>();
        Map<String, String> aliases = new LinkedHashMap<>();
        int furnacePermutation;
        long nextZeus, nextNames, nextMob, serviceTick;
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
            if (data.eventVersion != 1) { data.day = Long.MIN_VALUE; data.eventVersion = 1; data.daily.clear(); data.fuelMultiplier = 1; data.furnacePermutation = 0; }
        } catch (IOException | RuntimeException exception) { throw new IllegalStateException("Cannot read world state; original preserved: " + path, exception); }
    }
    public static WorldState get(MinecraftServer server) { currentServer = server; return ACTIVE.computeIfAbsent(server, WorldState::new); }
    public static boolean serverThread() { var server = currentServer; return server != null && server.isSameThread(); }
    public static MinecraftServer validationServer() { var server = currentServer; return server != null && server.isSameThread() ? server : null; }
    public static void saveIfLoaded(MinecraftServer server) { var state = ACTIVE.get(server); if (state != null) state.save(); }
    public static void unload(MinecraftServer server) { CraftingMaterialSwap.close(server); RedstoneRefresh.close(server); saveIfLoaded(server); ACTIVE.remove(server); if (currentServer == server) currentServer = null; }
    public PlayerData player(ServerPlayer player) { return data.players.computeIfAbsent(player.getUUID().toString(), key -> { var p = new PlayerData(); p.cleanliness = Rules.n("cleanliness", "maximum"); return p; }); }
    public Map<String, JsonObject> coinInventories() { if (data.coinInventories == null) data.coinInventories = new LinkedHashMap<>(); return data.coinInventories; }
    public boolean markCoinBlock(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        if (data.coinBlocks == null) data.coinBlocks = new HashSet<>();
        return !data.coinBlocks.add(level.dimension().identifier() + ":" + pos.asLong());
    }
    public boolean daily(String feature) { return DailyEvents.FEATURES.contains(feature) && Rules.on("daily_events") && Rules.on(feature) && data.daily.getOrDefault(feature, false); }
    public double fuelEfficiency() { return daily("daily_fuel") ? data.fuelMultiplier : 1; }
    public double fuelMultiplier() { return FuelAccounting.consumptionMultiplier(fuelEfficiency()); }
    public void fuelEfficiency(double value) { FuelAccounting.consumptionMultiplier(value); data.fuelMultiplier = value; data.daily.put("daily_fuel", true); }
    public int furnacePermutation() { return daily("furnace_textures") ? data.furnacePermutation : 0; }
    public Map<String, String> aliases() { return data.aliases; }
    // Native verification restores the complete saved state after temporary fixtures.
    JsonObject checkpoint() { return GSON.toJsonTree(data).getAsJsonObject(); }
    void restore(JsonObject checkpoint) { data = GSON.fromJson(checkpoint, Data.class); }
    public long serviceTime(){return data.serviceTick;}
    public void advanceServices(){data.serviceTick=Math.addExact(data.serviceTick,1);}
    public long day() { return data.day; }
    public long exchangedDay() { return data.moonExchangeDay; }
    public void exchangedDay(long day) { data.moonExchangeDay = day; }
    public long next(String event) { return switch(event) { case "zeus" -> data.nextZeus; case "names" -> data.nextNames; default -> data.nextMob; }; }
    public void next(String event, long tick) { switch(event) { case "zeus" -> data.nextZeus = tick; case "names" -> data.nextNames = tick; default -> data.nextMob = tick; } }
    public boolean updateDay() {
        long day = Math.floorDiv(server.overworld().getOverworldClockTime(), 24000);
        if (day == data.day) return false;
        data.day = day;
        var weights = new LinkedHashMap<String, Double>();
        for (String feature : DailyEvents.FEATURES) if (Rules.on(feature)) weights.put(feature, Rules.n(feature, "chance"));
        String selected = DailyEvents.choose(server.overworld().getSeed(), day, Rules.on("daily_events") ? Rules.n("daily_events", "chance") : 0, weights);
        data.daily.clear();
        for (String feature : DailyEvents.FEATURES) data.daily.put(feature, feature.equals(selected));
        var random = new Random(server.overworld().getSeed() ^ day * 0xD1B54A32D192ED03L);
        double min = Rules.n("daily_fuel", "minimum"), max = Math.max(min, Rules.n("daily_fuel", "maximum"));
        data.fuelMultiplier = selected.equals("daily_fuel") ? min + random.nextDouble() * (max - min) : 1;
        data.furnacePermutation = selected.equals("furnace_textures") ? 1 + random.nextInt(5) : 0;
        save();
        return true;
    }
    public void save() {
        try { saveStrict(); } catch (IllegalStateException exception) { Sum1reM0d.LOGGER.error("Cannot save world state {}", path, exception); }
    }
    public void saveStrict() {
        try {
            Files.createDirectories(path.getParent());
            Path temp = Files.createTempFile(path.getParent(), "sum1rem0d-state", ".tmp");
            try {
                Files.writeString(temp, GSON.toJson(data) + "\n");
                try (var channel = java.nio.channels.FileChannel.open(temp, StandardOpenOption.WRITE)) { channel.force(true); }
                try { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException exception) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temp); }
        } catch (IOException exception) { throw new IllegalStateException("Cannot save world state " + path, exception); }
    }
}
