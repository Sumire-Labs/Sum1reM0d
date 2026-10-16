package com.sumirelabs.sum1rem0d.game;
import java.util.Set;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.entity.ai.village.poi.*;
import net.minecraft.world.level.block.Blocks;
public final class PathFarmer {
 public static final Identifier ID=ModContent.id("farmer_path");
 public static final ResourceKey<PoiType> KEY=ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE,ID);
 private PathFarmer() {}
 public static PoiType create() { return new PoiType(Set.copyOf(Blocks.DIRT_PATH.getStateDefinition().getPossibleStates()),1,1); }
 public static void registerFabric() { Registry.register(BuiltInRegistries.POINT_OF_INTEREST_TYPE,ID,create()); com.sumirelabs.sum1rem0d.mixin.PathPoiAccess.sum1rem0d$register(BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(KEY),create().matchingStates()); }
}
