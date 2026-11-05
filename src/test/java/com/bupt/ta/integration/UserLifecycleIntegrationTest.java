package com.bupt.ta.integration;

import com.bupt.ta.model.Job;
import com.bupt.ta.model.User;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.testsupport.FileTestSupport;
import com.bupt.ta.testsupport.TestFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserLifecycleIntegrationTest {

    @Test
    void registerLoginApplyAndDeleteUser() throws Exception {
        ServiceFactory factory = FileTestSupport.newFactory();
        var auth = factory.getAuthService();
        var profiles = factory.getProfileService();
        var apps = factory.getApplicationService();
        var jobs = factory.getJobService();

        Job j = TestFixtures.sampleJob("j1", "CS101", "CS101");
        jobs.createJob(j);

        User u = auth.register("Integration User", TestFixtures.validBuptStudentId(),
                "integration@bupt.edu.cn", "pass123");
        profiles.upsert(TestFixtures.completeProfile(u.id));
        assertTrue(auth.login("integration@bupt.edu.cn", "pass123").isPresent());

        apps.save(TestFixtures.sampleApplication("app-1", u.id, "CS101", "CS101"));
        assertEquals(1, apps.getByUserId(u.id).size());
        assertTrue(jobs.getJobById("j1") != null);

        auth.setUserActive(u.id, false);
        assertFalse(auth.login("integration@bupt.edu.cn", "pass123").isPresent());
        auth.setUserActive(u.id, true);
        assertTrue(auth.login("integration@bupt.edu.cn", "pass123").isPresent());

        apps.deleteByUserId(u.id);
        profiles.deleteByUserId(u.id);
        auth.removeUserRecord(u.id);
        assertFalse(auth.findById(u.id).isPresent());
        assertTrue(apps.getByUserId(u.id).isEmpty());
        assertFalse(profiles.getByUserId(u.id).isPresent());
    }
}
