package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.ClientGameState;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import java.io.*;
import java.util.concurrent.CompletableFuture;

/** Runtime palette translation preserves resource-pack detail, animation and equipment models. */
public final class DailyTextures {
    private static volatile int active;
    private static CompletableFuture<?> reload;
    private static volatile byte[] programmerArt;
    static synchronized byte[] programmerArtTexture() throws IOException {
        if (programmerArt != null) return programmerArt;
        var pack=Minecraft.getInstance().getResourcePackRepository().getPack("programmer_art");
        if(pack==null) throw new IOException("Built-in programmer_art pack is unavailable");
        //? if >=26.3 {
        try(var packs=pack.open()) {
            for(var resources:packs.toList()) try(resources) {
                var supplier=resources.getResource(net.minecraft.server.packs.PackType.CLIENT_RESOURCES,Identifier.withDefaultNamespace("textures/block/netherrack.png"));
                if(supplier!=null) try(var input=supplier.get()) {programmerArt=input.readAllBytes();return programmerArt;}
            }
        }
        //?} else {
        /*try(var resources=pack.open()) {
            var supplier=resources.getResource(net.minecraft.server.packs.PackType.CLIENT_RESOURCES,Identifier.withDefaultNamespace("textures/block/netherrack.png"));
            if(supplier!=null) try(var input=supplier.get()) {programmerArt=input.readAllBytes();return programmerArt;}
        }
        *///?}
        throw new IOException("Programmer Art netherrack texture is unavailable");
    }
    private DailyTextures() {}
    static CompletableFuture<?> verifyReload(Minecraft mc,int mask) { active=mask; reload=mc.reloadResourcePacks();return reload; }
    public static void tick(Minecraft mc) {
        int wanted = mc.level == null ? 0 : (ClientGameState.daily("retro_netherrack") ? 1 : 0) | (com.sumirelabs.sum1rem0d.config.ClientRules.enabled("ardite_disguise") ? 2 : 0);
        if (wanted == active || reload != null && !reload.isDone() || mc.gui.overlay() != null) return;
        active = wanted;
        reload = mc.reloadResourcePacks();
    }
    public static Resource replace(Identifier id, Resource original, net.minecraft.server.packs.resources.ResourceManager manager) {
        if (!id.getNamespace().equals("minecraft") || !id.getPath().endsWith(".png")) return original;
        String path = id.getPath(); int mask = active;
        if ((mask & 1) != 0 && path.equals("textures/block/netherrack.png")) return new Resource(original.source(), () -> {
            return new ByteArrayInputStream(programmerArtTexture());
        });
        if ((mask & 2) == 0) return original;
        return DedicatedTextures.material(id,original,manager);
    }
}
