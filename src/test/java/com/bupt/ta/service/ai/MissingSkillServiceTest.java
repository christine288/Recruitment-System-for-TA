package com.bupt.ta.service.ai;

import com.bupt.ta.ai.LmConfig;
import com.bupt.ta.ai.LmException;
import com.bupt.ta.ai.LmResponse;
import com.bupt.ta.ai.MockLmClient;
import com.bupt.ta.testsupport.LmTestSupport;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MissingSkillServiceTest {

    @Test
    void identifyMissingSkills_invokesMockClient() throws LmException {
        ServletContext ctx = LmTestSupport.servletContextWithLmProperties("LM_PROVIDER=mock\n");
        LmConfig cfg = LmConfig.load(ctx);
        MissingSkillService svc = new MissingSkillService(new MockLmClient(), cfg);
        LmResponse r = svc.identifyMissingSkills("A", "A, B, C");
        assertTrue(r.isSuccess());
        assertTrue(r.getText().toLowerCase().contains("missing"));
    }
}
