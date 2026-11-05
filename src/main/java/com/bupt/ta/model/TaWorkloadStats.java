package com.bupt.ta.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-TA application statistics and human-readable position summaries.
 *
 * <p>Aggregated in memory by {@link com.bupt.ta.service.WorkloadService} from
 * {@code applications.json}; not persisted as a separate entity. Accepted and rejected
 * application lists hold one formatted line per matching {@link Application}.
 *
 * <p>Not thread-safe: mutable counters and lists are populated during a single request.
 */
public class TaWorkloadStats {

    /**
     * Accepted applications count toward assigned jobs; MO workload advice warns when
     * an applicant's accepted count is at or above this value.
     */
    public static final int ASSIGNED_JOBS_WARNING_THRESHOLD = 3;

    /** Total applications submitted by the TA (all statuses). */
    public int total;
    /** Count with status {@code Pending}. */
    public int pending;
    /** Count with status {@code Accepted}. */
    public int accepted;
    /** Count with status {@code Rejected}. */
    public int rejected;
    /** One display line per accepted application; see {@link #formatAcceptedLine(Application)}. */
    public final List<String> acceptedPositions = new ArrayList<>();
    /** One display line per rejected application (same format as accepted). */
    public final List<String> rejectedPositions = new ArrayList<>();

    /**
     * Formats a single application as a compact summary line for workload displays.
     *
     * <p>Layout: {@code Module [code] - role | date}. Missing parts are omitted; unknown
     * module name becomes {@code (Unknown module)}. Returns empty string for {@code null}
     * application.
     *
     * @param application application to format, or {@code null}
     * @return formatted line suitable for {@link #acceptedPositions} or {@link #rejectedPositions}
     */
    public static String formatAcceptedLine(Application application) {
        if (application == null) {
            return "";
        }
        String mod = application.moduleName != null ? application.moduleName.trim() : "";
        String code = application.moduleCode != null ? application.moduleCode.trim() : "";
        String role = application.role != null ? application.role.trim() : "";
        String date = application.applicationDate != null ? application.applicationDate.trim() : "";
        StringBuilder sb = new StringBuilder();
        sb.append(mod.isEmpty() ? "(Unknown module)" : mod);
        if (!code.isEmpty()) {
            sb.append(" [").append(code).append("]");
        }
        if (!role.isEmpty()) {
            sb.append(" - ").append(role);
        }
        if (!date.isEmpty()) {
            sb.append(" | ").append(date);
        }
        return sb.toString();
    }
}
