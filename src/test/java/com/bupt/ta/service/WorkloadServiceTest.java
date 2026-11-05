package com.bupt.ta.service;

import com.bupt.ta.model.Application;
import com.bupt.ta.model.Job;
import com.bupt.ta.model.MoWorkloadSnapshot;
import com.bupt.ta.model.Roles;
import com.bupt.ta.model.TaWorkloadStats;
import com.bupt.ta.model.User;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkloadServiceTest {

    private final WorkloadService service = new WorkloadService();

    @Test
    void buildTaWorkloadStats_countsAcceptedPendingRejected() {
        User ta = user("u1", Roles.TA);
        Application accepted = app("a1", "u1", "Accepted");
        Application pending = app("a2", "u1", "Pending");
        Application rejected = app("a3", "u1", "Rejected");

        Map<String, TaWorkloadStats> stats = service.buildTaWorkloadStats(
                Collections.singletonList(ta),
                Arrays.asList(accepted, pending, rejected));

        TaWorkloadStats row = stats.get("u1");
        assertNotNull(row);
        assertEquals(3, row.total);
        assertEquals(1, row.accepted);
        assertEquals(1, row.pending);
        assertEquals(1, row.rejected);
    }

    @Test
    void buildSnapshotForApplication_pendingIncludesCurrentInPotentialLoad() {
        Application accepted = app("a1", "u1", "Accepted");
        accepted.moduleCode = "CS50";
        Application pending1 = app("a2", "u1", "Pending");
        pending1.moduleCode = "MATH201";
        Application pending2 = app("a3", "u1", "Pending");
        pending2.moduleCode = "DATA301";

        Job cs50 = job("CS50", "4h/week");
        List<Application> apps = Arrays.asList(accepted, pending1, pending2);

        MoWorkloadSnapshot snap = service.buildSnapshotForApplication(
                "a2", apps, Collections.singletonList(cs50), "Alice");

        assertNotNull(snap);
        assertEquals(1, snap.acceptedCount);
        assertEquals(2, snap.pendingCount);
        assertEquals(3, snap.potentialLoadIfApprove);
        assertEquals("Alice", snap.applicantName);
        assertEquals("4h/week", snap.acceptedHoursHints.get(0));
    }

    @Test
    void buildSnapshotForApplication_nonPendingReturnsNull() {
        Application accepted = app("a1", "u1", "Accepted");
        MoWorkloadSnapshot snap = service.buildSnapshotForApplication(
                "a1", Collections.singletonList(accepted), Collections.emptyList(), "Bob");
        assertNull(snap);
    }

    @Test
    void buildSnapshotsForPendingApplications_onlyPendingKeys() {
        Application pending = app("p1", "u1", "Pending");
        Application accepted = app("a1", "u2", "Accepted");
        Map<String, String> names = new HashMap<>();
        names.put("u1", "Carol");

        Map<String, MoWorkloadSnapshot> map = service.buildSnapshotsForPendingApplications(
                Arrays.asList(pending, accepted),
                Collections.emptyList(),
                names);

        assertEquals(1, map.size());
        assertTrue(map.containsKey("p1"));
        assertEquals("Carol", map.get("p1").applicantName);
    }

    @Test
    void potentialLoadAtThreshold_boundary() {
        Application p1 = app("p1", "u1", "Pending");
        Application p2 = app("p2", "u1", "Pending");
        Application p3 = app("p3", "u1", "Pending");
        Application a1 = app("a1", "u1", "Accepted");

        MoWorkloadSnapshot snap = service.buildSnapshotForApplication(
                "p1", Arrays.asList(p1, p2, p3, a1), Collections.emptyList(), "Dan");

        assertEquals(1, snap.acceptedCount);
        assertEquals(3, snap.pendingCount);
        assertEquals(4, snap.potentialLoadIfApprove);
        assertTrue(snap.potentialLoadIfApprove >= TaWorkloadStats.ASSIGNED_JOBS_WARNING_THRESHOLD + 1);
    }

    @Test
    void buildTaWorkloadStats_ignoresNonTaUsers() {
        User mo = user("mo1", Roles.MO);
        Application app = app("a1", "mo1", "Accepted");
        Map<String, TaWorkloadStats> stats = service.buildTaWorkloadStats(
                Collections.singletonList(mo), Collections.singletonList(app));
        assertTrue(stats.isEmpty());
    }

    @Test
    void buildTaWorkloadStats_emptyUsers() {
        Application app = app("a1", "u1", "Pending");
        assertTrue(service.buildTaWorkloadStats(Collections.emptyList(),
                Collections.singletonList(app)).isEmpty());
    }

    @Test
    void buildSnapshot_resolvesHoursFromJob() {
        Application pending = app("p1", "u1", "Pending");
        pending.moduleCode = "CS50";
        Job job = job("CS50", "6h/week");
        MoWorkloadSnapshot snap = service.buildSnapshotForApplication(
                "p1", Collections.singletonList(pending), Collections.singletonList(job), "Eve");
        assertEquals("6h/week", snap.targetWorkloadHours);
    }

    @Test
    void buildSnapshot_unknownHours_returnsUnknown() {
        Application pending = app("p1", "u1", "Pending");
        pending.moduleCode = "UNKNOWN";
        MoWorkloadSnapshot snap = service.buildSnapshotForApplication(
                "p1", Collections.singletonList(pending), Collections.emptyList(), "Eve");
        assertEquals("unknown", snap.targetWorkloadHours);
    }

    @Test
    void buildSnapshotForApplication_missingId_returnsNull() {
        assertNull(service.buildSnapshotForApplication("missing",
                Collections.emptyList(), Collections.emptyList(), "X"));
    }

    private static User user(String id, String role) {
        User u = new User();
        u.id = id;
        u.role = role;
        u.name = id;
        return u;
    }

    private static Application app(String id, String userId, String status) {
        Application a = new Application();
        a.id = id;
        a.userId = userId;
        a.moduleName = "Module " + id;
        a.moduleCode = "MOD" + id;
        a.role = "TA";
        a.applicationDate = "2026-03-01";
        a.status = status;
        return a;
    }

    private static Job job(String moduleCode, String hours) {
        Job j = new Job();
        j.setModuleCode(moduleCode);
        j.setModuleName(moduleCode);
        j.setWorkloadHours(hours);
        return j;
    }
}
