package com.sumirelabs.sum1rem0d.client;
import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.game.*;
import com.sumirelabs.sum1rem0d.mixin.BookEditAccess;
import java.util.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;

/** Actual client widgets, resources, book editor and server commerce payload round trip. */
final class RemainingSmoke {
 private static long started;private static int stage,ticks;private static boolean done;private static String typed;private static JsonObject visualRules;private static final List<String> passed=new ArrayList<>();
 private RemainingSmoke(){}
 private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 private static void press(Minecraft mc,String prefix){var button=mc.gui.screen().children().stream().filter(c->c instanceof Button b && b.getMessage().getString().startsWith(prefix)).map(c->(Button)c).findFirst().orElseThrow();require(button.active,"Enabled widget "+prefix);button.onPress(new KeyEvent(InputConstants.KEY_RETURN,0,0));}
 private static boolean enabled(Minecraft mc,String prefix){return mc.gui.screen().children().stream().anyMatch(c->c instanceof Button b && b.getMessage().getString().startsWith(prefix)&&b.active);}
 private static void next(){stage++;ticks=0;}
 private static void command(String phase){var q=new JsonObject();q.addProperty("phase",phase);ShopClient.request("verify",q);}
 private static void screenshot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"sumire-remaining-"+name+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});}
 static void tick(Minecraft mc){
  if(done){if(++ticks>40)mc.stop();return;}
  if(!Boolean.getBoolean("sum1rem0d.remainingSmoke") || !mc.isGameLoadFinished() || mc.gui.overlay()!=null)return;
  try{
   if(started==0){started=System.nanoTime();String address=System.getProperty("sum1rem0d.networkAddress","127.0.0.1:25596");ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString(address),new ServerData("Remaining verification",address,ServerData.Type.OTHER),false,null);return;}
   if(System.nanoTime()-started>220_000_000_000L || mc.gui.screen() instanceof DisconnectedScreen)throw new AssertionError("Client timeout/disconnect stage="+stage+" message="+ShopClient.message);
   if(!EquipmentDefaultsClient.received || mc.player==null || !ClientRules.received() || !ClientGameState.received() || !ClientRules.enabled("sumzon"))return;ticks++;
   switch(stage){
    case 0->{
     if(!ArtworkPreview.tick(mc,passed::add))return;
     require(new ItemStack(ExtraContent.item("redstone_pickaxe")).getMaxDamage()==32,"Server defaults replace intentionally mismatched client durability");passed.add("Authoritative registered equipment defaults replace differing client settings");
     RemainingVisualChecks.verify(mc,passed::add);
     for(String id:ExtraEquipment.ITEMS){try(var in=mc.getResourceManager().getResource(ModContent.id("textures/item/"+id+".png")).orElseThrow().open()){var image=ImageIO.read(in);require(image.getWidth()==16 && image.getHeight()==16,"16x16 native generated sprite "+id);}var model=mc.getModelManager().getItemModel(ModContent.id(id));require(model!=null&&!model.getClass().getSimpleName().contains("Missing"),"Baked equipment model "+id);}
     for(String id:List.of("mixed_wool","mixed_bed","piston_base","tamed_book"))require(!mc.getModelManager().getItemModel(ModContent.id(id)).getClass().getSimpleName().contains("Missing"),"Extra item model "+id);
     passed.add("All 27 dedicated 16x16 equipment sprites and 31 extra item models resolve and bake");mc.gui.setScreen(new ShopScreen(null));next();
    }
    case 1->{if(!ShopClient.ready()||ShopClient.pending()||ticks<10)return;var boxes=mc.gui.screen().children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).toList();boxes.getFirst().setValue("minecraft:diamond");boxes.get(1).setValue("2");next();}
    case 2->{if(ticks<5||ShopClient.pending()||!enabled(mc,"追加"))return;screenshot(mc,"catalog");press(mc,"追加");press(mc,"カート");next();}
    case 3->{if(ticks<5||ShopClient.pending()||!enabled(mc,"注文する"))return;screenshot(mc,"cart");press(mc,"注文する");next();}
    case 4->{if(ShopClient.pending()||ticks<10)return;require(ShopClient.applied && ShopClient.state().getAsJsonArray("orders").size()==1,"Checkout reply");screenshot(mc,"delivery");press(mc,"受取");next();}
    case 5->{if(ShopClient.pending()||ticks<10)return;require(mc.player.getInventory().countItem(Items.DIAMOND)==2,"Exactly two diamonds delivered over native network");passed.add("Native catalog search, quantity, cart checkout and order claim deliver exactly two diamonds");press(mc,"プラン");next();}
    case 6->{if(ticks<5||ShopClient.pending()||!enabled(mc,"契約/延長"))return;press(mc,"契約/延長");next();}
    case 7->{if(ShopClient.pending()||ticks<10)return;require(ShopClient.number("adUntil")>ShopClient.number("now")&&ShopClient.adPremium(),"Authoritative ad premium synchronized");var buttons=mc.gui.screen().children().stream().filter(c->c instanceof Button b&&b.getMessage().getString().startsWith("契約/延長")).map(c->(Button)c).toList();buttons.get(1).onPress(new KeyEvent(InputConstants.KEY_RETURN,0,0));next();}
    case 8->{if(ShopClient.pending()||ticks<10)return;require(ShopClient.number("primeUntil")>ShopClient.number("now")&&!ShopClient.flag("primeAuto"),"Prime active, auto default OFF");screenshot(mc,"plans");passed.add("Native premium/Prime purchase widgets, server contracts and opt-in renewal state");visualRules=ClientRules.snapshot();var premiumRules=visualRules.deepCopy();premiumRules.getAsJsonObject("features").getAsJsonObject("advertisements").addProperty("enabled",true);premiumRules.getAsJsonObject("features").getAsJsonObject("advertisements").addProperty("enchanting",true);ClientRules.accept(new com.sumirelabs.sum1rem0d.network.ConfigStatePayload(ClientRules.revision,ClientRules.editable,premiumRules.toString(),"",0,false));mc.gui.toastManager().clear();mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.EnchantmentScreen(new net.minecraft.world.inventory.EnchantmentMenu(0,mc.player.getInventory()),mc.player.getInventory(),net.minecraft.network.chat.Component.literal("Premium advertisement verification")));command("normal_book");next();}
    case 9->{if(ticks<8||ShopClient.pending()||!mc.player.getMainHandItem().is(Items.WRITABLE_BOOK))return;require(BrowserOverlays.adSession()==null,"Paid premium prevents browser creation on actual enchantment screen");passed.add("Paid premium prevents Mod advertisements on actual native enchantment screen");ClientRules.accept(new com.sumirelabs.sum1rem0d.network.ConfigStatePayload(ClientRules.revision,ClientRules.editable,visualRules.toString(),"",0,false));var book=mc.player.getMainHandItem();mc.gui.setScreen(null);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);require(mc.gui.screen() instanceof BookEditScreen,"Native right-click opens writable editor");((BookEditAccess)mc.gui.screen()).sum1rem0d$page().setValue("Existing text:");next();}
    case 10->{if(ticks<45)return;typed=((BookEditAccess)mc.gui.screen()).sum1rem0d$page().getValue();require(typed.startsWith("Existing text:")&&typed.length()>14,"Vanilla book appends random text without deleting content");mc.gui.screen().onClose();command("tamed_book");next();}
    case 11->{if(ShopClient.pending()||!mc.player.getMainHandItem().is(ExtraContent.item("tamed_book")))return;var book=mc.player.getMainHandItem();mc.gui.setScreen(null);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);require(mc.gui.screen() instanceof BookEditScreen,"Native right-click opens writable editor");((BookEditAccess)mc.gui.screen()).sum1rem0d$page().setValue("Tamed manual text");next();}
    case 12->{if(ticks<45)return;require(((BookEditAccess)mc.gui.screen()).sum1rem0d$page().getValue().equals("Tamed manual text"),"Tamed book remains normally editable");screenshot(mc,"tamed-book");press(mc,net.minecraft.network.chat.Component.translatable("gui.done").getString());next();}
    case 13->{if(ticks<20)return;require(mc.player.getMainHandItem().get(DataComponents.WRITABLE_BOOK_CONTENT).pages().getFirst().raw().equals("Tamed manual text"),"Native edit packet saves tamed book on server");passed.add("Vanilla book random input preserves existing text; tamed book edits save through native server packets");command("finish");next();}
    case 14->{if(ShopClient.pending())return;require(ShopClient.applied,"Server accepted final delivery and contracts: "+ShopClient.message);finish(mc,null);}
   }
  }catch(Throwable e){Sum1reM0d.LOGGER.error("Remaining client failed stage {}",stage,e);finish(mc,e);}
 }
 private static void finish(Minecraft mc,Throwable failure){done=true;var result=new JsonObject();result.addProperty("success",failure==null);if(failure!=null)result.addProperty("failure",failure.toString());result.add("passed",new Gson().toJsonTree(passed));try{Files.writeString(mc.gameDirectory.toPath().resolve("sum1rem0d-remaining-client.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));}catch(Exception e){throw new IllegalStateException(e);}if(failure!=null)mc.stop();}
}
