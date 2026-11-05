package com.bupt.ta.service.admin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated metrics for the admin dashboard charts and AI analytics briefing.
 *
 * <p>Populated by {@link AdminMetricsBuilder#build} from in-memory lists of users, applications,
 * jobs, and TA workload stats. Not persisted — rebuilt on each {@code GET /admin} request.
 *
 * <p>Public fields are mutable containers filled during build; treat instances as request-scoped
 * snapshots rather than shared long-lived state.
 *
 * @see AdminMetricsBuilder#toChartJson(AdminDashboardMetrics)
 * @see com.bupt.ta.service.ai.AdminAnalyticsService
 */
public class AdminDashboardMetrics {

    /** Default constructor for metrics aggregation. */
    public AdminDashboardMetrics() {}

    /** Total registered accounts across all roles. */
    public int totalUsers;
    /** Accounts with {@code active=true}. */
    public int activeUsers;
    /** Accounts with {@code active=false}. */
    public int inactiveUsers;
    /** Count of job postings in {@code jobs.json}. */
    public int totalJobs;
    /** Count of rows in {@code applications.json}. */
    public int totalApplications;
    /** TAs at or above {@link com.bupt.ta.model.TaWorkloadStats#ASSIGNED_JOBS_WARNING_THRESHOLD}. */
    public int highWorkloadTaCount;

    /** Role name (e.g. TA, MO, ADMIN) to user count. */
    public final Map<String, Integer> usersByRole = new LinkedHashMap<>();
    /** Pipeline bucket (Pending / Accepted / Rejected) to application count. */
    public final Map<String, Integer> appsByStatus = new LinkedHashMap<>();
    /** Applications submitted per calendar month ({@code YYYY-MM} label). */
    public final List<SeriesPoint> monthlyApplications = new ArrayList<>();
    /** Cumulative unique applicants by first-application month. */
    public final List<SeriesPoint> monthlyApplicantPool = new ArrayList<>();
    /** New unique applicants per month (first application in that month). */
    public final List<SeriesPoint> monthlyNewApplicants = new ArrayList<>();
    /** Top modules by application volume (max five entries). */
    public final List<SeriesPoint> topModules = new ArrayList<>();
    /** All modules to application count (superset used to derive {@link #topModules}). */
    public final Map<String, Integer> appsByModule = new LinkedHashMap<>();

    /**
     * Label/value pair for time-series and bar charts (Chart.js) and AI prompt lines.
     */
    public static final class SeriesPoint {
        /** X-axis or category label (month, module name, etc.). */
        public final String label;
        /** Numeric metric for the label. */
        public final int value;

        /**
         * Creates a chart data point.
         *
         * @param label chart category
         * @param value metric count
         */
        public SeriesPoint(String label, int value) {
            this.label = label;
            this.value = value;
        }
    }
}
