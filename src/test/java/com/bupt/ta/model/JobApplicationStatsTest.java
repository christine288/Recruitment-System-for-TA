package com.bupt.ta.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests application counts and job matching used on the job detail page. */
class JobApplicationStatsTest {

    @Test
    void forJob_countsByStatusForMatchingModule() {
        Application pending = app("1", "CS101", "CS101", "Pending");
        Application accepted = app("2", "CS101", "CS101", "Accepted");
        Application other = app("3", "MATH201", "MATH201", "Pending");

        JobApplicationStats stats = JobApplicationStats.forJob(
                Arrays.asList(pending, accepted, other), "CS101", "CS101");

        assertEquals(1, stats.pending);
        assertEquals(1, stats.accepted);
        assertEquals(2, stats.activeTotal());
    }

    @Test
    void forJob_countsRejectedAndWithdrawn() {
        Application rejected = app("1", "CS101", "CS101", "Rejected");
        Application withdrawn = app("2", "CS101", "CS101", "Withdrawn");
        Application pending = app("3", "CS101", "CS101", "Pending");

        JobApplicationStats stats = JobApplicationStats.forJob(
                Arrays.asList(rejected, withdrawn, pending), "CS101", "CS101");

        assertEquals(1, stats.rejected);
        assertEquals(1, stats.withdrawn);
        assertEquals(1, stats.pending);
        assertEquals(2, stats.activeTotal());
    }

    @Test
    void forJob_nullList_returnsZeros() {
        JobApplicationStats stats = JobApplicationStats.forJob(null, "CS101", "CS101");
        assertEquals(0, stats.pending);
        assertEquals(0, stats.accepted);
        assertEquals(0, stats.activeTotal());
    }

    @Test
    void forJob_caseInsensitiveStatus() {
        Application accepted = app("1", "CS101", "CS101", "ACCEPTED");
        JobApplicationStats stats = JobApplicationStats.forJob(
                Collections.singletonList(accepted), "CS101", "CS101");
        assertEquals(1, stats.accepted);
    }

    @Test
    void forJob_nullStatus_countsPending() {
        Application a = app("1", "CS101", "CS101", null);
        JobApplicationStats stats = JobApplicationStats.forJob(
                Collections.singletonList(a), "CS101", "CS101");
        assertEquals(1, stats.pending);
    }

    @Test
    void activeTotal_sumsPendingAcceptedRejected() {
        Application pending = app("1", "CS101", "CS101", "Pending");
        Application accepted = app("2", "CS101", "CS101", "Accepted");
        Application rejected = app("3", "CS101", "CS101", "Rejected");
        JobApplicationStats stats = JobApplicationStats.forJob(
                Arrays.asList(pending, accepted, rejected), "CS101", "CS101");
        assertEquals(3, stats.activeTotal());
    }

    @Test
    void matchesJob_requiresModuleNameAndCode() {
        Application a = app("1", "CS101", "CS101", "Pending");
        assertTrue(JobApplicationStats.matchesJob(a, "CS101", "CS101"));
        assertFalse(JobApplicationStats.matchesJob(a, "CS101", "CS102"));
        assertFalse(JobApplicationStats.matchesJob(a, "MATH201", "MATH201"));
    }

    @Test
    void matchesJob_nullApp_false() {
        assertFalse(JobApplicationStats.matchesJob(null, "CS101", "CS101"));
    }

    @Test
    void matchesJob_trimsWhitespace() {
        Application a = app("1", " CS101 ", " CS101 ", "Pending");
        assertTrue(JobApplicationStats.matchesJob(a, "CS101", "CS101"));
    }

    @Test
    void matchesJob_moduleCodeNull_treatedAsEmpty() {
        Application a = app("1", "CS101", "", "Pending");
        assertTrue(JobApplicationStats.matchesJob(a, "CS101", null));
    }

    @Test
    void parseCapacity_defaultsAndParsesNumericString() {
        assertEquals(2, JobApplicationStats.parseCapacity(null));
        assertEquals(2, JobApplicationStats.parseCapacity(""));
        assertEquals(3, JobApplicationStats.parseCapacity("3"));
        assertEquals(2, JobApplicationStats.parseCapacity("2 TAs"));
    }

    @Test
    void parseCapacity_invalidValuesDefaultToTwo() {
        assertEquals(2, JobApplicationStats.parseCapacity("0"));
        assertEquals(2, JobApplicationStats.parseCapacity("abc"));
    }

    private static Application app(String id, String name, String code, String status) {
        Application a = new Application(id, "u1", name, code, "Lab Assistant", "2026-05-01");
        a.status = status;
        return a;
    }
}
