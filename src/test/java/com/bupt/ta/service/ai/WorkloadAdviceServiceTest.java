package com.bupt.ta.service.ai;

import com.bupt.ta.ai.AiFeatureNames;
import com.bupt.ta.ai.LmConfig;
import com.bupt.ta.ai.MockLmClient;
import com.bupt.ta.model.MoWorkloadSnapshot;
import com.bupt.ta.model.TaWorkloadStats;
import com.bupt.ta.testsupport.LmTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkloadAdviceServiceTest {

    @Test
    void buildAdviceRequest_usesWorkloadAdviceFeature() {
        LmConfig cfg = LmConfig.load(LmTestSupport.servletContextWithLmProperties("LM_PROVIDER=mock\n"));
        WorkloadAdviceService svc = new WorkloadAdviceService(new MockLmClient(), cfg);
        MoWorkloadSnapshot snap = snapshot(4, 1, 4);

        assertTrue(svc.buildAdviceRequest(snap).getFeatureName().equals(AiFeatureNames.WORKLOAD_ADVICE));
    }

    @Test
    void adviseMoOnWorkload_highLoadSuggestsRejectOrCaution() throws Exception {
        LmConfig cfg = LmConfig.load(LmTestSupport.servletContextWithLmProperties("LM_PROVIDER=mock\n"));
        WorkloadAdviceService svc = new WorkloadAdviceService(new MockLmClient(), cfg);

        String text = svc.adviseMoOnWorkload(snapshot(1, 3, 4)).getText().toLowerCase();

        assertTrue(text.contains("reject") || text.contains("caution"));
        assertTrue(text.contains("potential load"));
    }

    @Test
    void adviseMoOnWorkload_lowLoadSuggestsApprove() throws Exception {
        LmConfig cfg = LmConfig.load(LmTestSupport.servletContextWithLmProperties("LM_PROVIDER=mock\n"));
        WorkloadAdviceService svc = new WorkloadAdviceService(new MockLmClient(), cfg);

        String text = svc.adviseMoOnWorkload(snapshot(1, 1, 2)).getText().toLowerCase();

        assertTrue(text.contains("approve"));
    }

    private static MoWorkloadSnapshot snapshot(int accepted, int pending, int potential) {
        MoWorkloadSnapshot s = new MoWorkloadSnapshot();
        s.applicantName = "Test TA";
        s.targetModuleName = "CS101";
        s.targetModuleCode = "CS101";
        s.targetRole = "Lab Assistant";
        s.targetWorkloadHours = "4h/week";
        s.acceptedCount = accepted;
        s.pendingCount = pending;
        s.potentialLoadIfApprove = potential;
        s.warningThreshold = TaWorkloadStats.ASSIGNED_JOBS_WARNING_THRESHOLD;
        s.acceptedPositions.add("CS50 [CS50] - Lab | 2026-03-01");
        s.pendingPositions.add("MATH201 [MATH201] - Tutorial | 2026-03-02");
        s.acceptedHoursHints.add("4h/week");
        s.pendingHoursHints.add("unknown");
        return s;
    }
}
