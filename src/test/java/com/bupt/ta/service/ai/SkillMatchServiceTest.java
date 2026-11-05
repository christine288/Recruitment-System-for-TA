package com.bupt.ta.service.ai;

import com.bupt.ta.ai.LmConfig;
import com.bupt.ta.ai.LmException;
import com.bupt.ta.ai.LmResponse;
import com.bupt.ta.ai.MockLmClient;
import com.bupt.ta.testsupport.LmTestSupport;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillMatchServiceTest {

    @Test
    void matchApplicantSkills_invokesMockClient() throws LmException {
        ServletContext ctx = LmTestSupport.servletContextWithLmProperties("LM_PROVIDER=mock\n");
        LmConfig cfg = LmConfig.load(ctx);
        SkillMatchService svc = new SkillMatchService(new MockLmClient(), cfg);
        LmResponse r = svc.matchApplicantSkills("Python", "Python, Teaching");
        assertTrue(r.isSuccess());
        assertTrue(r.getText().contains("Match score"));
    }
<<<<<<< Updated upstream
=======

    @Test
    void matchApplicantSkills_emptyInputsStillSucceed() throws LmException {
        SkillMatchService svc = new SkillMatchService(new MockLmClient(), LmConfig.load(
                LmTestSupport.servletContextWithLmProperties("LM_PROVIDER=mock\n")));
        LmResponse r = svc.matchApplicantSkills("", "");
        assertTrue(r.isSuccess());
    }
>>>>>>> Stashed changes
}
