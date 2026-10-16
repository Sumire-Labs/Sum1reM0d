package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.ClientGameState;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import java.io.*;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;

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
        int wanted = mc.level == null ? 0 : (ClientGameState.daily("retro_netherrack") ? 1 : 0) | (ClientGameState.daily("ardite_disguise") ? 2 : 0);
        if (wanted == active || reload != null && !reload.isDone() || mc.gui.overlay() != null) return;
        active = wanted;
        reload = mc.reloadResourcePacks();
    }
    public static Resource replace(Identifier id, Resource original) {
        if (!id.getNamespace().equals("minecraft") || !id.getPath().endsWith(".png")) return original;
        String path = id.getPath(); int mask = active;
        if ((mask & 1) != 0 && path.equals("textures/block/netherrack.png")) return new Resource(original.source(), () -> {
            return new ByteArrayInputStream(programmerArtTexture());
        });
        boolean ardite = path.startsWith("textures/block/ancient_debris_") || path.equals("textures/item/netherite_scrap.png");
        boolean manyullyn = path.contains("netherite") && !path.contains("scrap");
        if ((mask & 2) == 0 || !(ardite || manyullyn)) return original;
        return new Resource(original.source(), () -> {
            try (var input = original.open()) {
                var image = ImageIO.read(input); if (image == null) throw new IOException("Invalid texture " + id);
                for (int y=0;y<image.getHeight();y++) for (int x=0;x<image.getWidth();x++) {
                    int c=image.getRGB(x,y), a=c>>>24, red=c>>16&255, green=c>>8&255, blue=c&255;
                    // Preserve brown wood handles, diamonds, and transparent pixels in resource packs.
                    if (a==0 || manyullyn && (red>green*1.35 && green>blue*1.25 || Math.max(red,Math.max(green,blue))-Math.min(red,Math.min(green,blue))>65)) continue;
                    int light=(red*3+green*6+blue)/10;
                    image.setRGB(x,y,a<<24 | (ardite ? Math.min(255,45+light*2)<<16 | Math.min(255,12+light)<<8 | Math.min(255,light/3) : Math.min(255,20+light)<<16 | Math.min(255,8+light*2/3)<<8 | Math.min(255,40+light*3/2)));
                }
                var output = new ByteArrayOutputStream(); ImageIO.write(image,"PNG",output); return new ByteArrayInputStream(output.toByteArray());
            }
        }, original::metadata);
    }
}
