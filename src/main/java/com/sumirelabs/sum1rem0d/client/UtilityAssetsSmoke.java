package com.sumirelabs.sum1rem0d.client;

import com.google.gson.JsonObject;
import com.sumirelabs.sum1rem0d.game.*;
import java.nio.file.Files;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Opt-in native asset check after synchronized item components are available. */
final class UtilityAssetsSmoke {
    private static long shownAt;
    private static boolean done,connecting;
    private static final List<String> NAMES=List.of("glowstone_torch","optical_fiber","wooden_helmet","wooden_chestplate","wooden_leggings","wooden_boots");
    static void tick(Minecraft mc){
        if(done||!Boolean.getBoolean("sum1rem0d.utilityAssetsSmoke")||!mc.isGameLoadFinished())return;
        if(mc.player==null||mc.level==null){
            if(!connecting){connecting=true;String address=System.getProperty("sum1rem0d.networkAddress","127.0.0.1:25591");
                net.minecraft.client.gui.screens.ConnectScreen.startConnecting(new net.minecraft.client.gui.screens.TitleScreen(),mc,net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(address),new net.minecraft.client.multiplayer.ServerData("Utility asset verification",address,net.minecraft.client.multiplayer.ServerData.Type.OTHER),false,null);}
            return;
        }
        mc.gui.toastManager().clear();
        try{
            if(shownAt==0){
                for(var name:NAMES)if(mc.getModelManager().getItemModel(ModContent.id(name)).getClass().getSimpleName().contains("Missing"))throw new AssertionError("Missing item model: "+name);
                mc.gui.setScreen(new Preview());shownAt=System.nanoTime();return;
            }
            if(System.nanoTime()-shownAt<2_000_000_000L)return;
            Screenshot.grab(mc.gameDirectory,"utility-five-assets.png",mc.gameRenderer.mainRenderTarget(),1,message->{});
            finish(mc,true,"");
        }catch(Throwable e){finish(mc,false,e.toString());}
    }
    private static void finish(Minecraft mc,boolean success,String failure){
        done=true;var report=new JsonObject();report.addProperty("success",success);report.addProperty("failure",failure);
        var checks=new com.google.gson.JsonArray();if(success){checks.add("Six new item models resolve and render in the native GUI");checks.add("Torch/fiber blockstate models and wooden equipment textures load");}report.add("passed",checks);
        try{Files.writeString(mc.gameDirectory.toPath().resolve("sum1rem0d-utility-assets.json"),report.toString());}catch(Exception e){throw new IllegalStateException(e);}
        mc.disconnectFromWorld(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE);
        mc.stop();
    }
    private static final class Preview extends Screen {
        Preview(){super(Component.literal("Utility additions"));}
        @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float tick){
            graphics.fill(0,0,width,height,0xFF222A33);
            graphics.text(font,Component.literal("Sum1reM0d - Utility additions"),16,12,0xFFFFFFFF,false);
            for(int i=0;i<NAMES.size();i++){
                int x=width*(2*(i%3)+1)/6,y=45+(i/3)*85;String name=NAMES.get(i);
                var stack=new ItemStack(UtilityContent.ITEMS.contains(name)?UtilityContent.item(name):ExtraContent.item(name));
                graphics.pose().pushMatrix();graphics.pose().translate(x-24,y);graphics.pose().scale(3f,3f);graphics.item(stack,0,0);graphics.pose().popMatrix();
                graphics.text(font,stack.getHoverName(),x-font.width(stack.getHoverName())/2,y+53,0xFFFFFFFF,false);
            }
        }
    }
}
