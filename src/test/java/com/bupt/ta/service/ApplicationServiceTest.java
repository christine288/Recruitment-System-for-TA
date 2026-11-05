package com.bupt.ta.service;

import com.bupt.ta.model.Application;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.testsupport.FileTestSupport;
import com.bupt.ta.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationServiceTest {

    private ApplicationService svc;

    @BeforeEach
    void setUp() throws Exception {
        ServiceFactory factory = FileTestSupport.newFactory();
        svc = factory.getApplicationService();
    }

    private void seedUser(String userId) throws Exception {
        FileTestSupport.seedUser(userId, userId + "@test.local");
    }

    @Test
    void saveAndGetByUserId_returnsSubmittedApplication() throws Exception {
        seedUser("user-1");
        Application app = TestFixtures.sampleApplication("app-1", "user-1", "CS101", "CS101");
        svc.save(app);
        List<Application> mine = svc.getByUserId("user-1");
        assertEquals(1, mine.size());
        assertEquals("CS101", mine.get(0).moduleName);
        assertEquals("Pending", mine.get(0).status);
    }

    @Test
    void updateStatus_changesStoredStatusAndFeedback() throws Exception {
        seedUser("user-2");
        svc.save(TestFixtures.sampleApplication("app-2", "user-2", "MATH201", "MATH201"));
        svc.updateStatus("app-2", "Accepted", "Welcome aboard");
        Optional<Application> found = svc.findById("app-2");
        assertTrue(found.isPresent());
        assertEquals("Accepted", found.get().status);
        assertEquals("Welcome aboard", found.get().feedback);
    }

    @Test
    void getByUserId_doesNotReturnOtherUsersApplications() throws Exception {
        seedUser("user-a");
        seedUser("user-b");
        svc.save(TestFixtures.sampleApplication("a1", "user-a", "ENG101", "ENG101"));
        svc.save(TestFixtures.sampleApplication("a2", "user-b", "PHY150", "PHY150"));
        assertEquals(1, svc.getByUserId("user-a").size());
    }

    @Test
    void listAll_returnsAllApplications() throws Exception {
        seedUser("u1");
        seedUser("u2");
        svc.save(TestFixtures.sampleApplication("a1", "u1", "A", "A"));
        svc.save(TestFixtures.sampleApplication("a2", "u2", "B", "B"));
        assertEquals(2, svc.listAll().size());
    }

    @Test
    void findById_nullOrEmpty_returnsEmpty() throws Exception {
        assertFalse(svc.findById(null).isPresent());
        assertFalse(svc.findById("").isPresent());
    }

    @Test
    void save_upsertsSameId() throws Exception {
        seedUser("u1");
        Application a = TestFixtures.sampleApplication("same", "u1", "CS101", "CS101");
        svc.save(a);
        a.status = "Accepted";
        svc.save(a);
        assertEquals("Accepted", svc.findById("same").get().status);
    }

    @Test
    void updateStatus_unknownId_noOp() throws Exception {
        svc.updateStatus("missing", "Accepted", "x");
        assertEquals(0, svc.listAll().size());
    }

    @Test
    void deleteByUserId_removesAllForUser() throws Exception {
        seedUser("u1");
        svc.save(TestFixtures.sampleApplication("a1", "u1", "A", "A"));
        svc.save(TestFixtures.sampleApplication("a2", "u1", "B", "B"));
        svc.deleteByUserId("u1");
        assertEquals(0, svc.getByUserId("u1").size());
    }

    @Test
    void getByUserId_nullUserId_returnsEmpty() throws Exception {
        assertTrue(svc.getByUserId(null).isEmpty());
    }
}
