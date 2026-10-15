package com.sumirelabs.sum1rem0d.client;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.config.ClientRules;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import com.mojang.blaze3d.platform.InputConstants;

/** Actual native widgets, inventory clicks and server packets from two development clients. */
public final class EconomySmoke {
    private static long started,originalBalance;
    private static int phase=-2,step,ticks;
    private static boolean done;
    private static List<ItemStack> original;
    private static final List<String> passed=new ArrayList<>();
    private EconomySmoke(){}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void press(Screen screen,String text,boolean suffix){var b=screen.children().stream().filter(c->c instanceof Button button&&(suffix?button.getMessage().getString().endsWith(text):button.getMessage().getString().equals(text))).map(c->(Button)c).findFirst().orElseThrow();require(b.active,"Control enabled: "+text);b.onPress(new KeyEvent(InputConstants.KEY_RETURN,0,0));}
    private static void amount(Screen s,String text){s.children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).findFirst().orElseThrow().setValue(text);}
    private static void screenshot(Minecraft mc,String name){mc.gui.toastManager().clear();Screenshot.grab(mc.gameDirectory,"sumire-economy-"+name+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});}
    private static void ack(int value){WalletScreen.request("verify","",value,0);}
    public static void tick(Minecraft mc){
        if(!Boolean.getBoolean("sum1rem0d.economySmoke")||done||!mc.isGameLoadFinished())return;
        try{
            if(started==0){if(mc.gui.overlay()!=null)return;started=System.nanoTime();String address=System.getProperty("sum1rem0d.networkAddress","127.0.0.1:25589");ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString(address),new ServerData("Economy verification",address,ServerData.Type.OTHER),false,null);return;}
            if(mc.gui.screen() instanceof DisconnectedScreen)throw new AssertionError("Disconnected during economy test");
            if(System.nanoTime()-started>240_000_000_000L)throw new AssertionError("Economy client timeout phase="+phase+" step="+step);
            if(mc.player==null||!ClientRules.received())return;
            var state=WalletScreen.state();if(!state.has("verificationPhase"))return;int next=state.get("verificationPhase").getAsInt();if(next<0)return;if(next!=phase){phase=next;Sum1reM0d.LOGGER.info("ECONOMY_SMOKE phase {}",phase);step=0;ticks=0;}ticks++;
            boolean host=mc.player.getGameProfile().name().equals("Developer");long expected=host?75:45;
            switch(phase){
                case 0->{if(step!=0)return;original=new ArrayList<>();for(int i=0;i<mc.player.getInventory().getContainerSize();i++)original.add(mc.player.getInventory().getItem(i).copy());originalBalance=WalletScreen.number(state,"balance");passed.add("captured original connected inventory and exact balance");ack(0);step=1;}
                case 1->{if(!(mc.gui.screen() instanceof WalletScreen)){if(mc.gui.screen()!=null)return;mc.gui.setScreen(new WalletScreen(null));ticks=0;return;}if(ticks<15)return;
                    if(step==0){screenshot(mc,"wallet");require(WalletScreen.number(state,"balance")== (host?100:20),"Wallet receives seeded balance");passed.add("native wallet displays authoritative coins and nearby player");if(host){press(mc.gui.screen()," に取引依頼",true);step=1;}}
                    if(!host&&step==0&&state.has("inviteRevision")){screenshot(mc,"invitation");press(mc.gui.screen(),"依頼を承諾",false);passed.add("recipient explicitly accepts invitation with native wallet button");step=1;}}
                case 2->{if(!(mc.gui.screen() instanceof TradeScreen screen))return;
                    if(step==0){mc.gameMode.handleContainerInput(screen.getMenu().containerId,45,0,ContainerInput.PICKUP,mc.player);step=1;return;}
                    if(step==1&&screen.getMenu().getSlot(0).hasItem()&&screen.getMenu().getSlot(9).hasItem()){amount(screen,host?"30":"5");press(screen,"金額を提示",false);step=2;passed.add("native inventory click proposes stack and amount edit sends offer");}}
                case 3->{if(!(mc.gui.screen() instanceof TradeScreen screen)||ticks<15||step!=0)return;var trade=state.getAsJsonObject("trade");require(WalletScreen.number(trade,"ownCoins")== (host?30:5)&&WalletScreen.number(trade,"otherCoins")== (host?5:30),"Both coin offers synchronized");require(screen.getMenu().getSlot(0).getItem().is(host?Items.DIAMOND:Items.EMERALD)&&screen.getMenu().getSlot(9).getItem().is(host?Items.EMERALD:Items.DIAMOND),"Both exact item previews synchronized");screenshot(mc,"trade");press(screen,"内容を承諾",false);passed.add("rendered counterpart item and amount before consenting");step=1;}
                case 4->{if(step==0){require(WalletScreen.number(state,"balance")==expected,"Committed balance exact");require(mc.player.getInventory().getItem(0).is(host?Items.EMERALD:Items.DIAMOND),"Committed item received");passed.add("server commit synchronizes exact balances and exchanged inventory");mc.gui.setScreen(new WalletScreen(null));step=1;ticks=0;return;}
                    if(step==1&&ticks>=15){amount(mc.gui.screen(),"10");press(mc.gui.screen(),"10コイン束",false);step=2;return;}
                    if(step==2&&WalletScreen.number(state,"balance")==expected-10){require(mc.player.getInventory().contains(s->s.is(com.sumirelabs.sum1rem0d.game.ModContent.coinRoll())),"Physical ten-coin roll received");press(mc.gui.screen(),"所持コインを全て入金",false);step=3;return;}
                    if(step==3&&WalletScreen.number(state,"balance")==expected){passed.add("wallet buttons withdraw and redeposit exact ten-coin denomination");screenshot(mc,"wallet-completed");ack(4);step=4;}}
                case 5->{if(step!=0)return;require(original!=null&&WalletScreen.number(state,"balance")==originalBalance,"Original balance restored");for(int i=0;i<original.size();i++)require(ItemStack.matches(original.get(i),mc.player.getInventory().getItem(i)),"Original inventory restored");passed.add("temporary fixture fully restores original inventory and balance");ack(5);step=1;}
                case 6->{finish(mc,true,"");}
                default->{}
            }
        }catch(Throwable failure){Sum1reM0d.LOGGER.error("ECONOMY_SMOKE failed",failure);finish(mc,false,failure.toString());}
    }
    private static void finish(Minecraft mc,boolean success,String failure){done=true;var report=new JsonObject();report.addProperty("success",success);report.addProperty("failure",failure);report.add("passed",new Gson().toJsonTree(passed));try{Files.writeString(mc.gameDirectory.toPath().resolve("sum1rem0d-economy-verification.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception e){Sum1reM0d.LOGGER.error("Cannot save economy client result",e);}mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);mc.stop();}
}
