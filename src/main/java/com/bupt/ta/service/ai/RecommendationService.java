package com.bupt.ta.service.ai;

import com.bupt.ta.ai.AiFeatureNames;
import com.bupt.ta.ai.LmClient;
import com.bupt.ta.ai.LmConfig;
import com.bupt.ta.ai.LmException;
import com.bupt.ta.ai.LmRequest;
import com.bupt.ta.ai.LmResponse;

/** Ranks open positions for a candidate (mock rules or LM-generated narrative). */
public final class RecommendationService {
    private final LmClient client;
    private final LmConfig config;

    public RecommendationService(LmClient client, LmConfig config) {
        this.client = client;
        this.config = config;
    }

    public LmResponse recommendJobs(String candidateProfile, String openPositions) throws LmException {
        return client.generate(buildRecommendRequest(candidateProfile, openPositions));
    }

    /** Same prompts as {@link #recommendJobs} — for streaming endpoints. */
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
