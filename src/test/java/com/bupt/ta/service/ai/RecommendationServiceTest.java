package com.bupt.ta.service.ai;

import com.bupt.ta.ai.LmConfig;
import com.bupt.ta.ai.LmException;
import com.bupt.ta.ai.LmResponse;
import com.bupt.ta.ai.MockLmClient;
import com.bupt.ta.testsupport.LmTestSupport;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationServiceTest {

    @Test
    void recommendJobs_invokesMockClient() throws LmException {
        ServletContext ctx = LmTestSupport.servletContextWithLmProperties("LM_PROVIDER=mock\n");
        LmConfig cfg = LmConfig.load(ctx);
        RecommendationService svc = new RecommendationService(new MockLmClient(), cfg);
        LmResponse r = svc.recommendJobs("Student profile", "Job one line\nJob two line");
        assertTrue(r.isSuccess());
        assertTrue(r.getText().contains("Top Recommendations"));
    }
}
