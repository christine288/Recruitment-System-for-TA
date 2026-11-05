package com.bupt.ta.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-application workload context assembled for module-owner (MO) approval and AI advice.
 *
 * <p>Built at request time by {@link com.bupt.ta.service.WorkloadService}; not persisted.
 * Summarizes how approving one pending application would affect the applicant's total load
 * relative to {@link TaWorkloadStats#ASSIGNED_JOBS_WARNING_THRESHOLD}.
 *
 * <p>Not thread-safe: list fields are mutable {@link ArrayList} instances populated during
 * snapshot construction; intended for single-threaded servlet request handling.
 */
public class MoWorkloadSnapshot {

    /** {@link Application#id} of the application under review. */
    public String applicationId;
    /** {@link User#id} of the applicant. */
    public String userId;
    /** Display name of the applicant for MO UI. */
    public String applicantName;
    /** Module name of the job tied to the pending application. */
    public String targetModuleName;
    /** Module code of the job tied to the pending application. */
    public String targetModuleCode;
    /** Role on the target application (e.g. Teaching Assistant). */
    public String targetRole;
    /** Expected hours from {@link Job#getWorkloadHours()} for the target job. */
    public String targetWorkloadHours;

    /** Number of the applicant's currently accepted applications. */
    public int acceptedCount;
    /** Number of the applicant's pending applications (includes the one under review). */
    public int pendingCount;
    /**
     * {@link #acceptedCount} + {@link #pendingCount}; models load if all pending apps are approved.
     */
    public int potentialLoadIfApprove;
    /**
     * Threshold at which workload is considered high; defaults to
     * {@link TaWorkloadStats#ASSIGNED_JOBS_WARNING_THRESHOLD}.
     */
    public int warningThreshold = TaWorkloadStats.ASSIGNED_JOBS_WARNING_THRESHOLD;

    /** Human-readable lines for each accepted position (see {@link TaWorkloadStats#formatAcceptedLine}). */
    public final List<String> acceptedPositions = new ArrayList<>();
    /** Human-readable lines for each pending position. */
    public final List<String> pendingPositions = new ArrayList<>();
    /**
     * Parallel hints for {@link #acceptedPositions}: module code mapped to hours or {@code "unknown"}.
     */
    public final List<String> acceptedHoursHints = new ArrayList<>();
    /**
     * Parallel hints for {@link #pendingPositions}: module code mapped to hours or {@code "unknown"}.
     */
    public final List<String> pendingHoursHints = new ArrayList<>();
}
