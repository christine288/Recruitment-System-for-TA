package com.bupt.ta.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Writes audit trail entries to {@code audit-logs.json}.
 *
 * <p>Each log row captures who performed an action, what changed, and when (ISO-8601 timestamp).
 * Used by admin flows such as forced status changes and user management for coursework traceability.
 *
 * <p>Append-only style writes through {@link FileStore}; not thread-safe under concurrent POSTs.
 */
public class AuditService {

    private static final String AUDIT_LOGS_JSON = "audit-logs.json";
    private final FileStore store;

    /** @param dataDir directory containing {@code audit-logs.json} */
    public AuditService(Path dataDir) {
        this.store = new FileStore(dataDir);
    }

    /** Appends one audit row with a generated id and current timestamp. */
    public void log(String actorId, String action, String targetType, String targetId, String details)
            throws IOException {
        List<Map<String, String>> rows = store.readMaps(AUDIT_LOGS_JSON);
        Map<String, String> row = new LinkedHashMap<>();
        row.put("id", UUID.randomUUID().toString());
        row.put("actorId", actorId != null ? actorId : "");
        row.put("action", action != null ? action : "");
        row.put("targetType", targetType != null ? targetType : "");
        row.put("targetId", targetId != null ? targetId : "");
        row.put("details", details != null ? details : "");
        row.put("createdAt", Instant.now().toString());
        rows.add(row);
        store.writeMaps(AUDIT_LOGS_JSON, rows);
    }
}
