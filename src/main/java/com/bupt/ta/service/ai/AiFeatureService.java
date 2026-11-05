package com.bupt.ta.service.ai;

import com.bupt.ta.ai.LmClient;
import com.bupt.ta.ai.LmConfig;
import com.bupt.ta.ai.LmException;
import com.bupt.ta.ai.LmResponse;
import com.bupt.ta.model.MoWorkloadSnapshot;

/**
 * Application-facing entry point for all AI-powered recruitment features.
 * <p>
 * Servlets and controllers should depend on this facade rather than on {@link LmClient}
 * or individual feature services directly. Each public method checks whether AI is
 * enabled, delegates to the appropriate specialist service, and returns a safe
 * {@link AiFeatureOutput} without propagating {@link LmException} to the web layer.
 */
public final class AiFeatureService {
    private final LmConfig config;
    private final SkillMatchService skillMatch;
    private final MissingSkillService missingSkill;
    private final RecommendationService recommendation;
    private final WorkloadAdviceService workloadAdvice;

    /**
     * Wires feature-specific services with the shared LM client and configuration.
     *
     * @param config runtime LM settings (enabled flag, model, provider)
     * @param client client used for synchronous generation calls
     */
    public AiFeatureService(LmConfig config, LmClient client) {
        this.config = config;
        this.skillMatch = new SkillMatchService(client, config);
        this.missingSkill = new MissingSkillService(client, config);
        this.recommendation = new RecommendationService(client, config);
        this.workloadAdvice = new WorkloadAdviceService(client, config);
    }

    /**
     * Compares applicant skills against job requirements and returns a structured match analysis.
     *
     * @param applicantSkills free-text skills from the applicant profile
     * @param jobRequirements required skills or description from the job posting
     * @return success output with Markdown sections, or error/disabled output
     */
    public AiFeatureOutput matchApplicantSkills(String applicantSkills, String jobRequirements) {
        return run(() -> skillMatch.matchApplicantSkills(applicantSkills, jobRequirements), "Skill match failed: ");
    }

    /**
     * Identifies skills required by the job that the candidate does not list.
     *
     * @param candidateSkills   skills text from the applicant
     * @param requiredJobSkills required skills from the job posting
     * @return success output with gap analysis, or error/disabled output
     */
    public AiFeatureOutput identifyMissingSkills(String candidateSkills, String requiredJobSkills) {
        return run(() -> missingSkill.identifyMissingSkills(candidateSkills, requiredJobSkills),
                "Missing-skill analysis failed: ");
    }

    /**
     * Ranks open TA positions for the candidate based on profile and postings text.
     *
     * @param candidateProfile summary of the applicant (skills, courses, availability)
     * @param openPositions    formatted list of published job summaries
     * @return success output with ranked recommendations, or error/disabled output
     */
    public AiFeatureOutput recommendJobs(String candidateProfile, String openPositions) {
        return run(() -> recommendation.recommendJobs(candidateProfile, openPositions), "Job recommendation failed: ");
    }

    /**
     * Generates MO-facing workload approval advice from a precomputed snapshot.
     *
     * @param snapshot workload context for one pending application
     * @return success output with recommendation Markdown, or error/disabled output
     */
    public AiFeatureOutput adviseMoOnWorkload(MoWorkloadSnapshot snapshot) {
        return run(() -> workloadAdvice.adviseMoOnWorkload(snapshot), "Workload advice failed: ");
    }

    private AiFeatureOutput run(LmCall call, String errPrefix) {
        if (!config.isEnabled()) {
            return AiFeatureOutput.disabled();
        }
        try {
            return AiFeatureOutput.fromResponse(call.get());
        } catch (LmException e) {
            return AiFeatureOutput.error(errPrefix + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface LmCall {
        LmResponse get() throws LmException;
    }
}
