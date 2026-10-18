package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;

/** Server-side conservation verification for save/load, death transfer and Config OFF recovery. */
final class SecondOffhandVerification {
    private SecondOffhandVerification() {}

    static void verify(MinecraftServer server, java.util.function.Consumer<String> passed) throws Exception {
        var store = Sum1reM0d.config();
        var original = store.snapshot();
        var draft = original.deepCopy();
        var owner = EconomyVerification.actor(server, server.overworld(), "SecondOffhandOwner");
        var restored = EconomyVerification.actor(server, server.overworld(), "SecondOffhandRespawn");
        var reloaded = EconomyVerification.actor(server, server.overworld(), "SecondOffhandReloaded");
        var droppedRespawn = EconomyVerification.actor(server, server.overworld(), "SecondOffhandDropRespawn");
        var overflowDrops = new java.util.ArrayList<ItemEntity>();
        try {
            var features = draft.getAsJsonObject("features");
            for (var entry : features.entrySet()) entry.getValue().getAsJsonObject().addProperty("enabled", false);
            var feature = features.getAsJsonObject("second_offhand");
            feature.addProperty("enabled", true);
            feature.addProperty("death_keep", true);
            feature.addProperty("forehead_display", true);
            store.update(draft, store.revision());

            var held = new ItemStack(Items.DIAMOND, 7);
            ((SecondOffhandAccess) owner).sum1rem0d$secondOffhand(held);
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, owner.registryAccess());
            owner.saveWithoutId(output);
            reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, reloaded.registryAccess(), output.buildResult()));
            require(((SecondOffhandAccess) reloaded).sum1rem0d$secondOffhand().getCount() == 7
                    && ((SecondOffhandAccess) reloaded).sum1rem0d$secondOffhand().is(Items.DIAMOND), "Second offhand item persists through native player save/load");
            reloaded.discard();

            SecondOffhandService.restore(restored, owner);
            require(((SecondOffhandAccess) owner).sum1rem0d$secondOffhand().isEmpty()
                    && ((SecondOffhandAccess) restored).sum1rem0d$secondOffhand().getCount() == 7,
                    "Death keep transfers exactly one stack to the respawned player");

            owner.containerMenu.setCarried(new ItemStack(Items.GOLD_INGOT, 5));
            ((SecondOffhandAccess) owner).sum1rem0d$secondOffhand(new ItemStack(Items.DIAMOND, 3));
            SecondOffhandService.requestSwap(owner);
            require(((SecondOffhandAccess) owner).sum1rem0d$secondOffhand().is(Items.GOLD_INGOT)
                    && ((SecondOffhandAccess) owner).sum1rem0d$secondOffhand().getCount() == 5
                    && owner.containerMenu.getCarried().is(Items.DIAMOND)
                    && owner.containerMenu.getCarried().getCount() == 3,
                    "Second-offhand swap exchanges the server cursor with the virtual slot exactly once");
            SecondOffhandService.requestSwap(owner);
            require(((SecondOffhandAccess) owner).sum1rem0d$secondOffhand().is(Items.DIAMOND)
                    && ((SecondOffhandAccess) owner).sum1rem0d$secondOffhand().getCount() == 3,
                    "Second-offhand reverse swap preserves both stacks");

            feature.addProperty("death_keep", false);
            store.update(draft, store.revision());
            var droppedStack = new ItemStack(Items.NETHER_STAR, 5);
            ((SecondOffhandAccess) owner).sum1rem0d$secondOffhand(droppedStack);
            long beforeDrops = countDrops(owner, Items.NETHER_STAR, 5);
            SecondOffhandService.restore(droppedRespawn, owner);
            long afterDrops = countDrops(owner, Items.NETHER_STAR, 5);
            require(((SecondOffhandAccess) owner).sum1rem0d$secondOffhand().isEmpty()
                    && ((SecondOffhandAccess) droppedRespawn).sum1rem0d$secondOffhand().isEmpty()
                    && afterDrops == beforeDrops + 1,
                    "Death keep OFF drops the exact virtual-slot stack once");
            overflowDrops.addAll(owner.level().getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(6), item -> item.getItem().is(Items.NETHER_STAR) && item.getItem().getCount() == 5));

            feature.addProperty("enabled", false);
            store.update(draft, store.revision());
            SecondOffhandService.disablePlayer(restored);
            boolean returned = java.util.stream.IntStream.range(0, restored.getInventory().getContainerSize())
                    .mapToObj(restored.getInventory()::getItem).anyMatch(item -> item.is(Items.DIAMOND) && item.getCount() == 7);
            require(((SecondOffhandAccess) restored).sum1rem0d$secondOffhand().isEmpty()
                    && returned,
                    "Config OFF returns the item to inventory without loss or duplication");

            for (int slot = 0; slot < owner.getInventory().getContainerSize(); slot++) owner.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            ((SecondOffhandAccess) owner).sum1rem0d$secondOffhand(new ItemStack(Items.NETHER_STAR, 7));
            long fullBefore = countDrops(owner, Items.NETHER_STAR, 7);
            SecondOffhandService.disablePlayer(owner);
            long fullAfter = countDrops(owner, Items.NETHER_STAR, 7);
            require(((SecondOffhandAccess) owner).sum1rem0d$secondOffhand().isEmpty() && fullAfter == fullBefore + 1,
                    "Config OFF with a full inventory drops the entire remaining stack once");
            overflowDrops.addAll(owner.level().getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(6), item -> item.getItem().is(Items.NETHER_STAR) && item.getItem().getCount() == 7));

            SecondOffhandService.syncOwner(owner);
            SecondOffhandService.disconnect(owner);
            require(((SecondOffhandAccess) owner).sum1rem0d$secondOffhand().isEmpty(),
                    "Owner sync and disconnect cleanup never resurrect or delete a stored item");
            passed.accept("Second offhand NBT/respawn/Config OFF, cursor swaps, non-keep drops, full-inventory overflow, sync and disconnect conservation");
        } finally {
            store.update(original, store.revision());
            overflowDrops.forEach(ItemEntity::discard);
            owner.discard();
            restored.discard();
            reloaded.discard();
            droppedRespawn.discard();
        }
    }

    private static long countDrops(ServerPlayer player, net.minecraft.world.item.Item item, int count) {
        return player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(6), entity ->
                entity.getItem().is(item) && entity.getItem().getCount() == count).size();
    }
}
