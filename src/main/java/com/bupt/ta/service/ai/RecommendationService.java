package com.bupt.ta.service.ai;

import com.bupt.ta.ai.AiFeatureNames;
import com.bupt.ta.ai.LmClient;
import com.bupt.ta.ai.LmConfig;
import com.bupt.ta.ai.LmException;
import com.bupt.ta.ai.LmRequest;
import com.bupt.ta.ai.LmResponse;

/**
 * Ranks open TA positions for a candidate using profile and job listing text.
 * <p>
 * Implements the {@link AiFeatureNames#JOB_RECOMMENDATION} feature. The model returns
 * Markdown with top recommendations, fit rationale, and risk notes for up to three jobs.
 */
public final class RecommendationService {
    private final LmClient client;
    private final LmConfig config;

    /**
     * Creates a service bound to the given LM client and configuration.
     *
     * @param client LM client for synchronous generation
     * @param config runtime settings (model, provider)
     */
    public RecommendationService(LmClient client, LmConfig config) {
        this.client = client;
        this.config = config;
    }

    /**
     * Calls the language model to recommend suitable open positions for the candidate.
     *
     * @param candidateProfile applicant capability summary (skills, courses, etc.)
     * @param openPositions    formatted text describing published job postings
     * @return raw LM response with ranked recommendations
     * @throws LmException if the client rejects or fails the request
     */
    public LmResponse recommendJobs(String candidateProfile, String openPositions) throws LmException {
        return client.generate(buildRecommendRequest(candidateProfile, openPositions));
    }

    /**
     * Builds the {@link LmRequest} for job recommendations without invoking the client.
     * <p>
     * Uses the same prompts as {@link #recommendJobs(String, String)} for streaming endpoints.
     *
     * @param candidateProfile applicant profile text
     * @param openPositions    open job listings text
     * @return configured request with system/user prompts and token limits
     */
    public LmRequest buildRecommendRequest(String candidateProfile, String openPositions) {
        return LmRequest.builder()
                .featureName(AiFeatureNames.JOB_RECOMMENDATION)
                .systemPrompt("You recommend TA positions based on the candidate profile and open postings.\n"
                        + "Return concise Markdown with sections:\n"
                        + "## Top Recommendations\n"
                        + "## Why These Fit\n"
                        + "## Risks / Notes\n"
                        + "Rank up to 3 jobs and use bullet points.")
                .userPrompt("Candidate profile:\n" + AiLmDefaults.nz(candidateProfile) + "\n\nOpen positions:\n"
                        + AiLmDefaults.nz(openPositions))
                .temperature(0.35d)
                .maxTokens(700)
                .model(AiLmDefaults.configuredModel(config))
                .build();
    }
}
