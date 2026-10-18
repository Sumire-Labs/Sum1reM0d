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
    private static volatile double activePufferScale = 5;
    private static CompletableFuture<?> reload;
    private static volatile boolean quietReload;
    private static final java.util.Map<String,byte[]> programmerArt = new java.util.HashMap<>();
    static synchronized byte[] programmerArtTexture() throws IOException { return programmerArtTexture("textures/block/netherrack.png"); }
    static synchronized byte[] programmerArtTexture(String texturePath) throws IOException {
        byte[] cached = programmerArt.get(texturePath);
        if (cached != null) return cached;
        var pack=Minecraft.getInstance().getResourcePackRepository().getPack("programmer_art");
        if(pack==null) throw new IOException("Built-in programmer_art pack is unavailable");
        //? if >=26.3 {
        try(var packs=pack.open()) {
            for(var resources:packs.toList()) try(resources) {
                var supplier=resources.getResource(net.minecraft.server.packs.PackType.CLIENT_RESOURCES,Identifier.withDefaultNamespace(texturePath));
                if(supplier!=null) try(var input=supplier.get()) {byte[] bytes=input.readAllBytes();programmerArt.put(texturePath,bytes);return bytes;}
            }
        }
        //?} else {
        /*try(var resources=pack.open()) {
            var supplier=resources.getResource(net.minecraft.server.packs.PackType.CLIENT_RESOURCES,Identifier.withDefaultNamespace(texturePath));
            if(supplier!=null) try(var input=supplier.get()) {byte[] bytes=input.readAllBytes();programmerArt.put(texturePath,bytes);return bytes;}
        }
        *///?}
        throw new IOException("Programmer Art netherrack texture is unavailable");
    }
    private DailyTextures() {}
    static synchronized CompletableFuture<?> verifyReload(Minecraft mc,int mask) { active=mask; return reloadResources(mc); }
    private static CompletableFuture<?> reloadResources(Minecraft mc) {
        quietReload = true;
        reload = mc.reloadResourcePacks();
        return reload;
    }
    public static boolean quietReloadActive() { return quietReload; }
    public static void reloadOverlayFinished() { quietReload = false; }
    public static void tick(Minecraft mc) {
        var personal = com.sumirelabs.sum1rem0d.config.PersonalConfig.store();
        boolean pufferfish = personal.enabled("programmer_pufferfish");
        double wantedPufferScale = pufferfish ? Math.max(1, Math.min(10, personal.number("programmer_pufferfish", "hand_scale"))) : 5;
        boolean clock = personal.enabled("clock_ticks");
        int wanted = mc.level == null ? 0 : (ClientGameState.daily("retro_netherrack") ? 1 : 0) | (com.sumirelabs.sum1rem0d.config.ClientRules.enabled("ardite_disguise") ? 2 : 0) | (pufferfish ? 4 : 0) | (ClientGameState.daily("retro_cobblestone") ? 8 : 0) | (clock ? 16 : 0);
        if (wanted == active && wantedPufferScale == activePufferScale || reload != null && !reload.isDone() || mc.gui.overlay() != null) return;
        active = wanted;
        activePufferScale = wantedPufferScale;
        reloadResources(mc);
    }
    public static Resource replace(Identifier id, Resource original, net.minecraft.server.packs.resources.ResourceManager manager) {
        if((active&2)!=0&&id.equals(com.sumirelabs.sum1rem0d.game.ModContent.id("textures/item/netherite_paxel.png")))return manager.getResource(com.sumirelabs.sum1rem0d.game.ModContent.id("textures/item/manyullyn_paxel.png")).orElse(original);
        if (!id.getNamespace().equals("minecraft")) return original;
        String path = id.getPath(); int mask = active;
        if ((mask & 4) != 0 && path.equals("models/item/pufferfish.json")) {
            double scale = activePufferScale;
            String model = "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"minecraft:item/pufferfish\"},\"display\":{" +
                    "\"firstperson_righthand\":{\"scale\":[" + scale + "," + scale + "," + scale + "]}," +
                    "\"firstperson_lefthand\":{\"scale\":[" + scale + "," + scale + "," + scale + "]}," +
                    "\"thirdperson_righthand\":{\"scale\":[" + scale + "," + scale + "," + scale + "]}," +
                    "\"thirdperson_lefthand\":{\"scale\":[" + scale + "," + scale + "," + scale + "]}}}";
            byte[] json = model.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return new Resource(original.source(), () -> new ByteArrayInputStream(json));
        }
        if (!path.endsWith(".png")) return original;
        if ((mask & 4) != 0 && (path.equals("textures/entity/fish/pufferfish.png") || path.equals("textures/item/pufferfish.png"))) {
            try {
                byte[] texture = programmerArtTexture(path);
                return new Resource(original.source(), () -> new ByteArrayInputStream(texture));
            }
            catch (IOException unavailable) { return original; }
        }
        if ((mask & 8) != 0 && path.equals("textures/block/cobblestone.png"))
            return manager.getResource(com.sumirelabs.sum1rem0d.game.ModContent.id("textures/block/beta_cobblestone.png")).orElse(original);
        if ((mask & 16) != 0 && path.matches("textures/item/clock_[0-9]{2}\\.png"))
            return manager.getResource(com.sumirelabs.sum1rem0d.game.ModContent.id("textures/item/clock_ticks.png")).orElse(original);
        if ((mask & 1) != 0 && path.equals("textures/block/netherrack.png")) return new Resource(original.source(), () -> {
            return new ByteArrayInputStream(programmerArtTexture());
        });
        if ((mask & 2) == 0) return original;
        return DedicatedTextures.material(id,original,manager);
    }
}
