package com.bupt.ta.service.ai;

import com.bupt.ta.ai.AiFeatureNames;
import com.bupt.ta.ai.LmConfig;
import com.bupt.ta.ai.MockLmClient;
import com.bupt.ta.model.Application;
import com.bupt.ta.model.ApplicantProfile;
import com.bupt.ta.model.Job;
import com.bupt.ta.model.MoProcessedReviewContext;
import com.bupt.ta.testsupport.LmTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessedDecisionReviewServiceTest {

    @Test
    void buildReviewRequest_usesDecisionReviewFeature() {
        LmConfig cfg = LmConfig.load(LmTestSupport.servletContextWithLmProperties("LM_PROVIDER=mock\n"));
        ProcessedDecisionReviewService svc = new ProcessedDecisionReviewService(new MockLmClient(), cfg);
        MoProcessedReviewContext ctx = new MoProcessedReviewContext();
        ctx.decisionStatus = "Accepted";
        ctx.applicantName = "Alice";
        ctx.applicantCapabilities = "Python, Java";
        assertTrue(svc.buildReviewRequest(ctx).getFeatureName().equals(AiFeatureNames.DECISION_REVIEW));
    }

    @Test
    void buildContext_mapsApplicationFields() {
        Application app = new Application();
        app.id = "a1";
        app.moduleName = "CS101";
        app.moduleCode = "CS101";
        app.role = "Lab TA";
        app.status = "Accepted";
        app.applicationDate = "2026-05-01";

        Job job = new Job();
        job.setActivityType("Lab");
        job.setWorkloadHours("4h/week");

        ApplicantProfile profile = new ApplicantProfile();
        profile.skills = "Python";

        MoProcessedReviewContext ctx = ProcessedDecisionReviewService.buildContext(
                app, job, profile, "Alice Chen");

        assertEquals("a1", ctx.applicationId);
        assertEquals("Alice Chen", ctx.applicantName);
        assertEquals("Accepted", ctx.decisionStatus);
        assertEquals("Lab", ctx.jobActivityType);
    }
}
