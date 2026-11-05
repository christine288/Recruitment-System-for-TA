package com.bupt.ta.service.ai;

import com.bupt.ta.ai.AiFeatureNames;
import com.bupt.ta.ai.LmClient;
import com.bupt.ta.ai.LmConfig;
import com.bupt.ta.ai.LmException;
import com.bupt.ta.ai.LmRequest;
import com.bupt.ta.ai.LmResponse;

/** Identifies skills required by a job that the candidate does not list (mock or LM). */
public final class MissingSkillService {
    private final LmClient client;
    private final LmConfig config;

    public MissingSkillService(LmClient client, LmConfig config) {
        this.client = client;
        this.config = config;
    }

    public LmResponse identifyMissingSkills(String candidateSkills, String requiredJobSkills) throws LmException {
        return client.generate(buildMissingRequest(candidateSkills, requiredJobSkills));
    }

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
