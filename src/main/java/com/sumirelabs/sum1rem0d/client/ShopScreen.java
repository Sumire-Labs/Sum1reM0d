package com.sumirelabs.sum1rem0d.client;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.game.*;
import java.util.*;
import java.text.Normalizer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Native, offline storefront; only the server decides price, payment, expiry and delivery. */
public final class ShopScreen extends Screen {
 private final Screen parent;private final LinkedHashMap<String,Integer> cart=new LinkedHashMap<>();
 private List<ItemStack> catalog=List.of(),visible=List.of();private String query="",quantity="1",seenParticipants="";private int tab,page,category,left,top,w,h,rows,productRows,ticks;private long seenRevision=-1,seenCompletion,seenRules=-1;
 public ShopScreen(Screen parent){super(Component.literal("Sumzon"));this.parent=parent;}
 private SearchSuggestionPopup suggestions;
 private final Map<Button,JsonObject> claimRows=new LinkedHashMap<>();
 private static long n(JsonObject o,String k){return o.has(k)?o.get(k).getAsLong():0;}
 private Button button(String text,int x,int y,int width,Runnable run){return addRenderableWidget(Button.builder(Component.literal(text),b->run.run()).bounds(x,y,width,20).build());}
 private boolean canAct(){return ShopClient.ready()&&!ShopClient.blocking()&&ShopClient.number("configRevision")==ClientRules.revision&&minecraft.player!=null&&minecraft.player.isAlive()&&!minecraft.player.isSpectator()&&(!minecraft.player.isCreative()||ClientRules.bool("sumzon","allow_creative"));}
 private void load(){var items=new ArrayList<>(java.util.stream.StreamSupport.stream(BuiltInRegistries.ITEM.spliterator(),false).filter(item->item!=Items.AIR&&item.isEnabled(minecraft.level.enabledFeatures())).map(ItemStack::new).filter(ShopCatalog::allowed).toList());var state=ShopClient.state();if(state.has("pastPlayers"))for(var row:state.getAsJsonArray("pastPlayers")){var person=row.getAsJsonObject();try{items.add(ShopCatalog.playerHead(UUID.fromString(person.get("id").getAsString()),person.get("name").getAsString()));}catch(IllegalArgumentException ignored){}}catalog=items.stream().sorted(Comparator.comparing(ShopCatalog::key)).toList();seenParticipants=state.has("pastPlayers")?state.get("pastPlayers").toString():"";filter();}
 private static String normalize(String s){return Normalizer.normalize(s,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);}
 private void filter(){String term=normalize(query);String wanted=term.isBlank()?switch(category){case 1->"blocks";case 2->"equipment";case 3->"food";case 4->"other";default->"";}:"";visible=catalog.stream().filter(s->wanted.isEmpty()||ShopCatalog.category(s).equals(wanted)).filter(s->{String text=normalize(s.getHoverName().getString()+" "+ShopCatalog.key(s)+" "+ShopCatalog.category(s)).replace('_',' ');return Arrays.stream(term.replace('_',' ').split("\\s+")).allMatch(text::contains);}).sorted(Comparator.comparingInt(s->normalize(ShopCatalog.key(s)).equals(term)?0:1)).toList();}
 @Override protected void init(){
  suggestions=null;
  w=Math.min(560,width-16);h=Math.min(330,height-16);left=(width-w)/2;top=(height-h)/2;rows=Math.max(1,(h-124)/22);productRows=Math.max(1,(h-130)/18);
  String[] tabs={"商品","カート ("+cart.size()+")","注文・受取","プラン"};for(int i=0;i<4;i++){final int chosen=i;button(tabs[i],left+8+i*((w-16)/4),top+28,(w-20)/4,()->{tab=chosen;page=0;rebuildWidgets();});}
  if(tab==0){
   var search=addRenderableWidget(new EditBox(font,left+8,top+52,w-130,20,Component.literal("全商品を検索")));search.setMaxLength(128);search.setValue(query);
   if(ClientRules.bool("sumzon","search_suggestions"))suggestions=new SearchSuggestionPopup(search,catalog.stream().map(s->new com.sumirelabs.sum1rem0d.wiki.SearchSuggestions.Entry(ShopCatalog.key(s),s.getHoverName().getString(),ShopCatalog.category(s))).toList(),height,id->{query=id;category=0;page=0;filter();rebuildWidgets();if(suggestions!=null)suggestions.dismiss();},entry->catalog.stream().filter(stack->ShopCatalog.key(stack).equals(entry.id())).findFirst().map(ItemStack::copy).orElse(ItemStack.EMPTY));
   search.setResponder(s->{query=s;page=0;filter();rebuildResults();if(suggestions!=null)suggestions.changed(s);});if(suggestions!=null)suggestions.changed(query);
   var amount=addRenderableWidget(new EditBox(font,left+w-116,top+52,44,20,Component.literal("個数")));amount.setMaxLength(4);amount.setValue(quantity);amount.setResponder(s->quantity=s);
   button(switch(category){case 1->"ブロック";case 2->"装備";case 3->"食品";case 4->"その他";default->"全て";},left+w-68,top+52,60,()->{category=(category+1)%5;page=0;filter();rebuildWidgets();});
  }
  rebuildResults();button("閉じる",left+w-68,top+h-26,60,this::onClose);if(!ShopClient.ready())ShopClient.refresh();
 }
 @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event){return suggestions!=null&&suggestions.key(event)||super.keyPressed(event);}
 @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean twice){return suggestions!=null&&suggestions.click(event)||super.mouseClicked(event,twice);}
 @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){return suggestions!=null&&suggestions.scroll(x,y,vertical)||super.mouseScrolled(x,y,horizontal,vertical);}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){super.extractRenderState(g,x,y,delta);if(suggestions!=null)suggestions.render(g,font,x,y);}
 private final List<Button> resultButtons=new ArrayList<>();
 private void rebuildResults(){for(var b:resultButtons)removeWidget(b);resultButtons.clear();claimRows.clear();int total=tab==0?visible.size():tab==1?cart.size():tab==2?orders().size():0;int capacity=tab==0?productRows*2:rows;int pages=Math.max(1,(total+capacity-1)/capacity);page=Math.min(page,pages-1);
  if(tab==0){int colWidth=(w-24)/2;for(int i=0;i<capacity&&page*capacity+i<visible.size();i++){var stack=visible.get(page*capacity+i);int col=i%2,row=i/2,x=left+8+col*(colWidth+8),y=top+78+row*18;var b=addRenderableWidget(Button.builder(Component.literal("追加"),button->add(stack)).bounds(x+colWidth-44,y,40,18).build());b.setTooltip(Tooltip.create(stack.getHoverName()));resultButtons.add(b);}}
  if(tab==1){var entries=new ArrayList<>(cart.entrySet());for(int i=0;i<rows&&page*rows+i<entries.size();i++){var entry=entries.get(page*rows+i);resultButtons.add(button("削除",left+w-56,top+78+i*22,48,()->{cart.remove(entry.getKey());rebuildWidgets();}));}var checkout=button("注文する",left+80,top+h-26,w-156,()->{var items=new JsonObject();cart.forEach(items::addProperty);var body=new JsonObject();body.add("cart",items);ShopClient.ordersPage=0;ShopClient.request("checkout",body);});resultButtons.add(checkout);}
  if(tab==2){
   if(ShopClient.number("ordersPages")>1){resultButtons.add(button("新しい注文",left+8,top+51,90,()->{ShopClient.ordersPage=Math.max(0,ShopClient.ordersPage-1);page=0;ShopClient.refresh();}));resultButtons.add(button("古い注文",left+102,top+51,90,()->{ShopClient.ordersPage=Math.min((int)ShopClient.number("ordersPages")-1,ShopClient.ordersPage+1);page=0;ShopClient.refresh();}));}
   var orders=orders();for(int i=0;i<rows&&page*rows+i<orders.size();i++){var order=orders.get(page*rows+i);var b=button("受取",left+w-56,top+78+i*22,48,()->{var body=new JsonObject();body.addProperty("order",n(order,"id"));ShopClient.request("claim",body);});b.active=ShopServices.remaining(order)>0&&n(order,"due")<=ShopClient.number("now");claimRows.put(b,order);resultButtons.add(b);}}
  if(tab==3){for(int i=0;i<2;i++){final String plan=i==0?"ad":"prime",f=i==0?"ad_premium":"sumzon_prime";int y=top+88+i*62;resultButtons.add(button("契約/延長 "+ClientRules.integer(f,"price"),left+8,y,(w-24)/2,()->{var body=new JsonObject();body.addProperty("plan",plan);ShopClient.request("subscribe",body);}));resultButtons.add(button("自動更新 "+(ShopClient.flag(plan+"Auto")?"ON":"OFF"),left+16+(w-24)/2,y,(w-24)/2,()->{var body=new JsonObject();body.addProperty("plan",plan);body.addProperty("enabled",!ShopClient.flag(plan+"Auto"));ShopClient.request("auto",body);}));}}
  if(tab!=3){var prev=button("◀",left+8,top+h-26,30,()->{page--;rebuildWidgets();});prev.active=page>0;resultButtons.add(prev);var next=button("▶",left+42,top+h-26,30,()->{page++;rebuildWidgets();});next.active=page+1<pages;resultButtons.add(next);}
 }
 private void add(ItemStack stack){try{int n=Integer.parseInt(quantity);if(n<1||n>ClientRules.integer("sumzon","maximum_quantity"))throw new IllegalArgumentException();String id=ShopCatalog.key(stack);if(!cart.containsKey(id)&&cart.size()>=ClientRules.integer("sumzon","maximum_lines"))throw new IllegalArgumentException();int total=Math.addExact(cart.getOrDefault(id,0),n);if(total>ClientRules.integer("sumzon","maximum_quantity"))throw new IllegalArgumentException();cart.put(id,total);ShopClient.message="カートに追加しました";rebuildWidgets();}catch(RuntimeException e){ShopClient.message="個数とカートの上限を確認してください";}}
 private List<JsonObject> orders(){var s=ShopClient.state();if(!s.has("orders"))return List.of();var list=new ArrayList<JsonObject>();s.getAsJsonArray("orders").forEach(row->list.add(row.getAsJsonObject()));Collections.reverse(list);return list;}
 private ItemStack cartItem(String id){return catalog.stream().filter(stack->ShopCatalog.key(stack).equals(id)).findFirst().orElseGet(()->ShopCatalog.item(id));}
 private long total(){long sum=0;for(var entry:cart.entrySet())sum=Math.addExact(sum,Math.multiplyExact(ShopCatalog.price(cartItem(entry.getKey())),entry.getValue()));return sum;}
 private static String time(long ticks){if(ticks<=0)return "期限切れ";long hours=(ticks+999)/1000;return hours>=24?hours/24+"日 "+hours%24+"時間":hours+"時間";}
 @Override public void tick(){
  if(minecraft.player==null||minecraft.level==null){onClose();return;}if(seenRules!=ClientRules.revision){seenRules=ClientRules.revision;load();rebuildWidgets();}
  String participants=ShopClient.state().has("pastPlayers")?ShopClient.state().get("pastPlayers").toString():"";if(!participants.equals(seenParticipants)){load();if(tab==0)rebuildWidgets();}
  if(ShopClient.completion!=seenCompletion){seenCompletion=ShopClient.completion;if(ShopClient.applied&&ShopClient.action.equals("checkout")){cart.clear();tab=2;page=0;}if(!ShopClient.action.equals("refresh"))rebuildWidgets();}
  if(seenRevision!=ShopClient.number("revision")){seenRevision=ShopClient.number("revision");if(tab>0)rebuildWidgets();}
  if(++ticks%Math.max(5,ClientRules.integer("sumzon","catalog_refresh_ticks"))==0&&!ShopClient.blocking()){if(ShopClient.ready()&&ShopClient.number("configRevision")!=ClientRules.revision)PlatformClientNetwork.refresh();ShopClient.refresh();}
  claimRows.forEach((button,order)->button.active=canAct()&&ShopServices.remaining(order)>0&&n(order,"due")<=ShopClient.number("now"));
  for(var b:resultButtons){String label=b.getMessage().getString();if(label.equals("追加"))b.active=canAct()&&ClientRules.enabled("sumzon")&&ClientRules.enabled("economy");if(label.equals("注文する"))b.active=canAct()&&!cart.isEmpty()&&ClientRules.enabled("sumzon")&&ClientRules.enabled("economy");if(label.startsWith("契約/延長")){int index=resultButtons.indexOf(b);b.active=canAct()&&ClientRules.enabled("economy")&&ClientRules.enabled(index<2?"ad_premium":"sumzon_prime");}if(label.startsWith("自動更新")){boolean ad=resultButtons.indexOf(b)<2;String plan=ad?"ad":"prime",feature=ad?"ad_premium":"sumzon_prime";b.active=ShopClient.ready()&&!ShopClient.blocking()&&(ShopClient.flag(plan+"Auto") || canAct()&&ClientRules.enabled("economy")&&ClientRules.enabled(feature)&&ShopClient.number(plan+"Until")>0);}}
 }
 @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){super.extractBackground(g,mouseX,mouseY,delta);g.fill(left,top,left+w,top+h,0xFF151D29);g.fill(left,top,left+w,top+24,0xFF243247);g.text(font,"Sumzon",left+8,top+8,0xFFFFC44D,false);g.text(font,"残高 "+ShopClient.number("balance")+" コイン",left+100,top+8,0xFFFFFFFF,false);
  if(tab==0){int colWidth=(w-24)/2;for(int i=0;i<productRows*2&&page*productRows*2+i<visible.size();i++){var s=visible.get(page*productRows*2+i);int col=i%2,row=i/2,x=left+8+col*(colWidth+8),y=top+78+row*18;g.item(s,x,y+1);g.text(font,font.plainSubstrByWidth(s.getHoverName().getString(),colWidth-132),x+20,y+5,0xFFFFFFFF,false);g.text(font,font.plainSubstrByWidth(ShopCatalog.price(s)+" コイン",64),x+colWidth-110,y+5,0xFFFFD45A,false);}}
  if(tab==1){var entries=new ArrayList<>(cart.entrySet());for(int i=0;i<rows&&page*rows+i<entries.size();i++){var e=entries.get(page*rows+i);var s=cartItem(e.getKey());int y=top+78+i*22;g.item(s,left+8,y+2);g.text(font,font.plainSubstrByWidth(s.getHoverName().getString(),w-160)+" ×"+e.getValue(),left+30,y+6,0xFFFFFFFF,false);}boolean prime=ShopClient.number("primeUntil")>ShopClient.number("now");long fee=prime?0:ClientRules.integer("sumzon","shipping");g.text(font,"商品 "+total()+" ＋送料 "+fee+" ＝ "+(total()+fee)+" コイン",left+8,top+55,0xFFFFD45A,false);}
  if(tab==2){var orders=orders();if(ShopClient.number("ordersPages")<=1)g.text(font,"空き枠分を受取。残りはそのまま保管します",left+8,top+55,0xFFBFC8D8,false);for(int i=0;i<rows&&page*rows+i<orders.size();i++){var order=orders.get(page*rows+i);int y=top+78+i*22;long remain=ShopServices.remaining(order);String status=remain==0?"受取済み":n(order,"due")<=ShopClient.number("now")?"受取可能":"配送中 "+time(n(order,"due")-ShopClient.number("now"));g.text(font,font.plainSubstrByWidth("#"+n(order,"id")+"  "+status+"  残り"+remain+"個",w-72),left+8,y+6,0xFFFFFFFF,false);}}
  if(tab==3){String[] plans={"ad","prime"};for(int i=0;i<2;i++){String plan=plans[i],f=i==0?"ad_premium":"sumzon_prime";int y=top+55+i*62;g.text(font,(i==0?"超高級広告プレミアム":"Sumzon Prime")+"  "+time(ShopClient.number(plan+"Until")-ShopClient.number("now")),left+8,y,0xFFFFD45A,false);g.text(font,font.plainSubstrByWidth((i==0?"Mod広告を停止。Webサイト自身の広告は対象外":"送料無料・配送時間短縮")+" / "+ClientRules.number(f,"days")+"日",w-16),left+8,y+13,0xFFBFC8D8,false);}}
  String message=ShopClient.showProgress()?"処理中…":ShopClient.message;g.text(font,font.plainSubstrByWidth(message,w-16),left+8,top+h-42,0xFFFFD45A,false);
  if(tab==0)g.text(font,(page+1)+" / "+Math.max(1,(visible.size()+productRows*2-1)/(productRows*2))+"  "+visible.size()+"商品",left+82,top+h-20,0xFFBFC8D8,false);
 }
 @Override public void onClose(){minecraft.gui.setScreen(parent);}
 @Override public boolean isPauseScreen(){return false;}
}
