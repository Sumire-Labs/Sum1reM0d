package net.dimaskama.mcef.platform;

import net.dimaskama.mcef.impl.MCEFModern;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

@Mod(value = "mcef_modern", dist = Dist.CLIENT)
public final class NeoForgeBootstrap {
    public NeoForgeBootstrap() { MCEFModern.configure(FMLPaths.CONFIGDIR.get()); }
}
