package com.bupt.ta.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Tracks per-user recently viewed job postings in {@code recently-viewed.json}.
 *
 * <p>Maintains a bounded list (see {@link #MAX_RECENT}) of job IDs per TA, ordered by most recent
 * view time. Used on the job list page for quick access without persisting full job snapshots.
 *
 * @see com.bupt.ta.servlet.JobServlet
 */
public class RecentlyViewedService {

    /** Maximum number of distinct recently viewed jobs retained per user. */
    public static final int MAX_RECENT = 3;

    private static final String RECENTLY_VIEWED_JSON = "recently-viewed.json";
    private static final AtomicLong VIEW_SEQUENCE = new AtomicLong();

    private final FileStore store;

    /** @param dataDir directory containing {@code recently-viewed.json} */
    public RecentlyViewedService(Path dataDir) {
        this.store = new FileStore(dataDir);
    }

    /** Returns up to {@link #MAX_RECENT} job ids for the user, most recent first. */
    public List<String> getRecentJobIds(String userId) throws IOException {
        if (userId == null || userId.isEmpty()) {
            return new ArrayList<>();
        }
        return findByUserId(userId).stream()
                .sorted(Comparator.comparing((RecentViewRow r) -> r.viewedAt).reversed())
                .map(r -> r.jobId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .limit(MAX_RECENT)
                .collect(Collectors.toList());
    }

    /** Records a job view and trims older entries beyond {@link #MAX_RECENT}. */
    public void recordView(String userId, String jobId) throws IOException {
        if (userId == null || userId.isEmpty() || jobId == null || jobId.isEmpty()) {
            return;
        }
        List<Map<String, String>> rows = store.readMaps(RECENTLY_VIEWED_JSON);
        rows.removeIf(m -> userId.equals(m.get("userId")) && jobId.equals(m.get("jobId")));

        Map<String, String> row = new LinkedHashMap<>();
        row.put("userId", userId);
        row.put("jobId", jobId);
        row.put("viewedAt", String.format("%019d", VIEW_SEQUENCE.incrementAndGet()));
        rows.add(row);

        List<String> keepIds = rows.stream()
                .filter(m -> userId.equals(m.get("userId")))
                .map(m -> new RecentViewRow(m.get("userId"), m.get("jobId"), m.get("viewedAt")))
                .sorted(Comparator.comparing((RecentViewRow r) -> r.viewedAt).reversed())
                .map(r -> r.jobId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .limit(MAX_RECENT)
                .collect(Collectors.toList());

        rows.removeIf(m -> userId.equals(m.get("userId"))
                && !keepIds.contains(m.get("jobId")));
        store.writeMaps(RECENTLY_VIEWED_JSON, rows);
    }

    private List<RecentViewRow> findByUserId(String userId) throws IOException {
        List<RecentViewRow> out = new ArrayList<>();
        for (Map<String, String> m : store.readMaps(RECENTLY_VIEWED_JSON)) {
            if (userId.equals(m.get("userId"))) {
                out.add(new RecentViewRow(m.get("userId"), m.get("jobId"), m.get("viewedAt")));
            }
        }
        return out;
    }

    private static final class RecentViewRow {
        final String userId;
        final String jobId;
        final String viewedAt;

        RecentViewRow(String userId, String jobId, String viewedAt) {
            this.userId = userId;
            this.jobId = jobId;
            this.viewedAt = viewedAt;
        }
    }
}
