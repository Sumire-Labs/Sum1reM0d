package net.dimaskama.mcef.platform;

import net.dimaskama.mcef.impl.MCEFModern;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricBootstrap implements ClientModInitializer {
    @Override public void onInitializeClient() { MCEFModern.configure(FabricLoader.getInstance().getConfigDir()); }
}
