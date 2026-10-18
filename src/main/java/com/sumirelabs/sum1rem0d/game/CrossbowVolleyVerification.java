package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;

/** Conservation checks for captured, fired and Config-OFF crossbow payloads. */
final class CrossbowVolleyVerification {
    private CrossbowVolleyVerification() {}

    static void verify(MinecraftServer server, java.util.function.Consumer<String> passed) throws Exception {
        var store = Sum1reM0d.config();
        var original = store.snapshot();
        var draft = original.deepCopy();
        var shooter = EconomyVerification.actor(server, server.overworld(), "CrossbowManualRelease");
        try {
            for (var entry : draft.getAsJsonObject("features").entrySet()) entry.getValue().getAsJsonObject().addProperty("enabled", false);
            var feature = draft.getAsJsonObject("features").getAsJsonObject("crossbow_circle");
            feature.addProperty("enabled", true);
            feature.addProperty("max_stored_arrows", 2);
            store.update(draft, store.revision());
            var level = server.overworld();
            var bow = new ItemStack(Items.CROSSBOW);
            bow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(List.of(new ItemStack(Items.ARROW))));
            require(CrossbowVolley.intercept(level, null, bow), "First paid arrow queues below volley threshold");
            require(CrossbowVolley.queuedCount(level, bow) == 1 && bow.get(DataComponents.CHARGED_PROJECTILES) == null,
                    "Queued arrow persists only in the crossbow component");

            bow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(List.of(new ItemStack(Items.TIPPED_ARROW))));
            require(!CrossbowVolley.intercept(level, null, bow), "Second paid arrow triggers vanilla volley");
            var volley = bow.get(DataComponents.CHARGED_PROJECTILES);
            require(volley != null && volley.items().size() == 2 && CrossbowVolley.queuedCount(level, bow) == 0,
                    "The threshold volley contains each paid projectile once and clears the queue");

            feature.addProperty("max_stored_arrows", 8);
            feature.addProperty("manual_release_enabled", true);
            store.update(draft, store.revision());
            level.addNewPlayer(shooter);
            var manual = new ItemStack(Items.CROSSBOW);
            manual.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(List.of(new ItemStack(Items.ARROW))));
            require(CrossbowVolley.intercept(level, shooter, manual), "A sub-cap paid arrow joins the saved manual volley");
            manual.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(List.of(new ItemStack(Items.TIPPED_ARROW))));
            require(CrossbowVolley.intercept(level, shooter, manual) && CrossbowVolley.queuedCount(level, manual) == 2,
                    "A second sub-cap paid projectile remains queued until explicit release");
            shooter.setItemInHand(InteractionHand.MAIN_HAND, manual);
            shooter.setShiftKeyDown(false);
            require(!CrossbowVolley.releaseOnShiftUse(level, shooter, manual) && CrossbowVolley.queuedCount(level, manual) == 2,
                    "Manual volley release requires the configured sneak-click gesture");
            shooter.setShiftKeyDown(true);
            feature.addProperty("manual_release_enabled", false); store.update(draft, store.revision());
            require(!CrossbowVolley.releaseOnShiftUse(level, shooter, manual) && CrossbowVolley.queuedCount(level, manual) == 2,
                    "Manual release Config OFF preserves the complete paid queue");
            feature.addProperty("manual_release_enabled", true); store.update(draft, store.revision());
            require(CrossbowVolley.releaseOnShiftUse(level, shooter, manual)
                    && CrossbowVolley.queuedCount(level, manual) == 0
                    && manual.get(DataComponents.CHARGED_PROJECTILES).items().size() == 2,
                    "Sneak-use transfers the entire paid sub-cap queue to vanilla charged projectiles");
            var arrowUuids = new java.util.HashSet<java.util.UUID>();
            for (Entity entity : ServerVerification.spawnedIn(level)) if (entity instanceof AbstractArrow) arrowUuids.add(entity.getUUID());
            ((CrossbowItem) Items.CROSSBOW).performShooting(level, shooter, InteractionHand.MAIN_HAND, manual, 3.15f, 1.0f, null);
            long newArrows = ServerVerification.spawnedIn(level).stream().filter(entity -> entity instanceof AbstractArrow && !arrowUuids.contains(entity.getUUID())).count();
            require(newArrows == 2, "Manual release passes each charged projectile to vanilla firing once: " + newArrows);
            passed.accept("Crossbow manual sneak-release sends a below-cap paid queue through native projectile firing without duplication");

            var disabledManual = new ItemStack(Items.CROSSBOW);
            disabledManual.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(List.of(new ItemStack(Items.ARROW))));
            require(CrossbowVolley.intercept(level, shooter, disabledManual), "Config-OFF fixture stores a paid arrow before disabling");
            var recovery = new ItemStack(Items.CROSSBOW);
            recovery.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(List.of(new ItemStack(Items.ARROW))));
            require(CrossbowVolley.intercept(level, null, recovery), "Recovery fixture stores a paid arrow");
            feature.addProperty("enabled", false);
            store.update(draft, store.revision());
            shooter.setShiftKeyDown(true);
            require(!CrossbowVolley.releaseOnShiftUse(level, shooter, disabledManual), "Config OFF ignores the manual-release gesture");
            CrossbowVolley.prepareUse(level, disabledManual);
            require(CrossbowVolley.queuedCount(level, disabledManual) == 0
                    && disabledManual.get(DataComponents.CHARGED_PROJECTILES).items().size() == 1,
                    "Config OFF recovers the queued arrow to vanilla firing instead of manual release");
            CrossbowVolley.prepareUse(level, recovery);
            require(CrossbowVolley.queuedCount(level, recovery) == 0
                    && recovery.get(DataComponents.CHARGED_PROJECTILES) != null
                    && recovery.get(DataComponents.CHARGED_PROJECTILES).items().size() == 1,
                    "Config OFF restores the already-paid arrow to vanilla firing without refund");
            passed.accept("Crossbow saved volley, tipped-arrow preservation and Config OFF projectile conservation");
        } finally {
            store.update(original, store.revision());
            shooter.discard();
        }
    }
}
