package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.PersonalConfig;
import net.minecraft.client.gui.screens.Screen;

public final class ViewerBlocker {
    private ViewerBlocker() {}
    public static boolean blocked(String mod) {
        if (!PersonalConfig.viewersBlocked()) return false;
        var targets = PersonalConfig.store().list("recipe_viewers", "target_mods");
        if (targets.contains(mod)) return true;
        if (mod.equals("roughlyenoughitems") && targets.contains("rei")) return true;
        if (mod.equals("emi") && (targets.contains("tmrv") || targets.contains("toomanyrecipeviewers"))) {
            //? if fabric {
            return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("toomanyrecipeviewers") || net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("tmrv");
            //?} elif neoforge {
            /*return net.neoforged.fml.ModList.get().isLoaded("toomanyrecipeviewers") || net.neoforged.fml.ModList.get().isLoaded("tmrv");
            *///?}
        }
        return false;
    }
    public static boolean screen(Screen screen) {
        if (screen == null) return false;
        String name = screen.getClass().getName();
        return name.startsWith("mezz.jei.") && blocked("jei") || name.startsWith("me.shedaniel.rei.") && blocked("roughlyenoughitems") || name.startsWith("dev.emi.emi.") && blocked("emi");
    }
}
