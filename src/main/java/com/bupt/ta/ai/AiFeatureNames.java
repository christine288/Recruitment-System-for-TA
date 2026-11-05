package com.bupt.ta.ai;

/**
 * Canonical string identifiers for AI-powered recruitment features in the TA system.
 *
 * <p>These keys are passed via {@link LmRequest#getFeatureName()} and used by
 * {@link MockLmClient} to route deterministic mock responses, by AI service classes
 * for prompt construction and logging, and as stable contract values across the
 * LM integration layer. Prefer these constants over literal strings so feature
 * names stay consistent between servlets, services, and mock routing.
 *
 * <p>This class is not instantiable; use the {@code public static final} constants only.
 *
 * @see LmRequest.Builder#featureName(String)
 * @see MockLmClient#generate(LmRequest)
 */
public final class AiFeatureNames {

    /** Feature key for skill overlap scoring between an applicant and a job posting. */
    public static final String SKILL_MATCH = "skill-match";

    /** Feature key for listing skills the candidate lacks relative to job requirements. */
    public static final String MISSING_SKILLS = "missing-skills";

    /** Feature key for ranked job recommendations based on candidate profile. */
    public static final String JOB_RECOMMENDATION = "job-recommendation";

    /** Feature key for module-organiser workload approval advice (approve / caution / reject). */
    public static final String WORKLOAD_ADVICE = "workload-advice";

    /** Feature key for MO retrospective review of processed application decisions. */
    public static final String DECISION_REVIEW = "decision-review";

    /** Feature key for admin dashboard platform analytics narrative. */
    public static final String ADMIN_ANALYTICS = "admin-analytics";

    /** Prevents instantiation of this constants-only utility class. */
    private AiFeatureNames() {}
}
