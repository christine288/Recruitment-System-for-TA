package com.bupt.ta.service.ai;

import com.bupt.ta.ai.AiFeatureNames;
import com.bupt.ta.ai.LmClient;
import com.bupt.ta.ai.LmConfig;
import com.bupt.ta.ai.LmException;
import com.bupt.ta.ai.LmRequest;
import com.bupt.ta.ai.LmResponse;
import com.bupt.ta.service.admin.AdminDashboardMetrics;
import com.bupt.ta.service.admin.AdminMetricsBuilder;

/**
 * Generates admin-facing platform analytics narrative from dashboard metrics.
 *
 * <p>Implements the {@link AiFeatureNames#ADMIN_ANALYTICS} feature. Converts an
 * {@link AdminDashboardMetrics} snapshot into a factual user prompt via
 * {@link AdminMetricsBuilder#buildAnalyticsPrompt} and requests executive-style Markdown
 * (trends, pipeline health, workload alerts, recommended actions).
 *
 * <p>Streaming entry point: {@link com.bupt.ta.servlet.AiStreamServlet} with
 * {@code feature=adminAnalytics} (Admin role only).
 *
 * @see AdminMetricsBuilder
 * @see com.bupt.ta.servlet.AdminServlet
 */
public final class AdminAnalyticsService {
    private final LmClient client;
    private final LmConfig config;

    /**
     * Creates the service with LM client and configuration.
     *
     * @param client LM client (mock or HTTP)
     * @param config provider settings and model name
     */
    public AdminAnalyticsService(LmClient client, LmConfig config) {
        this.client = client;
        this.config = config;
    }

    /**
     * Synchronous analytics generation (tests and non-stream callers).
     *
     * @param metrics dashboard snapshot
     * @return LM response with Markdown briefing
     * @throws LmException if the client rejects or fails the request
     */
    public LmResponse analyze(AdminDashboardMetrics metrics) throws LmException {
        return client.generate(buildRequest(metrics));
    }

    /**
     * Builds the {@link LmRequest} without invoking the client (shared by stream and batch paths).
     *
     * @param metrics dashboard snapshot from {@link AdminMetricsBuilder#build}
     * @return configured request with system/user prompts
     */
    public LmRequest buildRequest(AdminDashboardMetrics metrics) {
        return LmRequest.builder()
                .featureName(AiFeatureNames.ADMIN_ANALYTICS)
                .systemPrompt("You are an analytics assistant for a university TA recruitment platform admin.\n"
                        + "Use ONLY the metrics provided — do not invent numbers.\n"
                        + "Return concise Markdown with sections:\n"
                        + "## Executive Summary\n"
                        + "## User & Applicant Trends\n"
                        + "## Application Pipeline Health\n"
                        + "## Workload & Capacity Alerts\n"
                        + "## Recommended Actions\n"
                        + "Highlight month-over-month changes when visible in the data.")
                .userPrompt(AdminMetricsBuilder.buildAnalyticsPrompt(metrics))
                .temperature(0.3d)
                .maxTokens(650)
                .model(AiLmDefaults.configuredModel(config))
                .build();
    }
}
