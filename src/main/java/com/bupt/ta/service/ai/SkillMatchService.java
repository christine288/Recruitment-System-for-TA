package com.bupt.ta.service.ai;

import com.bupt.ta.ai.AiFeatureNames;
import com.bupt.ta.ai.LmClient;
import com.bupt.ta.ai.LmConfig;
import com.bupt.ta.ai.LmException;
import com.bupt.ta.ai.LmRequest;
import com.bupt.ta.ai.LmResponse;

/**
 * Compares applicant skills against job requirements via structured LM prompts.
 * <p>
 * Implements the {@link AiFeatureNames#SKILL_MATCH} feature. Builds system and user
 * prompts requesting Markdown sections for summary, matched skills, gaps, and next steps,
 * then delegates generation to {@link LmClient}.
 */
public final class SkillMatchService {
    private final LmClient client;
    private final LmConfig config;

    /**
     * Creates a service bound to the given LM client and configuration.
     *
     * @param client LM client for synchronous generation
     * @param config runtime settings (model, provider)
     */
    public SkillMatchService(LmClient client, LmConfig config) {
        this.client = client;
        this.config = config;
    }

    /**
     * Calls the language model to produce a skill match analysis.
     *
     * @param applicantSkills free-text skills from the applicant profile
     * @param jobRequirements job requirements or required skills text
     * @return raw LM response with comparison Markdown
     * @throws LmException if the client rejects or fails the request
     */
    public LmResponse matchApplicantSkills(String applicantSkills, String jobRequirements) throws LmException {
        return client.generate(buildMatchRequest(applicantSkills, jobRequirements));
    }

    /**
     * Builds the {@link LmRequest} for skill matching without invoking the client.
     * <p>
     * Useful for streaming endpoints that reuse the same prompts as synchronous calls.
     *
     * @param applicantSkills applicant skills text
     * @param jobRequirements job requirements text
     * @return configured request with system/user prompts and token limits
     */
    public LmRequest buildMatchRequest(String applicantSkills, String jobRequirements) {
        return LmRequest.builder()
                .featureName(AiFeatureNames.SKILL_MATCH)
                .systemPrompt("You are a TA recruitment assistant.\n"
                        + "Return concise Markdown with sections:\n"
                        + "## Comparison Summary\n"
                        + "## Matched Skills\n"
                        + "## Missing Skills\n"
                        + "## Recommended Next Steps\n"
                        + "Use bullet points and keep under 180 words.")
                .userPrompt("Applicant skills:\n" + AiLmDefaults.nz(applicantSkills) + "\n\nJob requirements:\n"
                        + AiLmDefaults.nz(jobRequirements))
                .temperature(0.2d)
                .maxTokens(512)
                .model(AiLmDefaults.configuredModel(config))
                .build();
    }
}
