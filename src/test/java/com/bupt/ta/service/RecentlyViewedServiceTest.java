package com.bupt.ta.service;

import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.testsupport.FileTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecentlyViewedServiceTest {

    private RecentlyViewedService svc;

    @BeforeEach
    void setUp() throws Exception {
        ServiceFactory factory = FileTestSupport.newFactory();
        svc = factory.getRecentlyViewedService();
    }

    private void seedUserAndJob(String userId, String jobId) throws Exception {
        FileTestSupport.seedUser(userId, userId + "@test.local");
        FileTestSupport.seedJob(jobId);
    }

    @Test
    void recordView_keepsAtMostThreeMostRecent() throws Exception {
        seedUserAndJob("u1", "a");
        FileTestSupport.seedJob("b");
        FileTestSupport.seedJob("c");
        FileTestSupport.seedJob("d");
        svc.recordView("u1", "a");
        svc.recordView("u1", "b");
        svc.recordView("u1", "c");
        svc.recordView("u1", "d");
        List<String> recent = svc.getRecentJobIds("u1");
        assertEquals(3, recent.size());
        assertEquals("d", recent.get(0));
    }

    @Test
    void recordView_revisitMovesJobToFront() throws Exception {
        seedUserAndJob("u1", "a");
        FileTestSupport.seedJob("b");
        svc.recordView("u1", "a");
        svc.recordView("u1", "b");
        svc.recordView("u1", "a");
        assertEquals("a", svc.getRecentJobIds("u1").get(0));
    }

    @Test
    void getRecentJobIds_nullUserId_returnsEmpty() throws Exception {
        assertTrue(svc.getRecentJobIds(null).isEmpty());
    }

    @Test
    void recordView_nullJobId_noOp() throws Exception {
        FileTestSupport.seedUser("u1", "u1@test.local");
        svc.recordView("u1", null);
        assertTrue(svc.getRecentJobIds("u1").isEmpty());
    }

    @Test
    void viewsAreScopedPerUser() throws Exception {
        seedUserAndJob("u1", "j1");
        seedUserAndJob("u2", "j2");
        svc.recordView("u1", "j1");
        svc.recordView("u2", "j2");
        assertEquals("j1", svc.getRecentJobIds("u1").get(0));
        assertEquals("j2", svc.getRecentJobIds("u2").get(0));
    }
}
