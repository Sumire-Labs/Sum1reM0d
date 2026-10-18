package com.sumirelabs.sum1rem0d.config;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
class ConfigDefaults204Test {
 @TempDir Path directory;
 @Test void migratesOldStandardsOnceAndPreservesNotesAndCustomValues()throws Exception{
  var path=directory.resolve("settings.json5");
  Files.writeString(path,"// my handwritten note\n{features:{leaf_pass:{inside_speed:0.5},tesla_coil:{range:4}}}");
  var store=new ConfigStore(path);store.load();
  assertEquals(.6,store.number("leaf_pass","inside_speed"));assertEquals(10,store.number("tesla_coil","range"));
  assertTrue(Files.exists(directory.resolve("settings.json5.before-2.0.4")));assertTrue(Files.readString(path).contains("my handwritten note"));
  var updated=store.snapshot();updated.getAsJsonObject("features").getAsJsonObject("tesla_coil").addProperty("range",4);store.update(updated,store.revision());store.load();
  assertEquals(4,store.number("tesla_coil","range"));
  var other=directory.resolve("custom.json5");Files.writeString(other,"{features:{leaf_pass:{inside_speed:0.8},tesla_coil:{range:6}}}");
  var custom=new ConfigStore(other);custom.load();assertEquals(.8,custom.number("leaf_pass","inside_speed"));assertEquals(6,custom.number("tesla_coil","range"));
 }
}
