package com.bupt.ta.service;

import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.testsupport.FileTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FavoriteServiceTest {

    private FavoriteService svc;

    @BeforeEach
    void setUp() throws Exception {
        ServiceFactory factory = FileTestSupport.newFactory();
        svc = factory.getFavoriteService();
    }

    private void seedUserAndJob(String userId, String jobId) throws Exception {
        FileTestSupport.seedUser(userId, userId + "@test.local");
        FileTestSupport.seedJob(jobId);
    }

    @Test
    void toggleFavorite_addThenRemove() throws Exception {
        seedUserAndJob("u1", "job-1");
        assertTrue(svc.toggleFavorite("u1", "job-1"));
        assertTrue(svc.isFavorite("u1", "job-1"));
        assertFalse(svc.toggleFavorite("u1", "job-1"));
        assertFalse(svc.isFavorite("u1", "job-1"));
    }

    @Test
    void favoritesAreScopedPerUser() throws Exception {
        seedUserAndJob("u1", "job-1");
        FileTestSupport.seedUser("u2", "u2@test.local");
        svc.toggleFavorite("u1", "job-1");
        assertTrue(svc.isFavorite("u1", "job-1"));
        assertFalse(svc.isFavorite("u2", "job-1"));
    }

    @Test
    void getFavoriteJobIds_returnsMultipleInOrder() throws Exception {
        seedUserAndJob("u1", "j1");
        FileTestSupport.seedJob("j2");
        svc.toggleFavorite("u1", "j1");
        svc.toggleFavorite("u1", "j2");
        Set<String> ids = svc.getFavoriteJobIds("u1");
        assertEquals(2, ids.size());
        assertTrue(ids.contains("j1"));
        assertTrue(ids.contains("j2"));
    }

    @Test
    void getFavoriteJobIds_nullUserId_returnsEmpty() throws Exception {
        assertTrue(svc.getFavoriteJobIds(null).isEmpty());
    }

    @Test
    void isFavorite_nullInputs_false() throws Exception {
        assertFalse(svc.isFavorite(null, "j1"));
        assertFalse(svc.isFavorite("u1", null));
    }

    @Test
    void toggleFavorite_threeToggles() throws Exception {
        seedUserAndJob("u1", "j1");
        assertTrue(svc.toggleFavorite("u1", "j1"));
        assertFalse(svc.toggleFavorite("u1", "j1"));
        assertTrue(svc.toggleFavorite("u1", "j1"));
    }
}
