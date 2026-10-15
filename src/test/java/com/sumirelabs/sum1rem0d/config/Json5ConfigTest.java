package com.sumirelabs.sum1rem0d.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Json5ConfigTest {
  @TempDir Path directory;

  @Test
  void readsJson5SyntaxListsMappingsAndPreciseNumbers() {
    var value =
        ConfigSchema.normalize(
            Json5Config.read(
                """
                // head
                { features: {
                    early_fall: { start_height: +2., },
                    coin_rewards: { kill_coins: 0xA },
                    vegan: { forbidden_items: ['minecraft:beef', 'minecraft:porkchop',], },
                    ore_drops: {mapping: {'minecraft:coal_ore': 'minecraft:redstone'}},
                    death_message: {text: '日本語\\\
                続き'},
                  }, 'extension key': [9007199254740993, null, true, .5,], }
                """));
    assertEquals(
        10,
        value
            .getAsJsonObject("features")
            .getAsJsonObject("coin_rewards")
            .get("kill_coins")
            .getAsInt());
    assertEquals(
        "minecraft:beef,minecraft:porkchop",
        value
            .getAsJsonObject("features")
            .getAsJsonObject("vegan")
            .get("forbidden_items")
            .getAsString());
    assertEquals(
        "minecraft:coal_ore=minecraft:redstone",
        value
            .getAsJsonObject("features")
            .getAsJsonObject("ore_drops")
            .get("mapping")
            .getAsString());
    assertEquals(9007199254740993L, value.getAsJsonArray("extension key").get(0).getAsLong());
    assertEquals(
        "日本語続き",
        value
            .getAsJsonObject("features")
            .getAsJsonObject("death_message")
            .get("text")
            .getAsString());
    assertEquals(value, Json5Config.read(Json5Config.write(value, null)));
  }

  @Test
  void migrationKeepsOriginalAndUnknownFieldsAndIsOnlyDoneOnce() throws Exception {
    var old = directory.resolve("sum1rem0d.json");
    String original =
        "{\"extension key\":{\"a:b\":123},\"features\":{\"wooden_pickaxe\":{\"enabled\":false}}}";
    Files.writeString(old, original);
    var path = directory.resolve("sum1rem0d.json5");
    var store = new ConfigStore(path);
    store.load();
    assertEquals(original, Files.readString(old));
    assertFalse(store.enabled("wooden_pickaxe"));
    assertEquals(123, store.snapshot().getAsJsonObject("extension key").get("a:b").getAsInt());
    assertTrue(Files.readString(path).contains("初期値:"));
    assertTrue(Files.readString(path).contains("適用:"));
    Files.writeString(old, "{broken");
    var again = new ConfigStore(path);
    again.load();
    assertEquals(store.snapshot(), again.snapshot());
  }

  @Test
  void handwrittenCommentsSurviveGuiEquivalentUpdateAndGeneratedOnesDoNotMultiply()
      throws Exception {
    String previous =
        """
        // 手書きヘッダー
        { features: {
            // このワールドは落下を安全にしたい
            early_fall: { enabled: false /* ここも手書き */, },
            ore_drops: { mapping: { // 独自の組合せ
                'minecraft:coal_ore': 'minecraft:redstone',
            } },
          }, extension: [ // 配列メモ
              1, 2,
          ],
          // フッターメモ
        } // 最終メモ
        """;
    var path = directory.resolve("sum1rem0d.json5");
    Files.writeString(path, previous);
    var store = new ConfigStore(path);
    store.load();
    var draft = store.snapshot();
    draft.getAsJsonObject("features").getAsJsonObject("early_fall").addProperty("enabled", true);
    store.update(draft, 0);
    String first = Files.readString(path);
    for (String note :
        new String[] {"手書きヘッダー", "このワールドは落下を安全にしたい", "ここも手書き", "独自の組合せ", "配列メモ", "フッターメモ", "最終メモ"})
      assertTrue(first.contains(note), note);
    store.update(store.snapshot(), 1);
    String second = Files.readString(path);
    assertEquals(first, second);
    assertEquals(path, store.path());
    assertTrue(
        Json5Config.read(second)
            .getAsJsonObject("features")
            .getAsJsonObject("early_fall")
            .get("enabled")
            .getAsBoolean());
  }

  @Test
  void corruptionDuplicateKeysTrailingGarbageAndNonFiniteNumbersAreRejectedWithoutOverwrite()
      throws Exception {
    var path = directory.resolve("sum1rem0d.json5");
    for (String invalid :
        new String[] {
          "{features:",
          "{a:}",
          "{a:1,a:2}",
          "{} garbage",
          "{a:1} {}",
          "{a:NaN}",
          "{a:Infinity}",
          "{a:[1,,2]}",
          "{/* unfinished"
        }) {
      Files.writeString(path, invalid);
      assertThrows(RuntimeException.class, () -> new ConfigStore(path).load(), invalid);
      assertEquals(invalid, Files.readString(path));
    }
  }

  @Test
  void corruptLegacyDoesNotCreateNewFileAndCorruptNewFileDoesNotFallBack() throws Exception {
    var old = directory.resolve("sum1rem0d.json");
    var path = directory.resolve("sum1rem0d.json5");
    Files.writeString(old, "{broken");
    assertThrows(IllegalStateException.class, () -> new ConfigStore(path).load());
    assertFalse(Files.exists(path));
    Files.writeString(old, "{}");
    Files.writeString(path, "{broken");
    assertThrows(IllegalStateException.class, () -> new ConfigStore(path).load());
    assertEquals("{broken", Files.readString(path));
  }

  @Test
  void staleInvalidOrFailedSavePreservesBothCommentsAndMemory() throws Exception {
    var path = directory.resolve("sum1rem0d.json5");
    var store = new ConfigStore(path);
    store.load();
    String original = Files.readString(path);
    var invalid = store.snapshot();
    invalid
        .getAsJsonObject("features")
        .getAsJsonObject("economy")
        .addProperty("maximum_balance", 0);
    assertThrows(IllegalArgumentException.class, () -> store.update(invalid, 0));
    assertEquals(original, Files.readString(path));
    assertEquals(0, store.revision());
    Files.delete(path);
    Files.createDirectory(path);
    Files.writeString(path.resolve("blocker"), "keep");
    assertThrows(java.io.IOException.class, () -> store.update(store.snapshot(), 0));
    assertEquals(0, store.revision());
  }

  @Test
  void rejectsWrongListTypesMappingInjectionLargeAndDeepInput() {
    assertThrows(
        RuntimeException.class, () -> Json5Config.read("{features:{vegan:{forbidden_items:[1]}}}"));
    assertThrows(
        RuntimeException.class,
        () ->
            Json5Config.read(
                "{features:{ore_drops:{mapping:{'minecraft:a':'minecraft:b,minecraft:c=minecraft:d'}}}}"));
    assertThrows(
        RuntimeException.class,
        () -> Json5Config.read(" ".repeat(Json5Config.MAX_FILE_LENGTH + 1)));
    assertThrows(
        RuntimeException.class, () -> Json5Config.read("{a:".repeat(130) + "0" + "}".repeat(130)));
  }

  @Test
  void numericCompatibilityAndUnicodeWhitespaceRemainExact() {
    var values =
        Json5Config.read(
            "\uFEFF{\u00a0'日本語キー': [1., +2., .5, -.5, 1e2, 1.e2, 1.5e-2, -0x10, +0x20,"
                + " 9007199254740993,], // note\u2028 next: 1 }");
    var array = values.getAsJsonArray("日本語キー");
    assertEquals(10, array.size());
    double[] expected = {1, 2, .5, -.5, 100, 100, .015, -16, 32};
    for (int n = 0; n < expected.length; n++) assertEquals(expected[n], array.get(n).getAsDouble());
    assertEquals(9007199254740993L, array.get(9).getAsLong());
    assertEquals(1, values.get("next").getAsInt());
    assertThrows(RuntimeException.class, () -> Json5Config.read("{a:01}"));
  }

  @Test
  void json5WriteFailureRetainsCommentsAndDoesNotCommitRevision() throws Exception {
    var path = directory.resolve("sum1rem0d.json5");
    var store = new ConfigStore(path);
    store.load();
    Files.writeString(path, "{broken");
    assertThrows(java.io.IOException.class, () -> store.update(store.snapshot(), 0));
    assertEquals("{broken", Files.readString(path));
    assertEquals(0, store.revision());
  }

  @Test
  void removingAnEntryKeepsItsHandwrittenNoteInFooter() {
    String old =
        "{features:{ore_drops:{mapping:{ // keep this coal note\n"
            + " 'minecraft:coal_ore':'minecraft:redstone'}}}}";
    var values = ConfigSchema.normalize(Json5Config.read(old));
    values
        .getAsJsonObject("features")
        .getAsJsonObject("ore_drops")
        .addProperty("mapping", "minecraft:iron_ore=minecraft:gold_ingot");
    String saved = Json5Config.write(values, old);
    assertTrue(saved.contains("keep this coal note"));
    assertTrue(saved.contains("削除された項目のメモ"));
    assertEquals(saved, Json5Config.write(values, saved));
  }
}
