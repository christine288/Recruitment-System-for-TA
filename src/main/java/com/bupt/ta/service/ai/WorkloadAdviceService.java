package com.bupt.ta.service.ai;

import com.bupt.ta.ai.AiFeatureNames;
import com.bupt.ta.ai.LmClient;
import com.bupt.ta.ai.LmConfig;
import com.bupt.ta.ai.LmException;
import com.bupt.ta.ai.LmRequest;
import com.bupt.ta.ai.LmResponse;
import com.bupt.ta.model.MoWorkloadSnapshot;

/**
 * Generates module-organiser (MO) workload approval advice via the language model.
 * <p>
 * Implements the {@link AiFeatureNames#WORKLOAD_ADVICE} feature. Converts a
 * {@link MoWorkloadSnapshot} into a factual user prompt (counts and position lists only)
 * and requests a practical Approve/Reject/Caution recommendation in Markdown form.
 */
public final class WorkloadAdviceService {
    private final LmClient client;
    private final LmConfig config;

    /**
     * Creates a service bound to the given LM client and configuration.
     *
     * @param client LM client for synchronous generation
     * @param config runtime settings (model, provider)
     */
    public WorkloadAdviceService(LmClient client, LmConfig config) {
        this.client = client;
        this.config = config;
    }

    /**
     * Calls the language model to advise on approving a pending TA application.
     *
     * @param snapshot precomputed workload context for the applicant and target role
     * @return raw LM response with recommendation Markdown
     * @throws LmException if the client rejects or fails the request
     */
    public LmResponse adviseMoOnWorkload(MoWorkloadSnapshot snapshot) throws LmException {
        return client.generate(buildAdviceRequest(snapshot));
    }

    /**
     * Builds the {@link LmRequest} for workload advice without invoking the client.
     * <p>
     * The user prompt is derived from {@link #buildUserPrompt(MoWorkloadSnapshot)} so
     * streaming and batch paths share identical input formatting.
     *
     * @param snapshot workload data for one pending application
     * @return configured request with system/user prompts and token limits
     */
    public LmRequest buildAdviceRequest(MoWorkloadSnapshot snapshot) {
        return LmRequest.builder()
                .featureName(AiFeatureNames.WORKLOAD_ADVICE)
                .systemPrompt("You advise module organisers (MO) on TA hiring decisions based on workload data.\n"
                        + "Use ONLY the counts and lists provided — do not invent numbers.\n"
                        + "Return concise Markdown with sections:\n"
                        + "## Workload Summary\n"
                        + "## Recommendation (Approve / Reject / Caution)\n"
                        + "## Reasoning\n"
                        + "## If Approved — Expected Load\n"
                        + "Be practical and mention schedule overlap risks when load is high.")
                .userPrompt(buildUserPrompt(snapshot))
                .temperature(0.25d)
                .maxTokens(600)
                .model(AiLmDefaults.configuredModel(config))
                .build();
    }

    static String buildUserPrompt(MoWorkloadSnapshot s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Applicant: ").append(AiLmDefaults.nz(s.applicantName)).append("\n");
        sb.append("Target application: ").append(AiLmDefaults.nz(s.targetModuleName));
        if (s.targetModuleCode != null && !s.targetModuleCode.isEmpty()) {
            sb.append(" [").append(s.targetModuleCode).append("]");
        }
        if (s.targetRole != null && !s.targetRole.isEmpty()) {
            sb.append(" — ").append(s.targetRole);
        }
        sb.append("\n");
        sb.append("Target workload hours: ").append(AiLmDefaults.nz(s.targetWorkloadHours)).append("\n\n");

        sb.append("Accepted count: ").append(s.acceptedCount).append("\n");
        sb.append("Pending count: ").append(s.pendingCount).append("\n");
        sb.append("Potential load if approve: ").append(s.potentialLoadIfApprove).append("\n");
        sb.append("Warning threshold: ").append(s.warningThreshold).append("\n\n");

        sb.append("Accepted positions:\n");
        appendPositionLines(sb, s.acceptedPositions, s.acceptedHoursHints);
        sb.append("\nPending positions:\n");
        appendPositionLines(sb, s.pendingPositions, s.pendingHoursHints);
        return sb.toString();
    }

    private static void appendPositionLines(StringBuilder sb, java.util.List<String> lines,
                                            java.util.List<String> hoursHints) {
        if (lines == null || lines.isEmpty()) {
            sb.append("- (none)\n");
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            sb.append("- ").append(lines.get(i));
            if (hoursHints != null && i < hoursHints.size()) {
                sb.append(" | hours: ").append(hoursHints.get(i));
            }
            sb.append("\n");
        }
    }
}
