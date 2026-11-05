package com.bupt.ta.model;

/**
 * Context for module-organiser (MO) retrospective AI review of a processed application.
 *
 * <p>Built at request time by {@link com.bupt.ta.service.ai.ProcessedDecisionReviewService#buildContext}
 * when an MO views an Accepted, Rejected, or Withdrawn application on the MO dashboard.
 * Not persisted — fields are copied from {@link Application}, optional {@link Job}, and
 * {@link ApplicantProfile} for LM prompt construction.
 *
 * <p>Not thread-safe: plain public fields for servlet request scope only.
 *
 * @see com.bupt.ta.servlet.AiStreamServlet
 * @see com.bupt.ta.ai.AiFeatureNames#DECISION_REVIEW
 */
public class MoProcessedReviewContext {

    /** {@link Application#id} under review. */
    public String applicationId;
    /** Display name resolved from user or profile. */
    public String applicantName;
    /** Module name from the application record. */
    public String moduleName;
    /** Module code from the application record. */
    public String moduleCode;
    /** Role applied for (e.g. Lab TA). */
    public String role;
    /** ISO date string of submission. */
    public String applicationDate;
    /** Recorded outcome: Accepted, Rejected, or Withdrawn. */
    public String decisionStatus;
    /** MO feedback text stored on the application, if any. */
    public String moFeedback;

    /** Job description from matched posting, when found. */
    public String jobDescription;
    /** Activity type from matched job. */
    public String jobActivityType;
    /** Expected workload hours from matched job. */
    public String workloadHours;
    /** Comma-separated required skills from matched job. */
    public String jobSkills;

    /** Flattened skills/courses text from {@link com.bupt.ta.service.ProfileService#buildAiCapabilityText}. */
    public String applicantCapabilities;
}
