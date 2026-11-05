package com.bupt.ta.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void readMaps_missingFile_returnsEmptyList() throws IOException {
        FileStore store = new FileStore(tempDir);
        assertTrue(store.readMaps("missing.json").isEmpty());
    }

    @Test
    void readMaps_emptyArray() throws IOException {
        Files.write(tempDir.resolve("data.json"), "[]".getBytes());
        assertTrue(new FileStore(tempDir).readMaps("data.json").isEmpty());
    }

    @Test
    void writeMaps_readMaps_roundTrip() throws IOException {
        FileStore store = new FileStore(tempDir);
        List<Map<String, String>> items = new ArrayList<>();
        Map<String, String> row = new LinkedHashMap<>();
        row.put("id", "1");
        row.put("name", "Alice");
        items.add(row);
        store.writeMaps("users.json", items);
        List<Map<String, String>> read = store.readMaps("users.json");
        assertEquals(1, read.size());
        assertEquals("Alice", read.get(0).get("name"));
    }

    @Test
    void parseJsonArray_escapedQuotesAndNewlines() {
        String json = "[{\"msg\":\"line1\\nline2\",\"quote\":\"say \\\"hi\\\"\"}]";
        List<Map<String, String>> rows = FileStore.parseJsonArray(json);
        assertEquals(1, rows.size());
        assertTrue(rows.get(0).get("msg").contains("\n"));
        assertTrue(rows.get(0).get("quote").contains("\""));
    }

    @Test
    void toJsonArrayOfObjects_escapesSpecialChars() {
        List<Map<String, String>> items = new ArrayList<>();
        Map<String, String> row = new LinkedHashMap<>();
        row.put("text", "a\"b\nc");
        items.add(row);
        String json = FileStore.toJsonArrayOfObjects(items);
        List<Map<String, String>> parsed = FileStore.parseJsonArray(json);
        assertEquals("a\"b\nc", parsed.get(0).get("text"));
    }

    @Test
    void parseJsonArray_multipleObjects() {
        String json = "[{\"a\":\"1\"},{\"a\":\"2\"}]";
        assertEquals(2, FileStore.parseJsonArray(json).size());
    }

    @Test
    void writeMaps_createsParentDirs() throws IOException {
        Path nested = tempDir.resolve("sub").resolve("dir");
        FileStore store = new FileStore(nested);
        store.writeMaps("file.json", new ArrayList<>());
        assertTrue(Files.exists(nested.resolve("file.json")));
    }
}
