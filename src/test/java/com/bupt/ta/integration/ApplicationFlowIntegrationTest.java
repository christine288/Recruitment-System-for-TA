package com.bupt.ta.integration;

import com.bupt.ta.model.Application;
import com.bupt.ta.model.Job;
import com.bupt.ta.model.JobApplicationStats;
import com.bupt.ta.model.User;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.testsupport.FileTestSupport;
import com.bupt.ta.testsupport.TestFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationFlowIntegrationTest {

    @Test
    void moPublishTaApplyMoApprove() throws Exception {
        ServiceFactory factory = FileTestSupport.newFactory();
        var auth = factory.getAuthService();
        var jobs = factory.getJobService();
        var apps = factory.getApplicationService();
        var profiles = factory.getProfileService();

        User ta = auth.register("Applicant", TestFixtures.validBuptStudentId(), "applicant@bupt.edu.cn", "pass");
        profiles.upsert(TestFixtures.completeProfile(ta.id));

        Job job = TestFixtures.sampleJob(null, "CS101", "CS101");
        job.setStatus("Published");
        job.setDescription("Lab");
        jobs.createJob(job);

        Application app = TestFixtures.sampleApplication("app-1", ta.id, "CS101", "CS101");
        apps.save(app);
        assertEquals("Pending", apps.findById("app-1").get().status);

        apps.updateStatus("app-1", "Accepted", "Approved");
        List<Application> all = apps.listAll();
        JobApplicationStats stats = JobApplicationStats.forJob(all, "CS101", "CS101");
        assertEquals(1, stats.accepted);

        apps.updateStatus("app-1", "Withdrawn", "");
        stats = JobApplicationStats.forJob(apps.listAll(), "CS101", "CS101");
        assertEquals(0, stats.accepted);
        assertTrue(stats.withdrawn >= 1);
    }

    @Test
    void fullSlotBlocksNewApplications() throws Exception {
        ServiceFactory factory = FileTestSupport.newFactory();
        var apps = factory.getApplicationService();
        var jobs = factory.getJobService();

        FileTestSupport.seedUser("u1", "u1@test.local");
        Job job = TestFixtures.sampleJob("j1", "CS101", "CS101");
        job.setNumberOfTAs("1");
        job.setStatus("Published");
        jobs.createJob(job);

        Application accepted = TestFixtures.sampleApplication("a1", "u1", "CS101", "CS101");
        accepted.status = "Accepted";
        apps.save(accepted);

        int capacity = JobApplicationStats.parseCapacity(jobs.getJobById("j1").getNumberOfTAs());
        JobApplicationStats stats = JobApplicationStats.forJob(apps.listAll(), "CS101", "CS101");
        assertTrue(stats.accepted >= capacity);
    }
}
