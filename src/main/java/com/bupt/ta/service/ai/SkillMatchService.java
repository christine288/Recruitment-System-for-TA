package com.bupt.ta.service.ai;

import com.bupt.ta.ai.AiFeatureNames;
import com.bupt.ta.ai.LmClient;
import com.bupt.ta.ai.LmConfig;
import com.bupt.ta.ai.LmException;
import com.bupt.ta.ai.LmRequest;
import com.bupt.ta.ai.LmResponse;

/** Builds prompts for skill matching; delegates generation to {@link LmClient}. */
public final class SkillMatchService {
    private final LmClient client;
    private final LmConfig config;

    public SkillMatchService(LmClient client, LmConfig config) {
        this.client = client;
        this.config = config;
    }

    public LmResponse matchApplicantSkills(String applicantSkills, String jobRequirements) throws LmException {
        return client.generate(buildMatchRequest(applicantSkills, jobRequirements));
    }

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
