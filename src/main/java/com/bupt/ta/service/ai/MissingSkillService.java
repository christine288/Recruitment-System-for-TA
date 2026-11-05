package com.bupt.ta.service.ai;

import com.bupt.ta.ai.AiFeatureNames;
import com.bupt.ta.ai.LmClient;
import com.bupt.ta.ai.LmConfig;
import com.bupt.ta.ai.LmException;
import com.bupt.ta.ai.LmRequest;
import com.bupt.ta.ai.LmResponse;

/**
 * Identifies skill gaps between a candidate's stated skills and job requirements.
 * <p>
 * Builds structured prompts for the {@link AiFeatureNames#MISSING_SKILLS} feature and
 * delegates generation to {@link LmClient}. Output is Markdown with gap summary,
 * missing skills, and learning priority sections.
 */
public final class MissingSkillService {
    private final LmClient client;
    private final LmConfig config;

    /**
     * Creates a service bound to the given LM client and configuration.
     *
     * @param client LM client for synchronous generation
     * @param config runtime settings (model, provider)
     */
    public MissingSkillService(LmClient client, LmConfig config) {
        this.client = client;
        this.config = config;
    }

    /**
     * Calls the language model to list skills required by the job that the candidate lacks.
     *
     * @param candidateSkills   free-text skills from the applicant profile
     * @param requiredJobSkills required skills from the job posting
     * @return raw LM response including generated Markdown
     * @throws LmException if the client rejects or fails the request
     */
    public LmResponse identifyMissingSkills(String candidateSkills, String requiredJobSkills) throws LmException {
        return client.generate(buildMissingRequest(candidateSkills, requiredJobSkills));
    }

    /**
     * Builds the {@link LmRequest} for missing-skill analysis without invoking the client.
     * <p>
     * Useful for streaming endpoints that reuse the same prompts as synchronous calls.
     *
     * @param candidateSkills   applicant skills text
     * @param requiredJobSkills job required skills text
     * @return configured request with system/user prompts and token limits
     */
    public LmRequest buildMissingRequest(String candidateSkills, String requiredJobSkills) {
        return LmRequest.builder()
                .featureName(AiFeatureNames.MISSING_SKILLS)
                .systemPrompt("You identify skill gaps for TA recruitment screening.\n"
                        + "Return concise Markdown with sections:\n"
                        + "## Gap Summary\n"
                        + "## Missing Skills\n"
                        + "## Suggested Learning Priority\n"
                        + "Use bullet points and keep under 150 words.")
                .userPrompt("Candidate skills:\n" + AiLmDefaults.nz(candidateSkills) + "\n\nRequired job skills:\n"
                        + AiLmDefaults.nz(requiredJobSkills))
                .temperature(0.1d)
                .maxTokens(512)
                .model(AiLmDefaults.configuredModel(config))
                .build();
    }
}
