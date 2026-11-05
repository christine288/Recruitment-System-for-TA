package com.bupt.ta.service.admin;

import com.bupt.ta.model.Application;
import com.bupt.ta.model.Job;
import com.bupt.ta.model.Roles;
import com.bupt.ta.model.TaWorkloadStats;
import com.bupt.ta.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Builds dashboard metrics and chart-ready JSON from users, applications, and jobs.
 *
 * <p>Pure static helpers — no I/O. Consumed by {@link com.bupt.ta.servlet.AdminServlet} for JSP
 * chart data and by {@link com.bupt.ta.service.ai.AdminAnalyticsService} for LM user prompts.
 * {@link #toChartJson} emits compact JSON embedded in the admin dashboard page for Chart.js.
 *
 * <p>This class is not instantiable.
 */
public final class AdminMetricsBuilder {

    private AdminMetricsBuilder() {}

    /**
     * Aggregates role counts, pipeline status, monthly trends, top modules, and workload alerts.
     *
     * @param users         all accounts (nullable → empty)
     * @param applications  all application rows (nullable → empty)
     * @param jobs          all job postings (nullable → empty)
     * @param taWorkload    per-TA stats from {@link com.bupt.ta.service.WorkloadService}
     * @return mutable metrics snapshot for charts and AI
     */
    public static AdminDashboardMetrics build(
            List<User> users,
            List<Application> applications,
            List<Job> jobs,
            Map<String, TaWorkloadStats> taWorkload) {
        AdminDashboardMetrics m = new AdminDashboardMetrics();
        if (users == null) users = Collections.emptyList();
        if (applications == null) applications = Collections.emptyList();
        if (jobs == null) jobs = Collections.emptyList();
        if (taWorkload == null) taWorkload = Collections.emptyMap();

        for (User u : users) {
            if (u == null) continue;
            String role = u.role != null ? u.role.trim().toUpperCase() : Roles.TA;
            m.usersByRole.put(role, m.usersByRole.getOrDefault(role, 0) + 1);
            if (u.active) {
                m.activeUsers++;
            } else {
                m.inactiveUsers++;
            }
        }
        m.totalUsers = users.size();
        m.totalJobs = jobs.size();
        m.totalApplications = applications.size();

        Map<String, Integer> appsByMonth = new TreeMap<>();
        Map<String, String> firstMonthByUser = new LinkedHashMap<>();

        for (Application a : applications) {
            if (a == null) continue;
            String rawSt = a.status == null ? "" : a.status.trim();
            String bucket;
            if ("Accepted".equalsIgnoreCase(rawSt)) bucket = "Accepted";
            else if ("Rejected".equalsIgnoreCase(rawSt)) bucket = "Rejected";
            else bucket = "Pending";
            m.appsByStatus.put(bucket, m.appsByStatus.getOrDefault(bucket, 0) + 1);

            String module = a.moduleName == null || a.moduleName.trim().isEmpty()
                    ? "Unknown" : a.moduleName.trim();
            m.appsByModule.put(module, m.appsByModule.getOrDefault(module, 0) + 1);

            String month = monthKey(a.applicationDate);
            appsByMonth.put(month, appsByMonth.getOrDefault(month, 0) + 1);

            if (a.userId != null && !a.userId.trim().isEmpty()) {
                String uid = a.userId.trim();
                String existing = firstMonthByUser.get(uid);
                if (existing == null || month.compareTo(existing) < 0) {
                    firstMonthByUser.put(uid, month);
                }
            }
        }

        Map<String, Set<String>> usersFirstSeenMonth = new TreeMap<>();
        for (Map.Entry<String, String> e : firstMonthByUser.entrySet()) {
            usersFirstSeenMonth
                    .computeIfAbsent(e.getValue(), k -> new LinkedHashSet<>())
                    .add(e.getKey());
        }

        List<String> months = new ArrayList<>(appsByMonth.keySet());
        Collections.sort(months);

        int cumulativeApplicants = 0;
        for (String month : months) {
            int appCount = appsByMonth.getOrDefault(month, 0);
            m.monthlyApplications.add(new AdminDashboardMetrics.SeriesPoint(month, appCount));

            Set<String> newUsers = usersFirstSeenMonth.getOrDefault(month, Collections.emptySet());
            cumulativeApplicants += newUsers.size();
            m.monthlyApplicantPool.add(new AdminDashboardMetrics.SeriesPoint(month, cumulativeApplicants));
            m.monthlyNewApplicants.add(new AdminDashboardMetrics.SeriesPoint(month, newUsers.size()));
        }

        if (m.monthlyApplicantPool.isEmpty() && m.totalUsers > 0) {
            m.monthlyApplicantPool.add(new AdminDashboardMetrics.SeriesPoint("All time", m.totalUsers));
            m.monthlyApplications.add(new AdminDashboardMetrics.SeriesPoint("All time", m.totalApplications));
        }

        List<Map.Entry<String, Integer>> topModules = new ArrayList<>(m.appsByModule.entrySet());
        topModules.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        int limit = Math.min(5, topModules.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Integer> e = topModules.get(i);
            m.topModules.add(new AdminDashboardMetrics.SeriesPoint(e.getKey(), e.getValue()));
        }

        for (TaWorkloadStats ws : taWorkload.values()) {
            if (ws != null && ws.accepted >= TaWorkloadStats.ASSIGNED_JOBS_WARNING_THRESHOLD) {
                m.highWorkloadTaCount++;
            }
        }

        return m;
    }

    /**
     * Serializes metrics to JSON for the {@code #admin-chart-data} script block.
     *
     * @param m metrics from {@link #build}; {@code null} yields {@code {}}
     * @return UTF-8 JSON string (not HTML-escaped)
     */
    public static String toChartJson(AdminDashboardMetrics m) {
        if (m == null) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"totalUsers\":").append(m.totalUsers).append(",");
        sb.append("\"activeUsers\":").append(m.activeUsers).append(",");
        sb.append("\"totalJobs\":").append(m.totalJobs).append(",");
        sb.append("\"totalApplications\":").append(m.totalApplications).append(",");
        sb.append("\"highWorkloadTaCount\":").append(m.highWorkloadTaCount).append(",");
        appendStringIntMap(sb, "usersByRole", m.usersByRole);
        sb.append(",");
        appendStringIntMap(sb, "appsByStatus", m.appsByStatus);
        sb.append(",");
        appendSeries(sb, "monthlyApplications", m.monthlyApplications);
        sb.append(",");
        appendSeries(sb, "monthlyApplicantPool", m.monthlyApplicantPool);
        sb.append(",");
        appendSeries(sb, "monthlyNewApplicants", m.monthlyNewApplicants);
        sb.append(",");
        appendSeries(sb, "topModules", m.topModules);
        sb.append("}");
        return sb.toString();
    }

    /**
     * Plain-text prompt section listing all metrics for {@link com.bupt.ta.service.ai.AdminAnalyticsService}.
     *
     * @param m metrics snapshot; {@code null} yields empty string
     * @return factual bullet-style text (no Markdown) for LM user prompt
     */
    public static String buildAnalyticsPrompt(AdminDashboardMetrics m) {
        if (m == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Platform snapshot:\n");
        sb.append("- Total users: ").append(m.totalUsers)
                .append(" (active: ").append(m.activeUsers)
                .append(", inactive: ").append(m.inactiveUsers).append(")\n");
        sb.append("- Total jobs: ").append(m.totalJobs).append("\n");
        sb.append("- Total applications: ").append(m.totalApplications).append("\n");
        sb.append("- TAs at/above workload warning: ").append(m.highWorkloadTaCount).append("\n\n");

        sb.append("Users by role:\n");
        for (Map.Entry<String, Integer> e : m.usersByRole.entrySet()) {
            sb.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }
        sb.append("\nApplications by status:\n");
        for (Map.Entry<String, Integer> e : m.appsByStatus.entrySet()) {
            sb.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }
        sb.append("\nMonthly applications:\n");
        appendSeriesLines(sb, m.monthlyApplications);
        sb.append("\nCumulative applicant pool (by first application month):\n");
        appendSeriesLines(sb, m.monthlyApplicantPool);
        sb.append("\nNew applicants per month:\n");
        appendSeriesLines(sb, m.monthlyNewApplicants);
        sb.append("\nTop modules:\n");
        appendSeriesLines(sb, m.topModules);
        return sb.toString();
    }

    private static void appendSeriesLines(StringBuilder sb, List<AdminDashboardMetrics.SeriesPoint> series) {
        if (series == null || series.isEmpty()) {
            sb.append("- (no data)\n");
            return;
        }
        for (AdminDashboardMetrics.SeriesPoint p : series) {
            sb.append("- ").append(p.label).append(": ").append(p.value).append("\n");
        }
    }

    private static void appendStringIntMap(StringBuilder sb, String key, Map<String, Integer> map) {
        sb.append("\"").append(key).append("\":{");
        boolean first = true;
        if (map != null) {
            for (Map.Entry<String, Integer> e : map.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(escapeJson(e.getKey())).append("\":").append(e.getValue());
            }
        }
        sb.append("}");
    }

    private static void appendSeries(StringBuilder sb, String key, List<AdminDashboardMetrics.SeriesPoint> series) {
        sb.append("\"").append(key).append("\":[");
        if (series != null) {
            for (int i = 0; i < series.size(); i++) {
                if (i > 0) sb.append(",");
                AdminDashboardMetrics.SeriesPoint p = series.get(i);
                sb.append("{\"label\":\"").append(escapeJson(p.label))
                        .append("\",\"value\":").append(p.value).append("}");
            }
        }
        sb.append("]");
    }

    private static String monthKey(String date) {
        if (date != null && date.length() >= 7) {
            return date.substring(0, 7);
        }
        return "Unknown";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
