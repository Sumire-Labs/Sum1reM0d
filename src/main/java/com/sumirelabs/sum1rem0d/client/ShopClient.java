package com.sumirelabs.sum1rem0d.client;
import com.google.gson.*;
import com.sumirelabs.sum1rem0d.config.*;
import com.sumirelabs.sum1rem0d.network.ShopStatePayload;
public final class ShopClient {
 private static JsonObject state=new JsonObject();private static long sequence,pendingId;
 public static int ordersPage;public static long completion;public static String message="",action="";public static boolean applied;
 private ShopClient(){}
 public static JsonObject state(){return state.deepCopy();}
 public static long number(String key){return state.has(key)?state.get(key).getAsLong():0;}
 public static boolean flag(String key){return state.has(key)&&state.get(key).getAsBoolean();}
 public static boolean ready(){return state.has("configRevision");}public static boolean pending(){return pendingId!=0;}
 public static void clear(){state=new JsonObject();ordersPage=0;pendingId=0;message="";completion=0;}
 public static void accept(ShopStatePayload payload){var next=JsonParser.parseString(payload.json()).getAsJsonObject();long id=next.get("requestId").getAsLong();if(id!=pendingId)return;state=next;if(id==pendingId){pendingId=0;completion=id;message=next.get("message").getAsString();action=next.get("action").getAsString();applied=next.get("applied").getAsBoolean();}}
 public static void request(String action,JsonObject body){if(pending())return;var q=body.deepCopy();q.addProperty("ordersPage",ordersPage);q.addProperty("action",action);q.addProperty("requestId",++sequence);q.addProperty("revision",number("revision"));q.addProperty("configRevision",number("configRevision"));pendingId=sequence;PlatformClientNetwork.shop(new com.sumirelabs.sum1rem0d.network.ShopRequestPayload(q.toString()));}
 public static void refresh(){request("refresh",new JsonObject());}
 public static boolean adPremium(){var root=ClientGameState.snapshot();return root.has("services") && root.getAsJsonObject("services").has("adPremium") && root.getAsJsonObject("services").get("adPremium").getAsBoolean();}
}
