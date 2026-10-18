package com.sumirelabs.sum1rem0d.config;
import com.google.gson.JsonObject;
/** One-time, narrow migration of the two old standard values explicitly changed in 2.0.4. */
final class ConfigDefaults204 {
 private ConfigDefaults204(){}
 static JsonObject migrate(JsonObject input){
  var out=input.deepCopy();
  if(out.has("defaults_revision")&&out.get("defaults_revision").isJsonPrimitive()&&out.get("defaults_revision").getAsJsonPrimitive().isNumber()&&out.get("defaults_revision").getAsInt()>=204)return out;
  if(out.has("features")&&out.get("features").isJsonObject()){
   var features=out.getAsJsonObject("features");
   replaceStandard(features,"leaf_pass","inside_speed",.5,.6);
   replaceStandard(features,"tesla_coil","range",4,10);
  }
  out.addProperty("defaults_revision",204);return out;
 }
 private static void replaceStandard(JsonObject features,String feature,String key,double before,double after){
  if(!features.has(feature)||!features.get(feature).isJsonObject())return;
  var values=features.getAsJsonObject(feature);
  if(values.has(key)&&values.get(key).isJsonPrimitive()&&values.get(key).getAsJsonPrimitive().isNumber()&&Double.compare(values.get(key).getAsDouble(),before)==0){if(after==Math.rint(after))values.addProperty(key,(int)after);else values.addProperty(key,after);}
 }
}
