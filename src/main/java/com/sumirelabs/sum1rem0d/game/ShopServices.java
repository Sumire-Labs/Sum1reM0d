package com.sumirelabs.sum1rem0d.game;

import com.google.gson.*;
import com.sumirelabs.sum1rem0d.Sum1reM0d;
import com.sumirelabs.sum1rem0d.network.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Orders and their money live in the same atomic state; deliveries share the wallet inventory journal. */
public final class ShopServices {
 private ShopServices(){}
 private static long now(ServerPlayer p){return WorldState.get(p.level().getServer()).serviceTime();}
 private static long number(JsonObject data,String key){return data.has(key)?data.get(key).getAsBigDecimal().longValueExact():0;}
 private static boolean flag(JsonObject data,String key){return data.has(key)&&data.get(key).getAsBoolean();}
 private static ItemStack catalogItem(String id,WorldState state){var item=ShopCatalog.item(id);var face=ShopCatalog.playerFaceId(id);if(face!=null){var participant=state.playerRecords().get(face.toString());if(participant!=null&&participant.lastKnownName!=null&&!participant.lastKnownName.isBlank())return ShopCatalog.playerHead(face,participant.lastKnownName);}return item;}
 private static ItemStack catalogItem(String id,ServerPlayer viewer){return catalogItem(id,WorldState.get(viewer.level().getServer()));}
 private static JsonObject data(ServerPlayer p){var d=WorldState.get(p.level().getServer()).player(p);if(d.services==null)d.services=new JsonObject();if(!d.services.has("orders"))d.services.add("orders",new JsonArray());if(number(d.services,"revision")<0)throw new IllegalStateException("Invalid service revision");return d.services;}
 private static String feature(String plan){return switch(plan){case "ad"->"ad_premium";case "prime"->"sumzon_prime";default->throw new IllegalArgumentException("プランを確認してください");};}
 private static long period(String plan){return Math.max(1,(long)Math.ceil(Rules.n(feature(plan),"days")*24000));}
 public static boolean active(ServerPlayer p,String plan){return number(data(p),plan+"Until")>now(p);}
 public static JsonObject summary(ServerPlayer p){var d=data(p);var result=new JsonObject();result.addProperty("adPremium",active(p,"ad"));result.addProperty("prime",active(p,"prime"));result.addProperty("revision",number(d,"revision"));result.addProperty("orders",d.getAsJsonArray("orders").size());return result;}
 public static JsonObject snapshot(ServerPlayer p){var d=data(p).deepCopy();d.addProperty("now",now(p));var wallet=WorldState.get(p.level().getServer()).player(p);d.addProperty("balance",wallet.walletInitialized?Economy.balance(p):0);d.addProperty("configRevision",Sum1reM0d.config().revision());return d;}
 /** Bounded owner-only order pages keep responses below the compressed payload limits. */
 public static JsonObject networkSnapshot(ServerPlayer p,int page){
  var sourceData=data(p);var state=new JsonObject();for(var entry:sourceData.entrySet())if(!entry.getKey().equals("orders"))state.add(entry.getKey(),entry.getValue().deepCopy());state.addProperty("now",now(p));var wallet=WorldState.get(p.level().getServer()).player(p);state.addProperty("balance",wallet.walletInitialized?Economy.balance(p):0);state.addProperty("configRevision",Sum1reM0d.config().revision());var all=sourceData.getAsJsonArray("orders");int pages=Math.max(1,(all.size()+63)/64);page=Math.clamp(page,0,pages-1);var rows=new JsonArray();
  for(int i=Math.max(0,all.size()-(page+1)*64);i<all.size()-page*64;i++){var source=all.get(i).getAsJsonObject();var row=new JsonObject();for(String key:List.of("id","due","total","prime"))if(source.has(key))row.add(key,source.get(key));row.addProperty("remaining",remaining(source));rows.add(row);}
  state.add("orders",rows);state.addProperty("ordersPage",page);state.addProperty("ordersPages",pages);state.addProperty("ordersTotal",all.size());
  var ownerData=WorldState.get(p.level().getServer()).player(p);
  if(ownerData.deliveryBoxDimension!=null&&!ownerData.deliveryBoxDimension.isBlank()){
   state.addProperty("deliveryBoxDimension",ownerData.deliveryBoxDimension);state.addProperty("deliveryBoxPos",ownerData.deliveryBoxPos);
   var boxPos=net.minecraft.core.BlockPos.of(ownerData.deliveryBoxPos);
   if(ownerData.deliveryBoxDimension.equals(p.level().dimension().identifier().toString())&&p.level().hasChunkAt(boxPos)&&p.level().getBlockEntity(boxPos) instanceof DeliveryBoxEntity box&&p.getUUID().equals(box.owner())){
    var allowed=new JsonArray();for(var id:box.permitted())allowed.add(id.toString());state.add("deliveryBoxPermitted",allowed);
   }else state.add("deliveryBoxPermitted",new JsonArray());
  }else state.add("deliveryBoxPermitted",new JsonArray());
  var past=new ArrayList<Map.Entry<String,WorldState.PlayerData>>();
  for(var entry:WorldState.get(p.level().getServer()).playerRecords().entrySet())if(!entry.getKey().equals(p.getUUID().toString())&&entry.getValue().lastKnownName!=null&&!entry.getValue().lastKnownName.isBlank())past.add(entry);
  past.sort(Comparator.comparing(e->e.getValue().lastKnownName,String.CASE_INSENSITIVE_ORDER));var participants=new JsonArray();int limit=Math.max(1,Rules.i("sumzon","player_head_listing_limit"));
  for(int i=0;i<Math.min(limit,past.size());i++){var entry=past.get(i);var person=new JsonObject();person.addProperty("id",entry.getKey());person.addProperty("name",entry.getValue().lastKnownName);participants.add(person);}state.add("pastPlayers",participants);
  return state;
 }
 private static void validateMode(ServerPlayer p){if(!p.isAlive() || p.isSpectator() || p.isCreative()&&!Rules.b("sumzon","allow_creative"))throw new IllegalArgumentException("購入・受取は生存中のサバイバル/アドベンチャーで利用できます");if(p.containerMenu!=p.inventoryMenu || Economy.isTrading(p))throw new IllegalArgumentException("取引や作業画面を閉じてください");}
 private static void store(ServerPlayer p,JsonObject next,long coins){
  var world=WorldState.get(p.level().getServer());var before=world.checkpoint();
  try{next.addProperty("revision",Math.addExact(number(data(p),"revision"),1));world.player(p).services=next;Economy.balance(p,coins);world.saveStrict();}
  catch(RuntimeException e){world.restore(before);throw new IllegalStateException("保存できないため操作を取り消しました",e);}
  Gameplay.sendState(p);
 }
 private static void deliverDue(MinecraftServer server, UUID ownerId, WorldState.PlayerData record, WorldState world) {
  if(record.services==null||!record.services.has("orders"))return;
  if (record.deliveryBoxDimension == null || record.deliveryBoxDimension.isBlank()) return;
  var boxLevel = worldLevel(server,record.deliveryBoxDimension);
  if (boxLevel == null) return;
  var pos = net.minecraft.core.BlockPos.of(record.deliveryBoxPos);
  if (!boxLevel.hasChunkAt(pos) || !(boxLevel.getBlockEntity(pos) instanceof DeliveryBoxEntity box) || !box.canDeliver(ownerId)) return;
  var current = record.services;
  long serviceTime=world.serviceTime();
  boolean changed = false;
  if(Rules.on("sumzon_delivery_box")){
   var migrated=current.deepCopy();boolean migration=false;
   for(var row:migrated.getAsJsonArray("orders")){var order=row.getAsJsonObject();if(!flag(order,"boxDelivery")){order.addProperty("boxDelivery",true);migration=true;}}
   if(migration){var checkpoint=world.checkpoint();try{migrated.addProperty("revision",Math.addExact(number(current,"revision"),1));record.services=migrated;world.saveStrict();current=migrated;changed=true;}catch(RuntimeException failure){world.restore(checkpoint);throw failure;}}
  }
  for (var row : current.getAsJsonArray("orders")) {
   var order = row.getAsJsonObject();
   if (!flag(order,"boxDelivery") || number(order,"due") > serviceTime || remaining(order) == 0 || !order.has("lines")) continue;
   var next = current.deepCopy();
   var copied = next.getAsJsonArray("orders").asList().stream().map(JsonElement::getAsJsonObject)
       .filter(candidate -> number(candidate,"id") == number(order,"id")).findFirst().orElse(null);
   if (copied == null) continue;
   var lines = copied.getAsJsonArray("lines"); boolean lineChanged = false;
   var checkpoint = world.checkpoint();
   try {
    for (var lineElement : lines) {
     var line = lineElement.getAsJsonObject(); long left = number(line,"remaining"); if (left <= 0) continue;
     var stack = catalogItem(line.get("item").getAsString(),world); if (stack.isEmpty()) continue;
     int amount = (int)Math.min(left, stack.getMaxStackSize()); stack.setCount(amount);
     int inserted = box.insertDelivered(stack);
     if (inserted > 0) { line.addProperty("remaining", left-inserted); lineChanged = true; }
    }
    if (!lineChanged) { world.restore(checkpoint); continue; }
    boolean completed=remaining(copied)==0&&!flag(copied,"notified");if(completed)copied.addProperty("notified",true);
    next.addProperty("revision", Math.addExact(number(current,"revision"),1));
    record.services = next;
    box.commitMailbox();
    current = next;
    changed = true;
    var online=server.getPlayerList().getPlayer(ownerId);if(completed&&online!=null)online.sendSystemMessage(net.minecraft.network.chat.Component.literal("[Sumzon] 注文 #"+number(copied,"id")+" が宅配BOXへ届きました"));
   } catch (RuntimeException failure) { world.restore(checkpoint); throw failure; }
  }
  var online=server.getPlayerList().getPlayer(ownerId);if (changed&&online!=null) Gameplay.sendState(online);
 }
 private static net.minecraft.server.level.ServerLevel worldLevel(MinecraftServer server,String dimension) {
  for(var level:server.getAllLevels()) if(level.dimension().identifier().toString().equals(dimension)) return level;
  return null;
 }
 private static void receipt(JsonObject q,ServerPlayer p){if(number(q,"configRevision")!=Sum1reM0d.config().revision())throw new IllegalArgumentException("価格や条件が更新されました。再確認してください");if(number(q,"revision")!=number(data(p),"revision"))throw new IllegalArgumentException("残高・注文・契約が更新されました。再確認してください");}
 public static void checkout(ServerPlayer p,JsonObject cart){
  validateMode(p);if(!Rules.on("economy") || !Rules.on("sumzon"))throw new IllegalArgumentException("現在、新規注文は利用できません");if(cart.size()==0 || cart.size()>Rules.i("sumzon","maximum_lines"))throw new IllegalArgumentException("カートの商品数を確認してください");
  var next=data(p).deepCopy();var orders=next.getAsJsonArray("orders");long open=0;for(var row:orders)if(remaining(row.getAsJsonObject())>0)open++;if(open>=Rules.i("sumzon","maximum_orders"))throw new IllegalArgumentException("未受取の注文が上限に達しています");
  long total=0;var lines=new JsonArray();
  for(var entry:cart.entrySet()){
   long quantity=entry.getValue().getAsBigDecimal().longValueExact();if(quantity<1 || quantity>Rules.i("sumzon","maximum_quantity"))throw new IllegalArgumentException("商品の個数を確認してください");var face=ShopCatalog.playerFaceId(entry.getKey());if(face!=null){var participant=WorldState.get(p.level().getServer()).playerRecords().get(face.toString());if(participant==null||participant.lastKnownName==null||participant.lastKnownName.isBlank())throw new IllegalArgumentException("参加履歴のないプレイヤーの頭です");}var item=catalogItem(entry.getKey(),p);if(!ShopCatalog.allowed(item) || !item.getItem().isEnabled(p.level().enabledFeatures()))throw new IllegalArgumentException("販売対象外の商品です: "+entry.getKey());
   long price=ShopCatalog.price(item);total=Math.addExact(total,Math.multiplyExact(price,quantity));var line=new JsonObject();line.addProperty("item",entry.getKey());line.addProperty("quantity",quantity);line.addProperty("remaining",quantity);line.addProperty("price",price);lines.add(line);
  }
  boolean prime=active(p,"prime");long shipping=prime?0:Rules.i("sumzon","shipping");total=Math.addExact(total,shipping);long balance=Economy.balance(p);if(balance<total)throw new IllegalArgumentException("コインが足りません");
  var order=new JsonObject();long id=Math.addExact(number(next,"sequence"),1);next.addProperty("sequence",id);order.addProperty("id",id);order.addProperty("created",now(p));order.addProperty("due",Math.addExact(now(p),(long)Math.ceil(Rules.n(prime?"sumzon_prime":"sumzon","delivery_hours")*1000)));order.addProperty("total",total);order.addProperty("shipping",shipping);order.addProperty("prime",prime);order.addProperty("boxDelivery",Rules.on("sumzon_delivery_box"));order.add("lines",lines);orders.add(order);
  int closed=0;for(var row:orders)if(remaining(row.getAsJsonObject())==0)closed++;for(int i=0;i<orders.size() && closed>Rules.i("sumzon","history_limit");){if(remaining(orders.get(i).getAsJsonObject())==0){orders.remove(i);closed--;}else i++;}
  store(p,next,balance-total);
 }
 public static long remaining(JsonObject order){if(!order.has("lines"))return number(order,"remaining");long n=0;for(var line:order.getAsJsonArray("lines"))n=Math.addExact(n,number(line.getAsJsonObject(),"remaining"));return n;}
 private static int insert(ItemStack[] inventory,ItemStack source,int amount){int left=amount;
  for(var current:inventory)if(!current.isEmpty() && ItemStack.isSameItemSameComponents(current,source)){int n=Math.min(left,Math.max(0,Math.min(current.getMaxStackSize(),source.getMaxStackSize())-current.getCount()));current.grow(n);left-=n;if(left==0)return amount;}
  for(int i=0;i<inventory.length && left>0;i++)if(inventory[i].isEmpty()){int n=Math.min(left,source.getMaxStackSize());inventory[i]=source.copyWithCount(n);left-=n;}return amount-left;
 }
 public static void claim(ServerPlayer p,long orderId){
  validateMode(p);if(Rules.on("sumzon_delivery_box")&&Rules.b("sumzon_delivery_box","require_for_claim")&&!DeliveryBoxAccess.canClaim(p))throw new IllegalArgumentException("先に自分の宅配BOXを開いてください");var next=data(p).deepCopy();JsonObject order=null;for(var row:next.getAsJsonArray("orders"))if(number(row.getAsJsonObject(),"id")==orderId)order=row.getAsJsonObject();if(order==null)throw new IllegalArgumentException("注文が見つかりません");if(number(order,"due")>now(p))throw new IllegalArgumentException("まだ配送中です");if(remaining(order)==0)throw new IllegalArgumentException("受取済みです");if(flag(order,"boxDelivery")&&Rules.on("sumzon_delivery_box")&&Rules.b("sumzon_delivery_box","require_for_claim"))throw new IllegalArgumentException("この注文は宅配BOXへ自動配送されます。BOX内の品物を取り出してください");
  var inventory=Economy.deliveryInventory(p);int moved=0;
  for(var row:order.getAsJsonArray("lines")){var line=row.getAsJsonObject();int left=Math.toIntExact(number(line,"remaining"));if(left==0)continue;var item=catalogItem(line.get("item").getAsString(),p);if(item.isEmpty())continue;int n=insert(inventory,item,left);line.addProperty("remaining",left-n);moved+=n;}
  if(moved==0)throw new IllegalArgumentException("空き枠がありません。商品のModが存在するかも確認してください");next.addProperty("revision",Math.addExact(number(data(p),"revision"),1));Economy.deliverServices(p,inventory,next);
 }
 public static void subscribe(ServerPlayer p,String plan){
  validateMode(p);String f=feature(plan);if(!Rules.on("economy") || !Rules.on(f))throw new IllegalArgumentException("現在、この契約は利用できません");long price=Rules.i(f,"price"),coins=Economy.balance(p);if(coins<price)throw new IllegalArgumentException("コインが足りません");var next=data(p).deepCopy();next.addProperty(plan+"Until",Math.addExact(Math.max(now(p),number(next,plan+"Until")),period(plan)));next.addProperty(plan+"Price",price);next.addProperty(plan+"Period",period(plan));store(p,next,coins-price);
 }
 public static void auto(ServerPlayer p,String plan,boolean enabled){feature(plan);if(enabled){if(!Rules.on("economy") || !Rules.on(feature(plan)) || number(data(p),plan+"Until")==0)throw new IllegalArgumentException("先にプランを契約してください");validateMode(p);}var next=data(p).deepCopy();next.addProperty(plan+"Auto",enabled);next.addProperty(plan+"Price",Rules.i(feature(plan),"price"));next.addProperty(plan+"Period",period(plan));store(p,next,Economy.balance(p));}
 private static void permission(ServerPlayer p,JsonObject q) {
  UUID target=UUID.fromString(q.get("target").getAsString());
  if(!q.has("allow")||!q.get("allow").isJsonPrimitive()||!q.getAsJsonPrimitive("allow").isBoolean())throw new IllegalArgumentException("権限設定を確認してください");
  var state=WorldState.get(p.level().getServer());var owner=state.player(p);
  long requested=number(q,"boxPos");var pos=net.minecraft.core.BlockPos.of(requested);
  if(!p.level().dimension().identifier().toString().equals(q.get("boxDimension").getAsString())||!owner.deliveryBoxDimension.equals(q.get("boxDimension").getAsString())||owner.deliveryBoxPos!=requested||!p.level().hasChunkAt(pos)||p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))>64)throw new IllegalArgumentException("宅配BOXの位置を確認してください");
  if(!(p.level().getBlockEntity(pos) instanceof DeliveryBoxEntity box)||!p.getUUID().equals(box.owner()))throw new IllegalArgumentException("宅配BOX所有者だけが権限を変更できます");
  var targetPlayer=p.level().getServer().getPlayerList().getPlayer(target);
  double radius=Rules.n("sumzon_delivery_box","permission_radius");if(targetPlayer==null||targetPlayer.level()!=p.level()||targetPlayer.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))>radius*radius)throw new IllegalArgumentException("許可するプレイヤーは同じ次元の近くに必要です");
  if(!box.permit(p.getUUID(),target,q.get("allow").getAsBoolean()))throw new IllegalArgumentException("宅配BOX権限を変更できません");
 }
 public static void tick(MinecraftServer server){
  var world=WorldState.get(server);world.advanceServices();
  for(var entry:world.playerRecords().entrySet())try{deliverDue(server,UUID.fromString(entry.getKey()),entry.getValue(),world);}catch(RuntimeException failure){Sum1reM0d.LOGGER.warn("Mailbox delivery failed for {}",entry.getKey(),failure);}
  for(var p:server.getPlayerList().getPlayers()){
   var d=world.player(p);if(d.lastServiceFailure!=Long.MIN_VALUE && world.serviceTime()-d.lastServiceFailure<100)continue;
   try{
    if(p.isAlive() && !Economy.isTrading(p))for(String plan:List.of("ad","prime"))if(flag(data(p),plan+"Auto") && !active(p,plan) && Rules.on("economy") && Rules.on(feature(plan))){
     if(number(data(p),plan+"Price")!=Rules.i(feature(plan),"price") || number(data(p),plan+"Period")!=period(plan)){auto(p,plan,false);p.sendSystemMessage(net.minecraft.network.chat.Component.literal("[Sumzon] 条件変更のため自動更新を停止しました"));}
     else if(Economy.balance(p)<Rules.i(feature(plan),"price")){auto(p,plan,false);p.sendSystemMessage(net.minecraft.network.chat.Component.literal("[Sumzon] 残高不足のため自動更新を停止しました"));}
     else if(p.containerMenu==p.inventoryMenu && (!p.isCreative() || Rules.b("sumzon","allow_creative")))subscribe(p,plan);
    }
    if(world.serviceTime()%20==0 && Rules.b("sumzon","notify_ready"))for(var row:data(p).getAsJsonArray("orders")){var order=row.getAsJsonObject();if(remaining(order)>0 && number(order,"due")<=world.serviceTime() && !flag(order,"notified")){order.addProperty("notified",true);p.sendSystemMessage(net.minecraft.network.chat.Component.literal(flag(order,"boxDelivery")?"[Sumzon] 注文 #"+number(order,"id")+" が宅配BOXへ届きました":"[Sumzon] 注文 #"+number(order,"id")+" の配達が完了しました。注文画面で受け取れます"));}}
   }catch(RuntimeException e){world.player(p).lastServiceFailure=world.serviceTime();Sum1reM0d.LOGGER.warn("Service renewal/notification failed for {}",p.getUUID(),e);}
  }
 }
 public static void handle(ShopRequestPayload packet,ServerPlayer p){
  var server=p.level().getServer();if(!server.isSameThread()){server.execute(()->handle(packet,p));return;}long requestId=0;int ordersPage=0;String action="refresh",message="";boolean applied=false;
  try{
   if(packet.json().length()>16384)throw new IllegalArgumentException("操作が大きすぎます");var q=JsonParser.parseString(packet.json()).getAsJsonObject();requestId=number(q,"requestId");ordersPage=(int)Math.clamp(number(q,"ordersPage"),0,Integer.MAX_VALUE);action=q.get("action").getAsString();
   if(!action.equals("refresh") && !(action.equals("verify") && Boolean.getBoolean("sum1rem0d.remainingVerify"))){var record=WorldState.get(server).player(p);if(record.lastServiceRequest!=Long.MIN_VALUE && now(p)-record.lastServiceRequest<Rules.i("sumzon","request_interval"))throw new IllegalArgumentException("少し待ってから操作してください");record.lastServiceRequest=now(p);receipt(q,p);}
   switch(action){case "verify"->{RemainingLiveVerification.command(p,q.get("phase").getAsString());applied=true;}case "refresh"->{}case "checkout"->{checkout(p,q.getAsJsonObject("cart"));applied=true;message="注文しました";}case "claim"->{claim(p,number(q,"order"));applied=true;message="受け取りました。残りの商品は受取ボックスに保管されます";}case "box_permission"->{permission(p,q);applied=true;message="宅配BOXの許可設定を保存しました";}case "subscribe"->{subscribe(p,q.get("plan").getAsString());applied=true;message="契約を更新しました";}case "auto"->{if(!q.get("enabled").isJsonPrimitive() || !q.get("enabled").getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException("設定値を確認してください");auto(p,q.get("plan").getAsString(),q.get("enabled").getAsBoolean());applied=true;message="自動更新設定を保存しました";}default->throw new IllegalArgumentException("操作を確認してください");}
  }catch(RuntimeException e){message=e.getMessage()==null?"操作を完了できません":e.getMessage();if(e instanceof IllegalStateException)Sum1reM0d.LOGGER.warn("Shop operation failed",e);if(message.length()>180)message=message.substring(0,180);}
  var state=networkSnapshot(p,ordersPage);state.addProperty("requestId",requestId);state.addProperty("action",action);state.addProperty("applied",applied);state.addProperty("message",message);PlatformNetwork.send(p,new ShopStatePayload(state.toString()));
 }
}
