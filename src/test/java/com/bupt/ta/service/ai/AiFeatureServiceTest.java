package com.bupt.ta.service.ai;

import com.bupt.ta.ai.LmClient;
import com.bupt.ta.ai.LmClientFactory;
import com.bupt.ta.ai.LmConfig;
import com.bupt.ta.testsupport.LmTestSupport;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end workflow: {@link LmConfig} → {@link LmClientFactory} → {@link AiFeatureService} (mock provider).
 */
class AiFeatureServiceTest {

    @Test
    void matchApplicantSkills_mockPipeline() {
        ServletContext ctx = LmTestSupport.servletContextWithLmProperties("LM_PROVIDER=mock\n");
        LmConfig cfg = LmConfig.load(ctx);
        LmClient client = LmClientFactory.create(cfg);
        AiFeatureService svc = new AiFeatureService(cfg, client);

        AiFeatureOutput out = svc.matchApplicantSkills("Java, SQL", "Java, Docker");
        assertTrue(out.isSuccess());
        assertTrue(out.getText().contains("Match score"));
        assertTrue(out.getProvider().contains("mock"));
    }

    @Test
    void identifyMissingSkills_mockPipeline() {
        LmConfig cfg = LmConfig.load(LmTestSupport.servletContextWithLmProperties("LM_PROVIDER=mock\n"));
        AiFeatureService svc = new AiFeatureService(cfg, LmClientFactory.create(cfg));
        AiFeatureOutput out = svc.identifyMissingSkills("Java", "Java, Kubernetes");
        assertTrue(out.isSuccess());
        assertTrue(out.getText().toLowerCase().contains("missing"));
    }

    @Test
    void recommendJobs_mockPipeline() {
        LmConfig cfg = LmConfig.load(LmTestSupport.servletContextWithLmProperties("LM_PROVIDER=mock\n"));
        AiFeatureService svc = new AiFeatureService(cfg, LmClientFactory.create(cfg));
        AiFeatureOutput out = svc.recommendJobs("Java student", "CS101 TA\nCS202 TA");
        assertTrue(out.isSuccess());
        assertTrue(out.getText().contains("CS101") || out.getText().contains("CS202"));
    }

    @Test
    void adviseMoOnWorkload_mockPipeline() {
        LmConfig cfg = LmConfig.load(LmTestSupport.servletContextWithLmProperties("LM_PROVIDER=mock\n"));
        AiFeatureService svc = new AiFeatureService(cfg, LmClientFactory.create(cfg));
        com.bupt.ta.model.MoWorkloadSnapshot snap = new com.bupt.ta.model.MoWorkloadSnapshot();
        snap.acceptedCount = 2;
        snap.pendingCount = 2;
        snap.potentialLoadIfApprove = 4;
        snap.warningThreshold = 3;
        snap.applicantName = "Alice";
        AiFeatureOutput out = svc.adviseMoOnWorkload(snap);
        assertTrue(out.isSuccess());
        assertTrue(out.getText().toLowerCase().contains("workload")
                || out.getText().toLowerCase().contains("recommendation"));
    }

    @Test
    void whenDisabled_returnsErrorOutput() {
        ServletContext ctx = LmTestSupport.servletContextWithLmProperties("LM_ENABLED=false\n");
        LmConfig cfg = LmConfig.load(ctx);
        AiFeatureService svc = new AiFeatureService(cfg, LmClientFactory.create(cfg));
        AiFeatureOutput out = svc.matchApplicantSkills("a", "b");
        assertFalse(out.isSuccess());
        assertTrue(out.getErrorMessage().contains("disabled"));
    }
}
