package com.bupt.ta.model;

import java.util.List;

/**
 * Immutable aggregated application counts for one job, matched by module name and code.
 *
 * <p>Computed in memory from the full applications list; not persisted. Used on MO and admin
 * dashboards to show pipeline status and capacity relative to {@link Job#getNumberOfTAs()}.
 *
 * <p>Thread-safe for read-only use after construction; factory methods create new instances
 * and do not mutate the input list.
 */
public final class JobApplicationStats {
    /** Count of applications still awaiting a decision ({@code Pending} or unknown status). */
    public final int pending;
    /** Count of applications with status {@code Accepted}. */
    public final int accepted;
    /** Count of applications with status {@code Rejected}. */
    public final int rejected;
    /** Count of applications with status {@code Withdrawn}. */
    public final int withdrawn;

    private JobApplicationStats(int pending, int accepted, int rejected, int withdrawn) {
        this.pending = pending;
        this.accepted = accepted;
        this.rejected = rejected;
        this.withdrawn = withdrawn;
    }

    /**
     * Total applications still in the decision pipeline (excludes withdrawn).
     *
     * @return sum of {@link #pending}, {@link #accepted}, and {@link #rejected}
     */
    public int activeTotal() {
        return pending + accepted + rejected;
    }

    /**
     * Builds stats for all applications matching the given module name and code.
     *
     * <p>Status matching is case-insensitive. Null or unrecognized statuses count as pending.
     * A {@code null} application list yields zero counts.
     *
     * @param all         full application list (typically from {@code applications.json})
     * @param moduleName  target module name (trimmed comparison)
     * @param moduleCode  target module code (trimmed; null treated as empty string)
     * @return aggregated counts for the matching job
     */
    public static JobApplicationStats forJob(List<Application> all, String moduleName, String moduleCode) {
        int p = 0, a = 0, r = 0, w = 0;
        if (all == null) {
            return new JobApplicationStats(0, 0, 0, 0);
        }
        for (Application app : all) {
            if (!matchesJob(app, moduleName, moduleCode)) {
                continue;
            }
            String st = app.status == null ? "Pending" : app.status.trim();
            if ("Withdrawn".equalsIgnoreCase(st)) {
                w++;
            } else if ("Accepted".equalsIgnoreCase(st)) {
                a++;
            } else if ("Rejected".equalsIgnoreCase(st)) {
                r++;
            } else {
                p++;
            }
        }
        return new JobApplicationStats(p, a, r, w);
    }

    /**
     * Returns whether an application targets the same module as the given job identifiers.
     *
     * @param app         application to test; {@code null} or missing module name yields {@code false}
     * @param moduleName  expected module name (required; null yields {@code false})
     * @param moduleCode  expected module code (null treated as empty string)
     * @return {@code true} if module name and code match after trimming
     */
    public static boolean matchesJob(Application app, String moduleName, String moduleCode) {
        if (app == null || app.moduleName == null || moduleName == null) {
            return false;
        }
        if (!moduleName.trim().equals(app.moduleName.trim())) {
            return false;
        }
        String jc = moduleCode == null ? "" : moduleCode.trim();
        String ac = app.moduleCode == null ? "" : app.moduleCode.trim();
        return jc.equals(ac);
    }

    /**
     * Parses TA headcount from a job's {@link Job#getNumberOfTAs()} string.
     *
     * <p>Non-digit characters are stripped before parsing. Invalid, empty, or values below 1
     * default to {@code 2}.
     *
     * @param numberOfTAs raw headcount string from the job record
     * @return parsed capacity, at least 1, defaulting to 2 when input is unusable
     */
    public static int parseCapacity(String numberOfTAs) {
        if (numberOfTAs == null || numberOfTAs.trim().isEmpty()) {
            return 2;
        }
        try {
            int n = Integer.parseInt(numberOfTAs.trim().replaceAll("[^0-9]", ""));
            return n < 1 ? 2 : n;
        } catch (NumberFormatException e) {
            return 2;
        }
    }
}
