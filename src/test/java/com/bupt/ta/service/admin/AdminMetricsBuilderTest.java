package com.bupt.ta.service.admin;

import com.bupt.ta.model.Application;
import com.bupt.ta.model.Job;
import com.bupt.ta.model.Roles;
import com.bupt.ta.model.TaWorkloadStats;
import com.bupt.ta.model.User;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminMetricsBuilderTest {

    @Test
    void build_countsUsersRolesStatusAndMonthlySeries() {
        User ta1 = user("u1", Roles.TA, true);
        User ta2 = user("u2", Roles.TA, true);
        User mo = user("m1", Roles.MO, true);
        User inactive = user("i1", Roles.TA, false);

        Application a1 = app("u1", "CS101", "Pending", "2026-03-15");
        Application a2 = app("u2", "CS102", "Accepted", "2026-04-10");
        Application a3 = app("u2", "CS103", "Rejected", "2026-04-20");
        Application a4 = app("u1", "CS104", "Accepted", "2026-04-05");

        Job job = new Job();
        job.setModuleName("CS101");

        AdminDashboardMetrics m = AdminMetricsBuilder.build(
                Arrays.asList(ta1, ta2, mo, inactive),
                Arrays.asList(a1, a2, a3, a4),
                Collections.singletonList(job),
                Collections.emptyMap());

        assertEquals(4, m.totalUsers);
        assertEquals(3, m.activeUsers);
        assertEquals(1, m.inactiveUsers);
        assertEquals(1, m.totalJobs);
        assertEquals(4, m.totalApplications);
        assertEquals(3, m.usersByRole.get(Roles.TA));
        assertEquals(1, m.usersByRole.get(Roles.MO));
        assertEquals(1, m.appsByStatus.get("Pending"));
        assertEquals(2, m.appsByStatus.get("Accepted"));
        assertEquals(1, m.appsByStatus.get("Rejected"));
        assertEquals(2, m.monthlyApplications.size());
        assertEquals("2026-03", m.monthlyApplications.get(0).label);
        assertEquals(1, m.monthlyApplications.get(0).value);
        assertEquals("2026-04", m.monthlyApplications.get(1).label);
        assertEquals(3, m.monthlyApplications.get(1).value);
        assertEquals(2, m.monthlyApplicantPool.get(1).value);
    }

    @Test
    void build_highWorkloadFromTaStats() {
        TaWorkloadStats hot = new TaWorkloadStats();
        hot.accepted = TaWorkloadStats.ASSIGNED_JOBS_WARNING_THRESHOLD;
        Map<String, TaWorkloadStats> workload = new HashMap<>();
        workload.put("u1", hot);

        AdminDashboardMetrics m = AdminMetricsBuilder.build(
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), workload);

        assertEquals(1, m.highWorkloadTaCount);
    }

    @Test
    void toChartJson_containsExpectedKeys() {
        AdminDashboardMetrics m = new AdminDashboardMetrics();
        m.totalUsers = 5;
        m.appsByStatus.put("Pending", 2);
        m.monthlyApplications.add(new AdminDashboardMetrics.SeriesPoint("2026-01", 3));
        m.topModules.add(new AdminDashboardMetrics.SeriesPoint("CS101", 4));

        String json = AdminMetricsBuilder.toChartJson(m);

        assertTrue(json.contains("\"totalUsers\":5"));
        assertTrue(json.contains("\"appsByStatus\""));
        assertTrue(json.contains("\"monthlyApplications\""));
        assertTrue(json.contains("\"topModules\""));
        assertTrue(json.contains("CS101"));
    }

    private static User user(String id, String role, boolean active) {
        User u = new User();
        u.id = id;
        u.role = role;
        u.active = active;
        return u;
    }

    private static Application app(String userId, String module, String status, String date) {
        Application a = new Application();
        a.userId = userId;
        a.moduleName = module;
        a.status = status;
        a.applicationDate = date;
        return a;
    }
}
