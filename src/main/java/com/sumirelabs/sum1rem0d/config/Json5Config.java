package com.sumirelabs.sum1rem0d.config;

import com.google.gson.*;
import de.marhali.json5.*;
import de.marhali.json5.config.Json5Options;
import java.io.*;
import java.util.*;

/** JSON5 persistence with generated descriptions and comments retained by JSON pointer. */
public final class Json5Config {
  public static final int MAX_FILE_LENGTH = 1024 * 1024;
  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
  private static final Json5 JSON5 = new Json5(Json5Options.builder().parseComments().build());
  private static final String START = "[Sum1reM0d]", END = "[/Sum1reM0d]";

  private Json5Config() {}

  public static JsonObject read(String source) {
    var structure = new Structure(source).document();
    Json5Element value = JSON5.parse(structure.normalized());
    JsonElement json = tree(value);
    if (!json.isJsonObject()) throw new IllegalArgumentException("Config root must be an object");
    return internal(json.getAsJsonObject());
  }

  private static JsonElement tree(Json5Element value) {
    if (value.isJson5Null()) return JsonNull.INSTANCE;
    if (value.isJson5Object()) {
      var object = new JsonObject();
      value.getAsJson5Object().entrySet().forEach(e -> object.add(e.getKey(), tree(e.getValue())));
      return object;
    }
    if (value.isJson5Array()) {
      var array = new JsonArray();
      value.getAsJson5Array().forEach(v -> array.add(tree(v)));
      return array;
    }
    var primitive = value.getAsJson5Primitive();
    if (primitive.isBoolean()) return new JsonPrimitive(value.getAsBoolean());
    if (primitive.isString()) return new JsonPrimitive(value.getAsString());
    if (primitive.isNumber()) return new JsonPrimitive(value.getAsNumber());
    throw new IllegalArgumentException("Unsupported JSON5 value");
  }

  private static JsonObject internal(JsonObject root) {
    var features = root.getAsJsonObject("features");
    if (features == null) return root;
    for (var feature : ConfigSchema.features()) {
      var values = features.getAsJsonObject(feature.id());
      if (values == null) continue;
      for (var option : feature.options()) {
        var value = values.get(option.key());
        if (value == null) continue;
        if (option.kind() == ConfigSchema.Kind.LIST && value.isJsonArray()) {
          var entries = new ArrayList<String>();
          for (var entry : value.getAsJsonArray()) {
            if (!entry.isJsonPrimitive()
                || !entry.getAsJsonPrimitive().isString()
                || entry.getAsString().contains(","))
              throw new IllegalArgumentException("List entries must be strings without commas");
            entries.add(entry.getAsString());
          }
          values.addProperty(option.key(), String.join(",", entries));
        } else if (option.kind() == ConfigSchema.Kind.MAP && value.isJsonObject()) {
          var entries = new ArrayList<String>();
          for (var entry : value.getAsJsonObject().entrySet()) {
            if (!entry.getValue().isJsonPrimitive()
                || !entry.getValue().getAsJsonPrimitive().isString())
              throw new IllegalArgumentException("Mapping values must be strings");
            if (entry.getKey().matches(".*[,=].*")
                || entry.getValue().getAsString().matches(".*[,=].*"))
              throw new IllegalArgumentException("Mapping entries cannot contain CSV delimiters");
            entries.add(entry.getKey() + "=" + entry.getValue().getAsString());
          }
          values.addProperty(option.key(), String.join(",", entries));
        }
      }
    }
    return root;
  }

  public static String write(JsonObject candidate, String previous) {
    var comments =
        previous == null
            ? new LinkedHashMap<String, String>()
            : new Structure(previous).document().comments;
    comments.replaceAll(
        (path, text) ->
            text.replaceAll("(?s)\\Q" + START + "\\E.*?\\Q" + END + "\\E\\s*", "").strip());
    var json = candidate.deepCopy();
    var features = json.getAsJsonObject("features");
    for (var feature : ConfigSchema.features()) {
      String path = "/features/" + escape(feature.id());
      addGenerated(comments, path, feature.label());
      var values = features.getAsJsonObject(feature.id());
      for (var option : feature.options()) {
        String detail =
            option.label()
                + "\n初期値: "
                + initialDescription(option)
                + (option.isNumber()
                    ? "\n範囲: " + numberLabel(option.min()) + " ～ " + numberLabel(option.max())
                    : "")
                + "\n設定範囲: "
                + (PersonalConfig.personal(feature.id(), option.key()) ? "個人" : "サーバー")
                + "\n適用: "
                + ConfigTiming.description(feature.id(), option.key());
        addGenerated(comments, path + "/" + escape(option.key()), detail);
        if (option.kind() == ConfigSchema.Kind.LIST) {
          var array = new JsonArray();
          ConfigSchema.split(values.get(option.key()).getAsString()).forEach(array::add);
          values.add(option.key(), array);
        }
        if (option.kind() == ConfigSchema.Kind.MAP) {
          var object = new JsonObject();
          for (String entry : ConfigSchema.split(values.get(option.key()).getAsString())) {
            var pair = entry.split("=", 2);
            object.addProperty(pair[0], pair[1]);
          }
          values.add(option.key(), object);
        }
      }
    }
    addGenerated(
        comments,
        "",
        "Sum1reM0d JSON5 Config\n"
            + "コメント・末尾カンマ・引用符なしのキーを使用できます。\n"
            + "LISTは配列、MAPはオブジェクトで編集できます。\n"
            + "説明コメントは自動更新されます。手書きコメントは同じ項目に保持します。\n"
            + "json5-java 3.0.0 (Apache-2.0)。");
    var available = new HashSet<String>();
    paths(json, "", available);
    for (var entry : new ArrayList<>(comments.entrySet())) {
      if (!available.contains(entry.getKey()) && !entry.getValue().isBlank()) {
        comments.merge(
            "/~footer",
            "削除された項目のメモ (" + entry.getKey() + "):\n" + entry.getValue(),
            (a, b) -> a + "\n" + b);
      }
    }
    var out = new StringBuilder();
    comment(out, comments.get(""), 0);
    emit(out, json, "", 0, comments);
    String result = out.append('\n').toString();
    if (result.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_FILE_LENGTH)
      throw new IllegalArgumentException("Config file too large after adding comments");
    return result;
  }

  private static String numberLabel(double value) {
    return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
  }

  private static String initialDescription(ConfigSchema.Option option) {
    if (option.kind() != ConfigSchema.Kind.LIST && option.kind() != ConfigSchema.Kind.MAP)
      return option.initial().toString();
    var entries = ConfigSchema.split(option.initial().getAsString());
    return entries.isEmpty() ? "空" : "\n- " + String.join("\n- ", entries);
  }

  private static void paths(JsonElement value, String path, Set<String> result) {
    result.add(path);
    if (value.isJsonObject()) {
      result.add(path + "/~footer");
      for (var entry : value.getAsJsonObject().entrySet())
        paths(entry.getValue(), path + "/" + escape(entry.getKey()), result);
    } else if (value.isJsonArray()) {
      result.add(path + "/~footer");
      int i = 0;
      for (var item : value.getAsJsonArray()) paths(item, path + "/" + i++, result);
    }
  }

  private static void addGenerated(Map<String, String> comments, String path, String generated) {
    String user = comments.getOrDefault(path, "");
    comments.put(path, START + "\n" + generated + "\n" + END + (user.isBlank() ? "" : "\n" + user));
  }

  private static String escape(String key) {
    return key.replace("~", "~0").replace("/", "~1");
  }

  private static void comment(StringBuilder out, String text, int depth) {
    if (text == null || text.isBlank()) return;
    for (String line : text.split("\\R", -1))
      out.append("  ".repeat(depth)).append("// ").append(line).append('\n');
  }

  private static void emit(
      StringBuilder out, JsonElement value, String path, int depth, Map<String, String> comments) {
    if (value.isJsonObject()) {
      out.append("{\n");
      for (var entry : value.getAsJsonObject().entrySet()) {
        String child = path + "/" + escape(entry.getKey());
        comment(out, comments.get(child), depth + 1);
        out.append("  ".repeat(depth + 1));
        String key = entry.getKey();
        out.append(key.matches("[A-Za-z_$][A-Za-z0-9_$]*") ? key : GSON.toJson(key)).append(": ");
        emit(out, entry.getValue(), child, depth + 1, comments);
        out.append(",\n");
      }
      comment(out, comments.get(path + "/~footer"), depth + 1);
      out.append("  ".repeat(depth)).append('}');
    } else if (value.isJsonArray()) {
      out.append("[\n");
      int i = 0;
      for (var item : value.getAsJsonArray()) {
        String child = path + "/" + i++;
        comment(out, comments.get(child), depth + 1);
        out.append("  ".repeat(depth + 1));
        emit(out, item, child, depth + 1, comments);
        out.append(",\n");
      }
      comment(out, comments.get(path + "/~footer"), depth + 1);
      out.append("  ".repeat(depth)).append(']');
    } else {
      Object primitive =
          value.isJsonNull()
              ? null
              : value.getAsJsonPrimitive().isString()
                  ? value.getAsString()
                  : value.getAsJsonPrimitive().isBoolean()
                      ? value.getAsBoolean()
                      : value.getAsNumber();
      try {
        out.append(
            JSON5.serialize(
                primitive == null
                    ? new Json5Null()
                    : primitive instanceof String text
                        ? Json5Primitive.fromString(text)
                        : primitive instanceof Boolean bool
                            ? Json5Primitive.fromBoolean(bool)
                            : Json5Primitive.fromNumber((Number) primitive)));
      } catch (IOException failure) {
        throw new java.io.UncheckedIOException(failure);
      }
    }
  }

  /**
   * Checks structure, duplicates and EOF while retaining comments by JSON pointer. No library
   * source is copied.
   */
  private static final class Structure {
    record Replacement(int start, int end, String value) {}

    final List<Replacement> replacements = new ArrayList<>();
    final String source;
    int at;
    final Map<String, String> comments = new LinkedHashMap<>();

    Structure(String source) {
      if (source.length() > MAX_FILE_LENGTH)
        throw new IllegalArgumentException("Config file too large");
      this.source = source;
    }

    Structure document() {
      store("", space());
      value("", 0);
      store("", space());
      if (at != source.length()) fail("Extra data after configuration");
      return this;
    }

    String normalized() {
      var result = new StringBuilder(source);
      for (var change : replacements.reversed())
        result.replace(change.start(), change.end(), change.value());
      return result.toString();
    }

    String normalizeNumber(String token) {
      if (!token.matches(
          "[+-]?(?:(?:0|[1-9][0-9]*)(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?")) {
        if ("+-.0123456789".indexOf(token.charAt(0)) >= 0
            && !token.matches("[+-]?0[xX][0-9a-fA-F]+")) fail("Invalid JSON5 numeric literal");
        return token;
      }
      if (token.length() > 4096) fail("Numeric literal too long");
      var number = new java.math.BigDecimal(token);
      if (Math.abs((long) number.scale()) > 4096) fail("Numeric exponent too large");
      return number.toPlainString();
    }

    void fail(String text) {
      throw new IllegalArgumentException(text + " at character " + at);
    }

    void store(String path, String text) {
      if (!text.isBlank()) comments.merge(path, text, (a, b) -> a + "\n" + b);
    }

    String space() {
      var result = new StringBuilder();
      while (at < source.length()) {
        char c = source.charAt(at);
        if (Character.isWhitespace(c)
            || Character.getType(c) == Character.SPACE_SEPARATOR
            || c == '\uFEFF') {
          at++;
          continue;
        }
        if (c == '/' && at + 1 < source.length()) {
          char next = source.charAt(at + 1);
          if (next == '/') {
            at += 2;
            int start = at;
            while (at < source.length()
                && source.charAt(at) != '\n'
                && source.charAt(at) != '\r'
                && source.charAt(at) != '\u2028'
                && source.charAt(at) != '\u2029') at++;
            String text = source.substring(start, at);
            result.append(text.startsWith(" ") ? text.substring(1) : text).append('\n');
            continue;
          }
          if (next == '*') {
            at += 2;
            int start = at;
            int end = source.indexOf("*/", at);
            if (end < 0) fail("Unterminated comment");
            result.append(source, start, end).append('\n');
            at = end + 2;
            continue;
          }
        }
        break;
      }
      return result.toString().strip();
    }

    boolean take(char c) {
      if (at < source.length() && source.charAt(at) == c) {
        at++;
        return true;
      }
      return false;
    }

    String token() {
      int start = at;
      if (at >= source.length()) fail("Expected value");
      char quote = source.charAt(at);
      if (quote == '\'' || quote == '\"') {
        at++;
        boolean done = false;
        while (at < source.length()) {
          char c = source.charAt(at++);
          if (c == '\\') {
            if (at >= source.length()) fail("Incomplete escape");
            if (source.charAt(at++) == '\r' && at < source.length() && source.charAt(at) == '\n')
              at++;
          } else if (c == quote) {
            done = true;
            break;
          } else if (c == '\n' || c == '\r' || c == '\u2028' || c == '\u2029')
            fail("Unescaped newline in string");
        }
        if (!done) fail("Unterminated string");
      } else {
        while (at < source.length()) {
          char c = source.charAt(at);
          if (Character.isWhitespace(c)
              || Character.getType(c) == Character.SPACE_SEPARATOR
              || "{}[],:/".indexOf(c) >= 0) break;
          at++;
        }
        if (start == at) fail("Expected token");
      }
      return source.substring(start, at);
    }

    void value(String path, int depth) {
      if (depth > 128) fail("Configuration nesting too deep");
      if (take('{')) {
        var keys = new HashSet<String>();
        String prefix = space();
        if (take('}')) {
          store(path + "/~footer", prefix);
          return;
        }
        while (true) {
          String keyToken = token();
          Json5Object key = JSON5.parse("{" + keyToken + ":null}").getAsJson5Object();
          if (key.size() != 1) fail("Invalid property name");
          String name = key.keySet().iterator().next();
          if (!keys.add(name)) fail("Duplicate property " + name);
          String child = path + "/" + escape(name);
          store(child, prefix);
          store(child, space());
          if (!take(':')) fail("Expected colon");
          store(child, space());
          value(child, depth + 1);
          store(child, space());
          if (take('}')) return;
          if (!take(',')) fail("Expected comma");
          prefix = space();
          if (take('}')) {
            store(path + "/~footer", prefix);
            return;
          }
        }
      } else if (take('[')) {
        int i = 0;
        String prefix = space();
        if (take(']')) {
          store(path + "/~footer", prefix);
          return;
        }
        while (true) {
          String child = path + "/" + i++;
          store(child, prefix);
          value(child, depth + 1);
          store(child, space());
          if (take(']')) return;
          if (!take(',')) fail("Expected comma");
          prefix = space();
          if (take(']')) {
            store(path + "/~footer", prefix);
            return;
          }
        }
      } else {
        int start = at;
        String scalar = token();
        String canonical = normalizeNumber(scalar);
        if (!canonical.equals(scalar)) replacements.add(new Replacement(start, at, canonical));
        Json5Array wrapper = JSON5.parse("[" + canonical + "]").getAsJson5Array();
        if (wrapper.size() != 1) fail("Invalid scalar");
        Json5Element parsed = wrapper.get(0);
        if (parsed.isJson5Object() || parsed.isJson5Array()) fail("Invalid scalar");
      }
    }
  }
}
