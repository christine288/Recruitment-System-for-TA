package com.bupt.ta.servlet;

import com.bupt.ta.model.Job;
import com.bupt.ta.model.User;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.service.ApplicationService;
import com.bupt.ta.testsupport.FileTestSupport;
import com.bupt.ta.testsupport.ServletTestSupport;
import com.bupt.ta.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;

class MoServletTest {

    private MoServlet servlet;
    private User mo;
    private ServiceFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        factory = FileTestSupport.newFactory();
        mo = TestFixtures.sampleMo("mo-1");
        servlet = new MoServlet();
        ServletTestSupport.injectField(servlet, "applications", factory.getApplicationService());
        ServletTestSupport.injectField(servlet, "auth", factory.getAuthService());
        ServletTestSupport.injectField(servlet, "workloadService", factory.getWorkloadService());
        ServletTestSupport.injectField(servlet, "jobs", factory.getJobService());
        ServletTestSupport.injectField(servlet, "profiles", factory.getProfileService());
        ServletTestSupport.injectField(servlet, "notifications", factory.getNotificationService());
        ServletTestSupport.injectField(servlet, "audit", factory.getAuditService());
    }

    @Test
    void get_nonMo_forbidden() throws Exception {
        User ta = TestFixtures.sampleTa("ta-1", "ta@bupt.edu.cn");
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), ta, null);
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doGet(req, resp);
        verify(resp).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void post_createJobDraft() throws Exception {
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), mo,
                ServletTestSupport.params(
                        "action", "createJob",
                        "moduleName", "New Module",
                        "moduleCode", "NM101",
                        "description", "A description",
                        "numberOfTAs", "2"));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doPost(req, resp);
        verify(resp).sendRedirect(contains("/mo?msg="));
    }

    @Test
    void post_approveApplication() throws Exception {
        FileTestSupport.seedUser("ta-1", "ta@bupt.edu.cn");
        Job job = TestFixtures.sampleJob("job-mo", "CS101", "CS101");
        job.setCreatedByMoId("mo-1");
        factory.getJobService().createJob(job);
        ApplicationService apps = factory.getApplicationService();
        apps.save(TestFixtures.sampleApplication("app-1", "ta-1", "CS101", "CS101"));
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), mo,
                ServletTestSupport.params(
                        "action", "approveApp",
                        "appId", "app-1",
                        "feedback", "Welcome"));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doPost(req, resp);
        verify(resp).sendRedirect(contains("Accepted"));
    }

    @Test
    void post_approveOtherMoJob_denied() throws Exception {
        FileTestSupport.seedUser("ta-1", "ta@bupt.edu.cn");
        Job otherJob = TestFixtures.sampleJob("job-other", "OTHER", "OTH");
        otherJob.setCreatedByMoId("mo-other");
        factory.getJobService().createJob(otherJob);
        ApplicationService apps = factory.getApplicationService();
        apps.save(TestFixtures.sampleApplication("app-other", "ta-1", "OTHER", "OTH"));
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), mo,
                ServletTestSupport.params("action", "approveApp", "appId", "app-other"));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doPost(req, resp);
        verify(resp).sendRedirect(contains("your+own+jobs"));
    }

    @Test
    void post_closeJob_owned() throws Exception {
        Job job = TestFixtures.sampleJob("job-close", "CLOSE", "CLS");
        job.setCreatedByMoId("mo-1");
        factory.getJobService().createJob(job);
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), mo,
                ServletTestSupport.params("action", "closeJob", "jobId", "job-close"));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doPost(req, resp);
        verify(resp).sendRedirect(contains("Job+closed"));
        assertEquals("Closed", factory.getJobService().getJobById("job-close").getStatus());
    }

    @Test
    void post_missingFields_redirectsWithMessage() throws Exception {
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), mo,
                ServletTestSupport.params("action", "createJob", "moduleName", "", "moduleCode", "", "description", ""));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doPost(req, resp);
        verify(resp).sendRedirect(contains("/mo?msg="));
    }
}
