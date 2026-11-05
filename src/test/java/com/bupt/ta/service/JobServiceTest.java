package com.bupt.ta.service;

import com.bupt.ta.model.Job;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.testsupport.FileTestSupport;
import com.bupt.ta.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobServiceTest {

    private JobService svc;

    @BeforeEach
    void setUp() throws Exception {
        ServiceFactory factory = FileTestSupport.newFactory();
        svc = factory.getJobService();
    }

    @Test
    void getAllJobs_parsesSeedJson() throws Exception {
        Job j = TestFixtures.sampleJob("j1", "Alpha", "A101");
        svc.createJob(j);
        List<Job> jobs = svc.getAllJobs();
        assertEquals(1, jobs.size());
        assertEquals("Alpha", jobs.get(0).getModuleName());
        assertEquals("2", jobs.get(0).getNumberOfTAs());
    }

    @Test
    void getAllJobs_emptyFile_returnsEmpty() {
        assertTrue(svc.getAllJobs().isEmpty());
    }

    @Test
    void getJobById_foundAndMissing() throws Exception {
        Job j = TestFixtures.sampleJob("j1", "Alpha", "A101");
        svc.createJob(j);
        assertNotNull(svc.getJobById("j1"));
        assertNull(svc.getJobById("missing"));
        assertNull(svc.getJobById(null));
    }

    @Test
    void listPublishedJobs_excludesDraft() throws Exception {
        Job pub = TestFixtures.sampleJob("j1", "Pub", "P1");
        pub.setStatus("Published");
        svc.createJob(pub);
        Job draft = TestFixtures.sampleJob("j2", "Draft", "D1");
        draft.setStatus("Draft");
        svc.createJob(draft);
        List<Job> published = svc.listPublishedJobs();
        assertEquals(1, published.size());
        assertEquals("Pub", published.get(0).getModuleName());
    }

    @Test
    void createJob_assignsIdAndDraftStatus() throws Exception {
        Job j = new Job();
        j.setModuleName("New");
        j.setModuleCode("N1");
        j.setDescription("Desc");
        Job created = svc.createJob(j);
        assertNotNull(created.getId());
        assertEquals("Draft", created.getStatus());
    }

    @Test
    void createJob_publishedSetsDates() throws Exception {
        Job j = new Job();
        j.setModuleName("Pub");
        j.setModuleCode("P1");
        j.setDescription("Desc");
        j.setStatus("Published");
        Job created = svc.createJob(j);
        assertFalse(created.getPostDate().isEmpty());
        assertFalse(created.getPublishedAt().isEmpty());
    }

    @Test
    void publishJob_changesStatusAndMoId() throws Exception {
        Job j = TestFixtures.sampleJob("j1", "M", "C");
        j.setStatus("Draft");
        j.setCreatedByMoId("");
        svc.createJob(j);
        assertTrue(svc.publishJob("j1", "mo-1"));
        Job updated = svc.getJobById("j1");
        assertEquals("Published", updated.getStatus());
        assertEquals("mo-1", updated.getCreatedByMoId());
    }

    @Test
    void publishJob_unknownId_returnsFalse() throws Exception {
        assertFalse(svc.publishJob("missing", "mo"));
    }

    @Test
    void deleteJobById_removesFromFile() throws Exception {
        Job j = TestFixtures.sampleJob("j1", "M", "C");
        svc.createJob(j);
        svc.deleteJobById("j1");
        assertNull(svc.getJobById("j1"));
    }

    @Test
    void getAllJobs_defaultScheduleForLab() throws Exception {
        Job j = new Job();
        j.setId("j1");
        j.setModuleName("Lab");
        j.setModuleCode("L1");
        j.setActivityType("Lab Assistant");
        j.setDescription("d");
        j.setPostDate("2026-01-01");
        j.setApplicationDeadline("2026-12-31");
        j.setDuration("One semester");
        j.setNumberOfTAs("1");
        j.setStatus("Published");
        j.setSchedule(List.of());
        svc.createJob(j);
        List<String> schedule = svc.getAllJobs().get(0).getSchedule();
        assertFalse(schedule.isEmpty());
        assertTrue(schedule.get(0).contains("Monday"));
    }

    @Test
    void getAllJobs_numberOfTAs_defaultsToTwo() throws Exception {
        Job j = new Job();
        j.setId("j1");
        j.setModuleName("M");
        j.setModuleCode("C");
        j.setActivityType("Lab");
        j.setDescription("d");
        j.setPostDate("2026-01-01");
        j.setStatus("Published");
        svc.createJob(j);
        assertEquals("2", svc.getAllJobs().get(0).getNumberOfTAs());
    }
}
