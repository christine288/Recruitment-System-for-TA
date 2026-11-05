package com.bupt.ta.util;

import com.bupt.ta.model.Job;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared search, sort, and favorited-only filtering for published job browsing lists.
 *
 * <p>Used by TA-facing job servlets to apply query parameters consistently. Operates on in-memory
 * {@link Job} lists already loaded from {@code jobs.json}; does not persist changes.
 *
 * <p>Thread-safe: static methods do not hold mutable state; callers should not mutate input lists
 * concurrently during a filter operation.
 */
public final class JobListFilters {

    private JobListFilters() {
    }

    /**
     * Applies sort mode, optional favorited-only filter, text search, and sorting to a job list.
     *
     * <p>When {@code sortBy} is {@code "favorited"}, only jobs whose ids appear in
     * {@code favoriteJobIds} are kept, then results are sorted by posting date (newest first).
     * Empty or null {@code sortBy} defaults to {@code "postingDate"}.
     *
     * <p>Search ({@code q}) matches module name, activity type, or any required skill
     * (case-insensitive substring).
     *
     * @param published       published jobs to filter; not modified in place—a new list is returned
     * @param favoriteJobIds  set of favorited job ids for {@code "favorited"} mode; may be empty
     * @param q               free-text search needle; null or empty skips search
     * @param sortBy          sort key: {@code "moduleName"}, {@code "activityType"},
     *                        {@code "favorited"}, or default posting date (newest first)
     * @return filtered and sorted job list
     */
    public static List<Job> apply(List<Job> published, Set<String> favoriteJobIds, String q, String sortBy) {
        String mode = safe(sortBy);
        if (mode.isEmpty()) {
            mode = "postingDate";
        }
        List<Job> jobs = published;
        if ("favorited".equals(mode)) {
            jobs = jobs.stream()
                    .filter(j -> j.getId() != null && favoriteJobIds.contains(j.getId()))
                    .collect(Collectors.toList());
            mode = "postingDate";
        }
        jobs = applySearch(jobs, q);
        return applySort(jobs, mode);
    }

    private static List<Job> applySearch(List<Job> jobs, String q) {
        if (q == null || q.isEmpty()) {
            return jobs;
        }
        String needle = q.toLowerCase(Locale.ROOT);
        return jobs.stream()
                .filter(j -> contains(j.getModuleName(), needle)
                        || contains(j.getActivityType(), needle)
                        || (j.getRequiredSkills() != null
                        && j.getRequiredSkills().stream().anyMatch(s -> contains(s, needle))))
                .collect(Collectors.toList());
    }

    private static List<Job> applySort(List<Job> jobs, String sortBy) {
        Comparator<Job> cmp;
        if ("moduleName".equals(sortBy)) {
            cmp = Comparator.comparing(j -> lower(j.getModuleName()));
        } else if ("activityType".equals(sortBy)) {
            cmp = Comparator.comparing(j -> lower(j.getActivityType()));
        } else {
            cmp = Comparator.comparing((Job j) -> lower(j.getPostDate())).reversed();
        }
        return jobs.stream().sorted(cmp).collect(Collectors.toList());
    }

    private static boolean contains(String s, String needle) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Moves recently viewed jobs to the front of the list while preserving relative order elsewhere.
     *
     * <p>Jobs are promoted in {@code recentJobIds} order (most recent id first). Only jobs
     * present in both {@code jobs} and {@code recentJobIds} are moved; duplicates in
     * {@code recentJobIds} are skipped after the first occurrence.
     *
     * @param jobs          full job list in current display order; may be {@code null}
     * @param recentJobIds  job ids from most-recent to least-recent view; may be {@code null}
     * @return reordered list, or the original {@code jobs} reference when nothing to promote
     */
    public static List<Job> promoteRecentlyViewed(List<Job> jobs, List<String> recentJobIds) {
        if (jobs == null || jobs.isEmpty() || recentJobIds == null || recentJobIds.isEmpty()) {
            return jobs;
        }
        Set<String> placed = new HashSet<>();
        List<Job> front = new ArrayList<>();
        for (String id : recentJobIds) {
            if (id == null || id.isEmpty() || placed.contains(id)) {
                continue;
            }
            for (Job job : jobs) {
                if (id.equals(job.getId())) {
                    front.add(job);
                    placed.add(id);
                    break;
                }
            }
        }
        if (front.isEmpty()) {
            return jobs;
        }
        List<Job> rest = jobs.stream()
                .filter(j -> j.getId() == null || !placed.contains(j.getId()))
                .collect(Collectors.toList());
        List<Job> merged = new ArrayList<>(front.size() + rest.size());
        merged.addAll(front);
        merged.addAll(rest);
        return merged;
    }
}
