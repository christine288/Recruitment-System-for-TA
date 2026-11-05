package com.bupt.ta.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Minimal JSON file persistence with no external libraries.
 * <p>
 * Supports reading and writing a JSON array of flat objects whose field values are strings.
 * Expected on-disk format: {@code [{"key":"value",...}, ...]}. Missing files are treated as
 * empty arrays. Also provides static helpers for serialising nested profile payloads and
 * parsing JSON arrays used elsewhere (e.g. education history in {@link ProfileService}).
 */
public class FileStore {
    private final Path baseDir;

    /**
     * Creates a store that resolves file names relative to {@code baseDir}.
     *
     * @param baseDir directory containing JSON data files
     */
    public FileStore(Path baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Reads a JSON array file and returns a list of string-to-string maps.
     * <p>
     * Returns an empty list when the file does not exist, is empty, or contains {@code []}.
     *
     * @param fileName file name relative to {@link #baseDir}
     * @return parsed rows; never {@code null}
     * @throws IOException if the file exists but cannot be read
     */
    public List<Map<String, String>> readMaps(String fileName) throws IOException {
        Path p = baseDir.resolve(fileName);
        if (!Files.exists(p)) return new ArrayList<>();
        String json = new String(Files.readAllBytes(p), StandardCharsets.UTF_8).trim();
        if (json.isEmpty() || json.equals("[]")) return new ArrayList<>();
        return parseJsonArray(json);
    }

    /**
     * Writes a list of string maps as a formatted JSON array to the given file.
     * <p>
     * Parent directories are created if missing. Existing file content is replaced.
     *
     * @param fileName file name relative to {@link #baseDir}
     * @param items    rows to serialise; {@code null} entries are not expected
     * @throws IOException if directories cannot be created or the file cannot be written
     */
    public void writeMaps(String fileName, List<Map<String, String>> items) throws IOException {
        Path p = baseDir.resolve(fileName);
        Files.createDirectories(p.getParent());
        try (Writer w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(p.toFile()), StandardCharsets.UTF_8))) {
            w.write(toJsonArray(items));
        }
    }

    private static String toJsonArray(List<Map<String, String>> items) {
        return toJsonArrayOfObjects(items);
    }

    /**
     * Serialises a JSON array of objects with string values.
     * <p>
     * Used for nested profile payloads such as education history embedded in profile fields.
     *
     * @param items list of flat string maps, one per JSON object
     * @return JSON array text with optional newlines between elements
     */
    public static String toJsonArrayOfObjects(List<Map<String, String>> items) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('\n').append(toJsonObject(items.get(i)));
        }
        if (!items.isEmpty()) sb.append('\n');
        sb.append(']');
        return sb.toString();
    }

    private static String toJsonObject(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('\"').append(escapeJson(e.getKey())).append('\"')
              .append(':').append('\"').append(escapeJson(e.getValue())).append('\"');
        }
        sb.append('}');
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Parses a JSON array of flat string-valued objects.
     * <p>
     * Hand-rolled parser that handles escaped characters inside string values. Non-string
     * scalar values are read as bare tokens; {@code null} literals become Java {@code null}.
     *
     * @param json JSON text beginning with {@code [}
     * @return list of object maps in array order; never {@code null}
     */
    static List<Map<String, String>> parseJsonArray(String json) {
        List<Map<String, String>> result = new ArrayList<>();
        int i = 0;
        int len = json.length();
        while (i < len && json.charAt(i) != '[') i++;
        i++;

        while (i < len) {
            while (i < len && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= len) break;
            char c = json.charAt(i);
            if (c == ']') break;
            if (c == '{') {
                int[] pos = {i + 1};
                Map<String, String> obj = new LinkedHashMap<>();
                while (pos[0] < len) {
                    skipWs(json, pos);
                    if (json.charAt(pos[0]) == '}') { pos[0]++; break; }
                    if (json.charAt(pos[0]) == ',') { pos[0]++; continue; }
                    if (json.charAt(pos[0]) == '\"') {
                        String key = readString(json, pos);
                        skipWs(json, pos);
                        if (pos[0] < len && json.charAt(pos[0]) == ':') pos[0]++;
                        skipWs(json, pos);
                        String value = readValue(json, pos);
                        obj.put(key, value);
                    } else {
                        pos[0]++;
                    }
                }
                result.add(obj);
                i = pos[0];
            } else if (c == ',') {
                i++;
            } else {
                i++;
            }
        }
        return result;
    }

    private static void skipWs(String s, int[] pos) {
        while (pos[0] < s.length() && Character.isWhitespace(s.charAt(pos[0]))) pos[0]++;
    }

    private static String readString(String s, int[] pos) {
        pos[0]++;
        StringBuilder sb = new StringBuilder();
        while (pos[0] < s.length()) {
            char c = s.charAt(pos[0]);
            if (c == '\\') {
                pos[0]++;
                if (pos[0] < s.length()) {
                    char esc = s.charAt(pos[0]);
                    switch (esc) {
                        case '\"': sb.append('\"'); break;
                        case '\\': sb.append('\\'); break;
                        case 'n':  sb.append('\n'); break;
                        case 'r':  sb.append('\r'); break;
                        case 't':  sb.append('\t'); break;
                        default:   sb.append(esc); break;
                    }
                    pos[0]++;
                }
            } else if (c == '\"') {
                pos[0]++;
                break;
            } else {
                sb.append(c);
                pos[0]++;
            }
        }
        return sb.toString();
    }

    private static String readValue(String s, int[] pos) {
        if (pos[0] < s.length() && s.charAt(pos[0]) == '\"') {
            return readString(s, pos);
        }
        StringBuilder sb = new StringBuilder();
        while (pos[0] < s.length()) {
            char c = s.charAt(pos[0]);
            if (c == ',' || c == '}' || Character.isWhitespace(c)) break;
            sb.append(c);
            pos[0]++;
        }
        String v = sb.toString();
        return v.equals("null") ? null : v;
    }
}
