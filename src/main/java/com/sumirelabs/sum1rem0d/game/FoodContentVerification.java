package com.sumirelabs.sum1rem0d.game;

import static com.sumirelabs.sum1rem0d.game.ServerVerification.require;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;

/** Native server checks for the L02/L06/L07/W24 food mechanics and saved state. */
final class FoodContentVerification {
    private FoodContentVerification() {}

    static void verify(MinecraftServer server, java.util.function.Consumer<String> passed) throws Exception {
        var store = Sum1reM0d.config();
        var original = store.snapshot();
        var draft = original.deepCopy();
        var owner = EconomyVerification.actor(server, server.overworld(), "FoodContentOwner");
        var reloaded = EconomyVerification.actor(server, server.overworld(), "FoodContentReloaded");
        var respawned = EconomyVerification.actor(server, server.overworld(), "FoodContentRespawn");
        try {
            var features = draft.getAsJsonObject("features");
            for (var entry : features.entrySet()) entry.getValue().getAsJsonObject().addProperty("enabled", false);
            var cigarette = features.getAsJsonObject("cigarette");
            cigarette.addProperty("enabled", true); cigarette.addProperty("buff_seconds", 180); cigarette.addProperty("buff_level", 1);
            cigarette.addProperty("buff_effects", "minecraft:speed"); cigarette.addProperty("debuff_seconds", 15);
            cigarette.addProperty("debuff_level", 1); cigarette.addProperty("debuff_effects", "minecraft:weakness");
            var milk = features.getAsJsonObject("milk_combo");
            milk.addProperty("enabled", true); milk.addProperty("buff_effects", "minecraft:haste");
            milk.addProperty("duration_seconds", 120); milk.addProperty("days_per_level", 1); milk.addProperty("maximum_level", 3);
            var bottle = features.getAsJsonObject("bottled_milk");
            bottle.addProperty("enabled", true); bottle.addProperty("milk_bundle_enabled", true);
            var meat = features.getAsJsonObject("cat_meat");
            meat.addProperty("enabled", true); meat.addProperty("drop_chance", 1); meat.addProperty("drop_min", 1); meat.addProperty("drop_max", 1);
            meat.addProperty("poison_chance", 1); meat.addProperty("poison_seconds", 5);
            meat.addProperty("hunger_chance", 1); meat.addProperty("hunger_seconds", 5);
            store.update(draft, store.revision());

            var cigaretteInput = CraftingInput.of(2, 1, List.of(new ItemStack(Items.PAPER), new ItemStack(Items.COAL)));
            var cigaretteRecipe = new FoodRecipe("cigarette");
            require(cigaretteRecipe.matches(cigaretteInput, server.overworld())
                    && cigaretteRecipe.assemble(cigaretteInput).getCount() == 4,
                    "Paper and coal craft exactly four cigarettes");
            FoodContent.consumeCigarette(owner, server.overworld());
            var timer = ModContent.effect("cigarette_delay");
            require(owner.hasEffect(MobEffects.SPEED) && owner.hasEffect(timer),
                    "Cigarette applies the configured buff and a persisted delayed-effect timer");
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, owner.registryAccess());
            owner.saveWithoutId(output);
            reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, reloaded.registryAccess(), output.buildResult()));
            require(reloaded.hasEffect(timer), "Cigarette delay timer survives player save/load");
            respawned.restoreFrom(owner, false);
            require(respawned.hasEffect(timer), "Cigarette delay timer survives native death respawn transfer");
            var delayEffect = BuiltInRegistries.MOB_EFFECT.getValue(ModContent.id("cigarette_delay"));
            delayEffect.applyEffectTick(server.overworld(), owner, 0);
            require(owner.hasEffect(MobEffects.WEAKNESS), "The random configured cigarette drawback applies after the delay");
            owner.removeAllEffects();
            cigarette.addProperty("enabled", false); store.update(draft, store.revision());
            owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(timer, 1, 0));
            delayEffect.applyEffectTick(server.overworld(), owner, 0);
            require(!owner.hasEffect(MobEffects.WEAKNESS), "Config OFF suppresses a previously scheduled cigarette drawback");
            owner.removeAllEffects();
            FoodContent.consumeCigarette(owner, server.overworld());
            require(!owner.hasEffect(MobEffects.SPEED) && !owner.hasEffect(timer), "Config OFF prevents new cigarette effects");

            long day = Math.floorDiv(server.overworld().getOverworldClockTime(), 24000L);
            var access = (MilkComboAccess) owner;
            access.sum1rem0d$lastMilkDay(day - 1); access.sum1rem0d$milkStreak(1);
            milk.addProperty("enabled", true); store.update(draft, store.revision());
            MilkComboContent.afterMilk(owner);
            require(access.sum1rem0d$milkStreak() == 2 && owner.getEffect(MobEffects.HASTE).getAmplifier() == 1,
                    "Milk on consecutive days raises the health buff tier");
            MilkComboContent.afterMilk(owner);
            require(access.sum1rem0d$milkStreak() == 2, "Multiple milk drinks on the same day do not increase the streak");
            access.sum1rem0d$lastMilkDay(day - 3); access.sum1rem0d$milkStreak(8);
            owner.removeEffect(MobEffects.HASTE);
            MilkComboContent.afterMilk(owner);
            require(access.sum1rem0d$milkStreak() == 1 && owner.getEffect(MobEffects.HASTE).getAmplifier() == 0,
                    "A missed calendar day resets the milk combo tier");
            access.sum1rem0d$lastMilkDay(day - 1); access.sum1rem0d$milkStreak(2);
            output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, owner.registryAccess());
            owner.saveWithoutId(output);
            reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, reloaded.registryAccess(), output.buildResult()));
            require(((MilkComboAccess) reloaded).sum1rem0d$milkStreak() == 2,
                    "Milk streak and last-drink day persist across reconnects");
            respawned.restoreFrom(owner, false);
            require(((MilkComboAccess) respawned).sum1rem0d$milkStreak() == 2,
                    "Milk streak persists through native death respawn transfer");

            var bottledMilk = FoodContent.item("bottled_milk");
            require(bottledMilk.getDefaultInstance().getMaxStackSize() == 16
                    && bottledMilk.getDefaultInstance().get(DataComponents.USE_REMAINDER) != null,
                    "Bottled milk stacks to sixteen and returns a glass bottle");
            var bottleInput = CraftingInput.of(2, 2, List.of(new ItemStack(Items.MILK_BUCKET), new ItemStack(Items.GLASS_BOTTLE), new ItemStack(Items.GLASS_BOTTLE), new ItemStack(Items.GLASS_BOTTLE)));
            var bottleRecipe = new FoodRecipe("bottled_milk");
            require(bottleRecipe.matches(bottleInput, server.overworld()) && bottleRecipe.assemble(bottleInput).getCount() == 3,
                    "One milk bucket and three glass bottles make three bottled milk");
            var bundleInput = CraftingInput.of(2, 1, List.of(new ItemStack(Items.BUNDLE), new ItemStack(bottledMilk)));
            var bundleRecipe = new FoodRecipe("milk_bundle");
            var milkBundle = bundleRecipe.assemble(bundleInput);
            BundleContents bundleContents = milkBundle.get(DataComponents.BUNDLE_CONTENTS);
            require(bundleRecipe.matches(bundleInput, server.overworld()) && bundleContents != null
                    && bundleContents.size() == 1 && bundleContents.items().get(0).item().value() == bottledMilk,
                    "Milk-only bundle crafting preserves the inserted bottle as contents");
            var persistedBundle = milkBundle.copy();
            var bundleItem = (MilkBundleItem) FoodContent.item("milk_bundle");
            var destination = new SimpleContainer(1);
            var emptySlot = new Slot(destination, 0, 0, 0);
            require(bundleItem.overrideStackedOnOther(milkBundle, emptySlot, ClickAction.SECONDARY, owner)
                    && destination.getItem(0).is(bottledMilk)
                    && milkBundle.get(DataComponents.BUNDLE_CONTENTS).isEmpty(),
                    "Milk-only bundle extracts a bottled milk into an empty slot");
            var source = new SimpleContainer(1);
            source.setItem(0, new ItemStack(bottledMilk));
            var sourceSlot = new Slot(source, 0, 0, 0);
            require(bundleItem.overrideStackedOnOther(milkBundle, sourceSlot, ClickAction.PRIMARY, owner)
                    && source.getItem(0).isEmpty()
                    && milkBundle.get(DataComponents.BUNDLE_CONTENTS).items().get(0).item().value() == bottledMilk,
                    "Milk-only bundle accepts the permitted bottle through vanilla bundle transfer");
            var prohibited = new SimpleContainer(1);
            prohibited.setItem(0, new ItemStack(Items.DIAMOND));
            require(!bundleItem.overrideStackedOnOther(milkBundle, new Slot(prohibited, 0, 0, 0), ClickAction.PRIMARY, owner)
                    && prohibited.getItem(0).is(Items.DIAMOND),
                    "Milk-only bundle refuses unrelated items without consuming them");
            source.setItem(0, new ItemStack(bottledMilk));
            bottle.addProperty("milk_bundle_enabled", false); store.update(draft, store.revision());
            require(!bundleItem.overrideStackedOnOther(milkBundle, sourceSlot, ClickAction.PRIMARY, owner)
                    && source.getItem(0).is(bottledMilk)
                    && !milkBundle.get(DataComponents.BUNDLE_CONTENTS).isEmpty(),
                    "Config OFF blocks new insertion while keeping stored contents intact");
            var recovery = new SimpleContainer(1);
            var recoverySlot = new Slot(recovery, 0, 0, 0);
            require(bundleItem.overrideStackedOnOther(milkBundle, recoverySlot, ClickAction.SECONDARY, owner)
                    && recovery.getItem(0).is(bottledMilk),
                    "Config OFF still lets players recover previously stored bottled milk");
            bottle.addProperty("milk_bundle_enabled", true); store.update(draft, store.revision());
            var occupiedBundle = new ItemStack(Items.BUNDLE);
            occupiedBundle.set(DataComponents.BUNDLE_CONTENTS, bundleContents);
            var rejectsContentsLoss = CraftingInput.of(2, 1, List.of(occupiedBundle, new ItemStack(bottledMilk)));
            require(!bundleRecipe.matches(rejectsContentsLoss, server.overworld()),
                    "Milk bundle recipe rejects nonempty bundles instead of destroying their contents");
            var ops = net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, server.overworld().registryAccess());
            var encodedBundle = ItemStack.CODEC.encodeStart(ops, persistedBundle).getOrThrow();
            var loadedBundle = ItemStack.CODEC.parse(ops, encodedBundle).getOrThrow();
            require(loadedBundle.get(DataComponents.BUNDLE_CONTENTS).size() == 1
                    && loadedBundle.get(DataComponents.BUNDLE_CONTENTS).items().get(0).item().value() == bottledMilk,
                    "Milk-only bundle contents survive native ItemStack save/load");

            FoodContent.consumeRawCatMeat(owner);
            require(owner.hasEffect(MobEffects.POISON) && owner.hasEffect(MobEffects.HUNGER),
                    "Raw cat meat applies configured poison and hunger chances");
            require(FoodContent.catMeatDropCount(net.minecraft.util.RandomSource.create(42L)) == 1,
                    "Configured cat death produces the expected meat drop count");
            meat.addProperty("enabled", false); store.update(draft, store.revision());
            owner.removeAllEffects(); FoodContent.consumeRawCatMeat(owner);
            require(!owner.hasEffect(MobEffects.POISON) && !owner.hasEffect(MobEffects.HUNGER),
                    "Config OFF suppresses raw cat meat drawbacks");
            require(FoodContent.catMeatDropCount(net.minecraft.util.RandomSource.create(42L)) == 0,
                    "Config OFF suppresses new cat meat drops");
            passed.accept("Cigarette delay save/death/Config OFF, milk combo streak/reset/persistence, bottled milk bundle conservation, and cat meat effects");
        } finally {
            store.update(original, store.revision());
            owner.discard(); reloaded.discard(); respawned.discard();
        }
    }
}
