package com.sumirelabs.sum1rem0d.client;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.network.ConfigStatePayload;
import com.sumirelabs.sum1rem0d.wiki.SearchSuggestions;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Focused local-only browser/media and native navigation regression; no third-party video dependency. */
final class UiContinuitySmoke {
    private static int stage;
    private static long changedAt,startedAt;
    private static JsonObject saved,savedRules,beforeMedia;
    private static HttpServer server;
    private static BrowserSession session;
    private static final Map<String,CompletableFuture<String>> probes=new HashMap<>();
    private static final List<String> passed=new ArrayList<>();
    private static final String STATE="(()=>{const v=document.querySelector('video');return {playing:!!v&&!v.paused&&!v.ended,time:v?.currentTime??0,width:innerWidth,height:innerHeight,token:window.fixtureToken}})()";
    static void tick(Minecraft mc){
        if(stage<0||!Boolean.getBoolean("sum1rem0d.uiContinuitySmoke")||!mc.isGameLoadFinished())return;
        long now=System.nanoTime();
        try{
            if(startedAt!=0&&now-startedAt>90_000_000_000L)throw new AssertionError("UI/media fixture timed out at stage "+stage);
            if(changedAt!=0&&now-changedAt<1_200_000_000L)return;
            switch(stage){
                case 0->{
                    startedAt=now;String address=System.getProperty("sum1rem0d.networkAddress","127.0.0.1:25591");
                    ConnectScreen.startConnecting(new TitleScreen(),mc,net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(address),new net.minecraft.client.multiplayer.ServerData("UI continuity",address,net.minecraft.client.multiplayer.ServerData.Type.OTHER),false,null);next(now);
                }
                case 1->{
                    if(mc.player==null||!ClientRules.received())return;
                    saved=PersonalConfig.store().snapshot();savedRules=ClientRules.snapshot();
                    server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
                    server.createContext("/",exchange->{
                        String html="<!doctype html><meta charset=utf-8><style>html,body{margin:0;background:#234}video{display:block;width:100%}</style><canvas id=c width=320 height=180 hidden></canvas><video id=v autoplay muted playsinline></video><script>window.fixtureToken='same-session';const c=document.querySelector('canvas'),v=document.querySelector('video'),g=c.getContext('2d');let t=0;setInterval(()=>{g.fillStyle='hsl('+(t++%360)+',70%,45%)';g.fillRect(0,0,320,180);g.fillStyle='white';g.font='24px sans-serif';g.fillText('Persistent video '+t,15,95)},50);v.srcObject=c.captureStream(20);v.play();</script>";
                        byte[] bytes=html.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","text/html; charset=utf-8");exchange.sendResponseHeaders(200,bytes.length);try(var out=exchange.getResponseBody()){out.write(bytes);}
                    });server.start();
                    var config=saved.deepCopy();var yt=config.getAsJsonObject("features").getAsJsonObject("youtube");yt.addProperty("url","http://127.0.0.1:"+server.getAddress().getPort()+"/");yt.addProperty("stop_on_close",false);yt.addProperty("random_video",false);yt.addProperty("mini_player",true);yt.addProperty("mini_width",160);yt.addProperty("mini_height",90);yt.addProperty("mini_margin",8);yt.addProperty("volume",0);config.getAsJsonObject("features").getAsJsonObject("wiki").addProperty("enabled",false);config.getAsJsonObject("features").getAsJsonObject("wiki").addProperty("search_suggestions",true);PersonalConfig.store().update(config,PersonalConfig.store().revision());
                    var rules=savedRules.deepCopy();for(String id:List.of("youtube","economy","sumzon"))rules.getAsJsonObject("features").getAsJsonObject(id).addProperty("enabled",true);ClientRules.accept(new ConfigStatePayload(ClientRules.revision,false,rules.toString(),"",0,false));BrowserOverlays.closeAll();
                    var inventory=new InventoryScreen(mc.player);mc.gui.setScreen(inventory);
                    press(inventory,"Sum1re Wallet");require(mc.gui.screen() instanceof WalletScreen,"Wallet button opens the wallet");mc.gui.setScreen(inventory);
                    press(inventory,"Sumzon");require(mc.gui.screen() instanceof ShopScreen,"Sumzon button opens the shop");mc.gui.setScreen(inventory);
                    passed.add("Inventory Wallet and Sumzon buttons open their native screens");
                    var suggestions=new SuggestionFixture();mc.gui.setScreen(suggestions);suggestions.verify();next(now);
                }
                case 2->{
                    shot(mc,"suggestions-scroll-icons");passed.add("Suggestion wheel scroll reaches entries past the visible rows; icons resolve and keyboard selection follows scrolling");
                    mc.gui.setScreen(new InventoryScreen(mc.player));next(now);
                }
                case 3->{
                    session=BrowserOverlays.youtubeSession();if(session==null||!session.ready())return;
                    var state=probe(STATE);if(!state.get("playing").getAsBoolean()||state.get("time").getAsDouble()<.5){probes.clear();return;}
                    beforeMedia=state;
                    session.cef().executeJavaScript("setTimeout(()=>document.querySelector('video').pause(),150)",session.url(),0);
                    mc.gui.setScreen(null);require(BrowserOverlays.youtubeSession()==session&&!session.closed(),"Closing inventory retains exactly the same browser");next(now);
                }
                case 4->{
                    if(now-changedAt<2_000_000_000L)return;
                    var state=probe(STATE);require(state.get("playing").getAsBoolean()&&state.get("time").getAsDouble()>beforeMedia.get("time").getAsDouble()+.3,"Media timeline continues after inventory closes, including a transition pause");
                    require(state.get("width").equals(beforeMedia.get("width"))&&state.get("height").equals(beforeMedia.get("height")),"HUD drawing does not resize or reload the browser viewport");
                    int[] bounds=BrowserOverlays.hudBounds();require(bounds!=null&&bounds[0]>mc.getWindow().getGuiScaledWidth()/2&&bounds[1]==26,"Mini player is anchored in the top-right corner");
                    shot(mc,"persistent-mini-player");passed.add("Same browser keeps playing after inventory close; cropped top-right mini player preserves viewport and timeline");
                    session.pauseMedia();next(now);
                }
                case 5->{
                    require(!probe(STATE).get("playing").getAsBoolean(),"Intentional pause stays paused");session.playMedia();mc.gui.setScreen(new InventoryScreen(mc.player));next(now);
                }
                case 6->{
                    require(BrowserOverlays.youtubeSession()==session&&probe(STATE).get("playing").getAsBoolean(),"Reopening inventory retains and resumes the same media session");passed.add("User pause is respected and reopening inventory reuses the playing browser");finish(mc,true,"");
                }
            }
        }catch(Pending ignored){}catch(Throwable failure){finish(mc,false,failure.toString());}
    }
    private static void press(Screen screen,String label){var button=screen.children().stream().filter(c->c instanceof Button b&&b.getMessage().getString().equals(label)).map(c->(Button)c).findFirst().orElseThrow();require(button.active,"Active inventory navigation: "+label);button.onPress(new KeyEvent(InputConstants.KEY_RETURN,0,0));}
    private static void next(long now){stage++;changedAt=now;probes.clear();}
    private static JsonObject probe(String expression){var pending=probes.get(expression);if(pending==null){var params=new JsonObject();params.addProperty("expression",expression);params.addProperty("returnByValue",true);probes.put(expression,session.cef().getDevToolsClient().executeDevToolsMethod("Runtime.evaluate",params.toString()));throw new Pending();}if(!pending.isDone())throw new Pending();return JsonParser.parseString(pending.join()).getAsJsonObject().getAsJsonObject("result").getAsJsonObject("value");}
    private static void shot(Minecraft mc,String name){net.minecraft.client.Screenshot.grab(mc.gameDirectory,name+".png",mc.gameRenderer.mainRenderTarget(),1,c->{});}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void finish(Minecraft mc,boolean success,String failure){
        stage=-1;BrowserOverlays.closeAll();if(server!=null)server.stop(0);
        try{if(saved!=null)PersonalConfig.store().update(saved,PersonalConfig.store().revision());if(savedRules!=null)ClientRules.accept(new ConfigStatePayload(ClientRules.revision,false,savedRules.toString(),"",0,false));}catch(Exception e){success=false;failure+="; "+e;}
        var report=new JsonObject();report.addProperty("success",success);report.addProperty("failure",failure);report.add("passed",new Gson().toJsonTree(passed));
        try{Files.writeString(mc.gameDirectory.toPath().resolve("sum1rem0d-ui-continuity.json"),report.toString());}catch(Exception e){throw new IllegalStateException(e);}
        mc.disconnectFromWorld(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE);mc.stop();
    }
    private static final class Pending extends RuntimeException{@Override public synchronized Throwable fillInStackTrace(){return this;}}
    private static final class SuggestionFixture extends Screen{
        private EditBox input;private SearchSuggestionPopup popup;private List<SearchSuggestions.Entry> entries;private String opened="";
        SuggestionFixture(){super(Component.literal("Suggestions"));}
        @Override protected void init(){input=addRenderableWidget(new EditBox(font,16,24,width-32,20,Component.literal("Search")));entries=net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().filter(i->i!=Items.AIR).limit(30).map(i->new SearchSuggestions.Entry(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(i).toString(),i.getDefaultInstance().getHoverName().getString(),"")).toList();popup=new SearchSuggestionPopup(input,entries,height,id->opened=id);input.setValue("minecraft");input.setFocused(true);setFocused(input);popup.changed(input.getValue());}
        void verify(){
            var found=SearchSuggestions.find(entries,"minecraft",entries.size());require(found.size()>10,"More suggestions than fit in visible rows");
            require(popup.scroll(input.getX()+8,input.getY()+input.getHeight()+5,-6),"Wheel consumed by the popup");
            popup.click(new MouseButtonEvent(input.getX()+8,input.getY()+input.getHeight()+5,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0)));
            require(opened.equals(found.get(6).id()),"Click after scrolling opens the actual seventh entry, not an old visible index");
            popup.changed("minecraft");popup.key(new KeyEvent(InputConstants.KEY_UP,0,0));popup.key(new KeyEvent(InputConstants.KEY_RETURN,0,0));require(opened.equals(found.getLast().id()),"Keyboard navigation reaches the final result");
            require(!SearchSuggestionPopup.defaultIcon(found.getFirst()).isEmpty(),"Existing item suggestion has a native icon");popup.changed("minecraft");popup.scroll(input.getX()+8,input.getY()+input.getHeight()+5,-6);
        }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics,int x,int y,float tick){graphics.fill(0,0,width,height,0xFF202832);super.extractRenderState(graphics,x,y,tick);popup.render(graphics,font,x,y);}
    }
}
