package com.sumirelabs.sum1rem0d.network;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;

public final class JsonCompression {
    public static final int MAX_WIRE_BYTES = 30000;
    public static final int MAX_JSON_BYTES = 262144;
    private JsonCompression() {}
    public static byte[] encode(String text) {
        byte[] plain = text.getBytes(StandardCharsets.UTF_8);
        if (plain.length > MAX_JSON_BYTES) throw new IllegalArgumentException("JSON too large");
        try {
            var out = new ByteArrayOutputStream();
            try (var gzip = new GZIPOutputStream(out)) { gzip.write(plain); }
            byte[] compressed = out.toByteArray();
            if (compressed.length > MAX_WIRE_BYTES) throw new IllegalArgumentException("Compressed JSON too large");
            return compressed;
        } catch (IOException exception) { throw new IllegalArgumentException("Cannot encode JSON", exception); }
    }
    public static String decode(byte[] compressed) {
        if (compressed.length > MAX_WIRE_BYTES) throw new IllegalArgumentException("Payload too large");
        try (var gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            byte[] plain = gzip.readNBytes(MAX_JSON_BYTES + 1);
            if (plain.length > MAX_JSON_BYTES) throw new IllegalArgumentException("Expanded JSON too large");
            return new String(plain, StandardCharsets.UTF_8);
        } catch (IOException exception) { throw new IllegalArgumentException("Invalid compressed JSON", exception); }
    }
}
