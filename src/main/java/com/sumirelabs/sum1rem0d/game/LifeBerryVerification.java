package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import net.minecraft.server.MinecraftServer;

/** Native verifier for life-berry stacking caps, respawn restoration, and reversible Config behavior. */
final class LifeBerryVerification {
    private LifeBerryVerification() {}

    static void verify(MinecraftServer server, java.util.function.Consumer<String> passed) throws Exception {
        var store = Sum1reM0d.config();
        var original = store.snapshot();
        var draft = original.deepCopy();
        var eater = EconomyVerification.actor(server, server.overworld(), "LifeBerryEater");
        var respawned = EconomyVerification.actor(server, server.overworld(), "LifeBerryRespawned");
        double base = eater.getMaxHealth();
        try {
            var features = draft.getAsJsonObject("features");
            for (var entry : features.entrySet()) entry.getValue().getAsJsonObject().addProperty("enabled", false);
            var berry = features.getAsJsonObject("life_berry");
            berry.addProperty("enabled", true);
            berry.addProperty("max_health_bonus", 2);
            berry.addProperty("maximum_bonus", 4);
            store.update(draft, store.revision());

            LifeBerryItem.applyBonus(eater);
            require(eater.getMaxHealth() == base + 2, "First life berry adds configured permanent maximum health");
            LifeBerryItem.applyBonus(eater);
            require(eater.getMaxHealth() == base + 4, "Life berry bonus reaches configured cumulative cap");
            LifeBerryItem.applyBonus(eater);
            require(eater.getMaxHealth() == base + 4, "Life berry cannot exceed cumulative cap");

            LifeBerryItem.restoreAfterDeath(respawned, eater);
            require(respawned.getMaxHealth() == base + 4, "Permanent life berry modifiers copy to a non-keep-inventory respawn");
            berry.addProperty("enabled", false);
            store.update(draft, store.revision());
            require(eater.getMaxHealth() == base + 4 && respawned.getMaxHealth() == base + 4,
                    "Config OFF preserves already earned life berry health");
            berry.addProperty("enabled", true);
            berry.addProperty("maximum_bonus", 1);
            store.update(draft, store.revision());
            LifeBerryItem.applyBonus(eater);
            require(eater.getMaxHealth() == base + 4, "Lowering the cap does not remove existing earned health");
            passed.accept("Life berry permanent health cap, respawn persistence, Config OFF and cap reductions");
        } finally {
            store.update(original, store.revision());
            eater.discard();
            respawned.discard();
        }
    }
}
