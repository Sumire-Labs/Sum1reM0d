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
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;

/** Actual client widgets, resources, book editor and server commerce payload round trip. */
final class RemainingSmoke {
 private static long started;private static int stage,ticks;private static boolean done,worldChatImeVerified;private static String typed;private static long walletBalanceBefore,walletBalanceAfterDeposit,ordersBeforeCheckout;private static JsonObject visualRules;private static final List<String> passed=new ArrayList<>();
 private static List<String> retainedSearch=List.of();private static JsonObject personalOriginal;
 private RemainingSmoke(){}
 private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 private static void press(Minecraft mc,String prefix){var button=mc.gui.screen().children().stream().filter(c->c instanceof Button b && b.getMessage().getString().startsWith(prefix)).map(c->(Button)c).findFirst().orElseThrow();require(button.active,"Enabled widget "+prefix);button.onPress(new KeyEvent(InputConstants.KEY_RETURN,0,0));}
 private static boolean enabled(Minecraft mc,String prefix){return mc.gui.screen().children().stream().anyMatch(c->c instanceof Button b && b.getMessage().getString().startsWith(prefix)&&b.active);}
 private static void next(){stage++;ticks=0;Sum1reM0d.LOGGER.info("REMAINING_SMOKE stage {}",stage);}
 private static void command(String phase){var q=new JsonObject();q.addProperty("phase",phase);ShopClient.request("verify",q);}
 private static void screenshot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"sumire-remaining-"+name+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});}
 private static Object field(Object object,String name)throws Exception{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
 private static List<String> visibleKeys(ShopScreen screen)throws Exception{@SuppressWarnings("unchecked")var items=(List<ItemStack>)field(screen,"visible");return items.stream().map(ShopCatalog::key).toList();}
 private static int addButtons(Minecraft mc){return (int)mc.gui.screen().children().stream().filter(c->c instanceof Button b&&b.getMessage().getString().equals("追加")).count();}
 private static void type(EditBox box,String value){box.setFocused(true);box.setValue(value);}
 private static void clickFirstSuggestion(ShopScreen screen,EditBox input){screen.mouseClicked(new MouseButtonEvent(input.getX()+10,input.getY()+input.getHeight()+8,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0)),false);}
 private static void verifyWorldChatIme(Minecraft mc)throws Exception{
  personalOriginal=PersonalConfig.store().snapshot();var draft=personalOriginal.deepCopy();draft.getAsJsonObject("features").getAsJsonObject("japanese_ime").addProperty("enabled",true);PersonalConfig.store().update(draft,PersonalConfig.store().revision());
  var previous=mc.gui.screen();
  var chat=new ChatScreen("",false,false);
  mc.gui.setScreen(chat);
  try{var input=chat.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();type(input,"日本語:");input.setCursorPosition(input.getValue().length());require(input.preeditUpdated(new PreeditEvent("かな漢字",4,List.of("かな","漢字"),1)),"World ChatScreen accepts native IME preedit");require(input.getValue().equals("日本語:"),"ChatScreen preedit remains uncommitted");require(input.preeditUpdated(null),"ChatScreen clears native IME preedit");"仮名".codePoints().forEach(point->require(input.charTyped(new CharacterEvent(point)),"ChatScreen accepts committed IME characters"));require(input.getValue().equals("日本語:仮名"),"ChatScreen commits the Japanese string exactly once");passed.add("World-backed native ChatScreen IME composition is separate and committed text is inserted once");}
  finally{mc.gui.setScreen(previous);PersonalConfig.store().update(personalOriginal,PersonalConfig.store().revision());personalOriginal=null;}
 }
 static void tick(Minecraft mc){
  if(done){if(++ticks>40)mc.stop();return;}
  if(!Boolean.getBoolean("sum1rem0d.remainingSmoke") || !mc.isGameLoadFinished() || mc.gui.overlay()!=null)return;
  try{
   if(started==0){started=System.nanoTime();String address=System.getProperty("sum1rem0d.networkAddress","127.0.0.1:25596");ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString(address),new ServerData("Remaining verification",address,ServerData.Type.OTHER),false,null);return;}
   if(System.nanoTime()-started>220_000_000_000L || mc.gui.screen() instanceof DisconnectedScreen)throw new AssertionError("Client timeout/disconnect stage="+stage+" message="+ShopClient.message);
   if(!EquipmentDefaultsClient.received || mc.player==null || !ClientRules.received() || !ClientGameState.received() || !ClientRules.enabled("sumzon"))return;ticks++;
   switch(stage){
    case 0->{
     if(!worldChatImeVerified){verifyWorldChatIme(mc);worldChatImeVerified=true;}
     if(!MixedColorLiveChecks.tick(mc,passed::add))return;
     if(!ArtworkPreview.tick(mc,passed::add))return;
     require(new ItemStack(ExtraContent.item("redstone_pickaxe")).getMaxDamage()==32,"Server defaults replace intentionally mismatched client redstone pickaxe durability");
     require(PaxelContent.item("diamond").getDefaultInstance().getMaxDamage()==Items.DIAMOND_PICKAXE.getDefaultInstance().getMaxDamage(),"Server defaults replace intentionally mismatched client diamond paxel durability");
     passed.add("Authoritative redstone pickaxe and diamond paxel defaults replace differing client durability settings");
     RemainingVisualChecks.verify(mc,passed::add);
     for(String id:ExtraEquipment.ITEMS){try(var in=mc.getResourceManager().getResource(ModContent.id("textures/item/"+id+".png")).orElseThrow().open()){var image=ImageIO.read(in);require(image.getWidth()==16 && image.getHeight()==16,"16x16 native generated sprite "+id);}var model=mc.getModelManager().getItemModel(ModContent.id(id));require(model!=null&&!model.getClass().getSimpleName().contains("Missing"),"Baked equipment model "+id);}
     for(String id:List.of("mixed_wool","mixed_bed","piston_base","tamed_book"))require(!mc.getModelManager().getItemModel(ModContent.id(id)).getClass().getSimpleName().contains("Missing"),"Extra item model "+id);
     passed.add("All 27 dedicated 16x16 equipment sprites and 31 extra item models resolve and bake");mc.gui.setScreen(new ShopScreen(null));next();
    }
    case 1->{if(!ShopClient.ready()||ShopClient.pending()||ticks<10)return;if(personalOriginal==null){personalOriginal=PersonalConfig.store().snapshot();var suggestions=personalOriginal.deepCopy();suggestions.getAsJsonObject("features").getAsJsonObject("wiki").addProperty("search_suggestions",true);PersonalConfig.store().update(suggestions,PersonalConfig.store().revision());}var screen=(ShopScreen)mc.gui.screen();var all=visibleKeys(screen);int capacity=((Integer)field(screen,"productRows"))*2;int expected=Math.min(capacity,all.size());int actual=addButtons(mc);require(actual==expected&&actual>=Math.min(10,capacity),"Dense catalog fills the viewport's calculated two-column capacity (rows="+(capacity/2)+", visible="+all.size()+", buttons="+actual+")");press(mc,"全て");require(enabled(mc,"▶"),"Large block category has an additional page");press(mc,"▶");var input=mc.gui.screen().children().stream().filter(c->c instanceof EditBox).map(EditBox.class::cast).findFirst().orElseThrow();type(input,"minecraft:diamond");var popup=(SearchSuggestionPopup)field(screen,"suggestions");require(popup.visible(),"Global search offers items beyond the current category and page");clickFirstSuggestion(screen,input);retainedSearch=visibleKeys(screen);require(!retainedSearch.isEmpty()&&retainedSearch.contains("minecraft:diamond")&&field(screen,"category").equals(0)&&field(screen,"page").equals(0),"Suggestion search opens a matching item outside the selected category/page");var count=addButtons(mc);ShopClient.refresh();require(ShopClient.pending()&&visibleKeys(screen).equals(retainedSearch)&&addButtons(mc)==count,"Live refresh leaves visible products in place while the real request is pending");var boxes=screen.children().stream().filter(c->c instanceof EditBox).map(EditBox.class::cast).toList();boxes.get(1).setValue("2");next();}
    case 2->{if(ticks<5||ShopClient.pending()||!enabled(mc,"追加"))return;var screen=(ShopScreen)mc.gui.screen();require(visibleKeys(screen).equals(retainedSearch)&&addButtons(mc)>0,"Server refresh preserves the same global search results after completion");passed.add("Global search crosses page/category, dense multi-column rows render, and native refresh preserves results without flicker");screenshot(mc,"catalog");press(mc,"追加");press(mc,"カート");next();}
    case 3->{if(ticks<5||ShopClient.pending()||!enabled(mc,"注文する"))return;screenshot(mc,"cart");ordersBeforeCheckout=ShopClient.number("ordersTotal");press(mc,"注文する");next();}
    case 4->{if(ShopClient.pending()||ticks<10)return;require(ShopClient.number("ordersTotal")>ordersBeforeCheckout,"Server checkout created an order [action="+ShopClient.action+", applied="+ShopClient.applied+", message="+ShopClient.message+", orders="+ShopClient.number("ordersTotal")+", balance="+ShopClient.number("balance")+"]");screenshot(mc,"delivery");press(mc,"受取");next();}
    case 5->{if(ShopClient.pending()||ticks<10)return;require(mc.player.getInventory().countItem(Items.DIAMOND)==2,"Exactly two diamonds delivered over native network");passed.add("Native catalog search, quantity, cart checkout and order claim deliver exactly two diamonds");press(mc,"プラン");next();}
    case 6->{if(ticks<5||ShopClient.pending()||!enabled(mc,"契約/延長"))return;press(mc,"契約/延長");next();}
    case 7->{if(ShopClient.pending()||ticks<10)return;require(ShopClient.number("adUntil")>ShopClient.number("now")&&ShopClient.adPremium(),"Authoritative ad premium synchronized");var buttons=mc.gui.screen().children().stream().filter(c->c instanceof Button b&&b.getMessage().getString().startsWith("契約/延長")).map(c->(Button)c).toList();buttons.get(1).onPress(new KeyEvent(InputConstants.KEY_RETURN,0,0));next();}
    case 8->{if(ShopClient.pending()||ticks<10)return;require(ShopClient.number("primeUntil")>ShopClient.number("now")&&!ShopClient.flag("primeAuto"),"Prime active, auto default OFF");screenshot(mc,"plans");passed.add("Native premium/Prime purchase widgets, server contracts and opt-in renewal state");visualRules=ClientRules.snapshot();var premiumRules=visualRules.deepCopy();premiumRules.getAsJsonObject("features").getAsJsonObject("advertisements").addProperty("enabled",true);premiumRules.getAsJsonObject("features").getAsJsonObject("advertisements").addProperty("enchanting",true);ClientRules.accept(new com.sumirelabs.sum1rem0d.network.ConfigStatePayload(ClientRules.revision,ClientRules.editable,premiumRules.toString(),"",0,false));mc.gui.toastManager().clear();mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.EnchantmentScreen(new net.minecraft.world.inventory.EnchantmentMenu(0,mc.player.getInventory()),mc.player.getInventory(),net.minecraft.network.chat.Component.literal("Premium advertisement verification")));command("normal_book");next();}
    case 9->{if(ticks<8||ShopClient.pending()||!mc.player.getMainHandItem().is(Items.WRITABLE_BOOK))return;require(BrowserOverlays.adSession()==null,"Paid premium prevents browser creation on actual enchantment screen");passed.add("Paid premium prevents Mod advertisements on actual native enchantment screen");ClientRules.accept(new com.sumirelabs.sum1rem0d.network.ConfigStatePayload(ClientRules.revision,ClientRules.editable,visualRules.toString(),"",0,false));var book=mc.player.getMainHandItem();mc.gui.setScreen(null);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);require(mc.gui.screen() instanceof BookEditScreen,"Native right-click opens writable editor");((BookEditAccess)mc.gui.screen()).sum1rem0d$page().setValue("Existing text:");next();}
    case 10->{if(ticks<45)return;typed=((BookEditAccess)mc.gui.screen()).sum1rem0d$page().getValue();require(typed.startsWith("Existing text:")&&typed.length()>=20,"Vanilla book appends random text without deleting content");mc.gui.screen().onClose();command("tamed_book");next();}
    case 11->{if(ShopClient.pending()||!mc.player.getMainHandItem().is(ExtraContent.item("tamed_book")))return;var book=mc.player.getMainHandItem();mc.gui.setScreen(null);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);require(mc.gui.screen() instanceof BookEditScreen,"Native right-click opens writable editor");((BookEditAccess)mc.gui.screen()).sum1rem0d$page().setValue("Tamed manual text");next();}
    case 12->{if(ticks<45)return;require(((BookEditAccess)mc.gui.screen()).sum1rem0d$page().getValue().equals("Tamed manual text"),"Tamed book remains normally editable");screenshot(mc,"tamed-book");press(mc,net.minecraft.network.chat.Component.translatable("gui.done").getString());next();}
    case 13->{if(ticks<20)return;require(mc.player.getMainHandItem().get(DataComponents.WRITABLE_BOOK_CONTENT).pages().getFirst().raw().equals("Tamed manual text"),"Native edit packet saves tamed book on server");passed.add("Vanilla book random input preserves existing text; tamed book edits save through native server packets");command("creative_on");next();}
    case 14->{if(ShopClient.pending())return;require(mc.player.isCreative(),"Server switches the live test player to Creative for commerce checks");mc.gui.setScreen(new ShopScreen(null));next();}
    case 15->{if(!ShopClient.ready()||ShopClient.pending()||ticks<8)return;var screen=(ShopScreen)mc.gui.screen();var boxes=screen.children().stream().filter(c->c instanceof EditBox).map(EditBox.class::cast).toList();type(boxes.getFirst(),"minecraft:diamond");require(visibleKeys(screen).getFirst().equals("minecraft:diamond")&&enabled(mc,"追加"),"Creative player can select the exact search result in the live storefront");boxes.get(1).setValue("1");press(mc,"追加");press(mc,"カート");require(enabled(mc,"注文する"),"Creative checkout widget is server-config enabled");ordersBeforeCheckout=ShopClient.number("ordersTotal");press(mc,"注文する");next();}
    case 16->{if(ShopClient.pending()||ticks<10)return;if(ShopClient.number("ordersTotal")<=ordersBeforeCheckout){if(ticks<80)return;throw new AssertionError("Creative checkout did not create an order [action="+ShopClient.action+", applied="+ShopClient.applied+", message="+ShopClient.message+", balance="+ShopClient.number("balance")+"]");}var screen=(ShopScreen)mc.gui.screen();if(!field(screen,"tab").equals(2)){press(mc,"注文・受取");return;}if(!enabled(mc,"受取"))return;press(mc,"受取");next();}
    case 17->{if(ShopClient.pending()||ticks<10)return;require(mc.player.getInventory().countItem(Items.DIAMOND)==3,"Creative purchase completes through the native order and delivery path");passed.add("Creative Sumzon access supports a server-accepted purchase and delivery");command("wallet_seed");next();}
    case 18->{if(ShopClient.pending())return;mc.gui.setScreen(new WalletScreen(null));next();}
    case 19->{if(ticks<8)return;var economy=ClientGameState.snapshot().getAsJsonObject("economy");var players=economy.getAsJsonArray("players");require(players.size()==1&&players.get(0).getAsJsonObject().get("name").getAsString().equals("SumireNear"),"Wallet refresh includes the nearby player and excludes self and out-of-range players");require(enabled(mc,"SumireNear に取引依頼")&&enabled(mc,"所持コインを全て入金")&&enabled(mc,"札束（10）"),"Wallet shows selectable nearby and creative-enabled deposit/withdraw widgets");walletBalanceBefore=economy.get("balance").getAsLong();press(mc,"SumireNear に取引依頼");next();}
    case 20->{if(ticks<8)return;var verify=new JsonObject();verify.addProperty("phase","wallet_invite_check");ShopClient.request("verify",verify);next();}
    case 21->{if(ShopClient.pending())return;require(ShopClient.applied,"Server confirms the actual Wallet button selected the nearby recipient only");press(mc,"所持コインを全て入金");next();}
    case 22->{var economy=ClientGameState.snapshot().getAsJsonObject("economy");if(mc.player.getInventory().countItem(ModContent.coin())!=0||!economy.has("balance")||economy.get("balance").getAsLong()!=walletBalanceBefore+5)return;walletBalanceAfterDeposit=economy.get("balance").getAsLong();var amount=mc.gui.screen().children().stream().filter(c->c instanceof EditBox).map(EditBox.class::cast).findFirst().orElseThrow();amount.setValue("10");press(mc,"札束（10）");next();}
    case 23->{var economy=ClientGameState.snapshot().getAsJsonObject("economy");if(mc.player.getInventory().countItem(ModContent.coinRoll())!=1||!economy.has("balance")||economy.get("balance").getAsLong()!=walletBalanceAfterDeposit-10)return;passed.add("Creative Wallet nearby invite, coin deposit, and ten-coin withdrawal use live server responses");command("finish");next();}
    case 24->{if(ShopClient.pending())return;require(ShopClient.applied,"Server accepted final live verification: "+ShopClient.message);finish(mc,null);}
   }
  }catch(Throwable e){try{screenshot(mc,"failed-stage-"+stage);}catch(Throwable ignored){}Sum1reM0d.LOGGER.error("Remaining client failed stage {}",stage,e);finish(mc,e);}
 }
 private static void finish(Minecraft mc,Throwable failure){done=true;if(personalOriginal!=null)try{PersonalConfig.store().update(personalOriginal,PersonalConfig.store().revision());}catch(Exception restore){if(failure==null)failure=restore;}var result=new JsonObject();result.addProperty("success",failure==null);if(failure!=null)result.addProperty("failure",failure.toString());result.add("passed",new Gson().toJsonTree(passed));try{Files.writeString(mc.gameDirectory.toPath().resolve("sum1rem0d-remaining-client.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));}catch(Exception e){throw new IllegalStateException(e);}if(failure!=null)mc.stop();}
}
