package com.bupt.ta.servlet;

import com.bupt.ta.model.Job;
import com.bupt.ta.model.User;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.service.ApplicationService;
import com.bupt.ta.service.JobService;
import com.bupt.ta.service.ProfileService;
import com.bupt.ta.testsupport.FileTestSupport;
import com.bupt.ta.testsupport.ServletTestSupport;
import com.bupt.ta.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;

class ApplicationServletTest {

    private ApplicationServlet servlet;
    private User ta;
    private ServiceFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        factory = FileTestSupport.newFactory();
        FileTestSupport.seedUser("ta-1", "ta@bupt.edu.cn");
        Job job = TestFixtures.sampleJob("job-1", "CS101", "CS101");
        factory.getJobService().createJob(job);
        servlet = new ApplicationServlet();
        ServletTestSupport.injectField(servlet, "appService", factory.getApplicationService());
        ServletTestSupport.injectField(servlet, "profiles", factory.getProfileService());
        ServletTestSupport.injectField(servlet, "jobService", factory.getJobService());
        ServletTestSupport.injectField(servlet, "auth", factory.getAuthService());
        ServletTestSupport.injectField(servlet, "notifications", factory.getNotificationService());
        ta = TestFixtures.sampleTa("ta-1", "ta@bupt.edu.cn");
    }

    @Test
    void get_notLoggedIn_redirectsLogin() throws Exception {
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), (User) null, null);
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doGet(req, resp);
        verify(resp).sendRedirect(contains("/login"));
    }

    @Test
    void post_incompleteProfile_redirectsProfile() throws Exception {
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), ta,
                ServletTestSupport.params(
                        "jobId", "job-1",
                        "moduleName", "CS101",
                        "moduleCode", "CS101",
                        "role", "TA"));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doPost(req, resp);
        verify(resp).sendRedirect(contains("/profile?msg="));
    }

    @Test
    void post_successfulApply_redirectsApplications() throws Exception {
        factory.getProfileService().upsert(TestFixtures.completeProfile("ta-1"));
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), ta,
                ServletTestSupport.params(
                        "jobId", "job-1",
                        "moduleName", "CS101",
                        "moduleCode", "CS101",
                        "role", "TA"));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doPost(req, resp);
        verify(resp).sendRedirect(contains("/applications?msg="));
    }

    @Test
    void post_duplicateApply_redirectsWithError() throws Exception {
        factory.getProfileService().upsert(TestFixtures.completeProfile("ta-1"));
        ApplicationService apps = factory.getApplicationService();
        apps.save(TestFixtures.sampleApplication("existing", "ta-1", "CS101", "CS101"));

        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), ta,
                ServletTestSupport.params(
                        "jobId", "job-1",
                        "moduleName", "CS101",
                        "moduleCode", "CS101",
                        "role", "TA"));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doPost(req, resp);
        verify(resp).sendRedirect(contains("/job?id=job-1&err="));
    }

    @Test
    void post_withdraw_ownApplication() throws Exception {
        ApplicationService apps = factory.getApplicationService();
        apps.save(TestFixtures.sampleApplication("app-w", "ta-1", "CS101", "CS101"));
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), ta,
                ServletTestSupport.params("action", "withdraw", "appId", "app-w"));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doPost(req, resp);
        verify(resp).sendRedirect(contains("/applications?msg="));
    }

    @Test
    void post_pastDeadline_redirectsWithError() throws Exception {
        Job job = TestFixtures.sampleJob("job-expired", "CS102", "CS102");
        job.setApplicationDeadline("2020-01-01");
        factory.getJobService().createJob(job);
        factory.getProfileService().upsert(TestFixtures.completeProfile("ta-1"));
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), ta,
                ServletTestSupport.params(
                        "jobId", "job-expired",
                        "moduleName", "CS102",
                        "moduleCode", "CS102",
                        "role", "TA"));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doPost(req, resp);
        verify(resp).sendRedirect(contains("/job?id=job-expired&err="));
    }

    @Test
    void post_missingCv_allowsApplication() throws Exception {
        var profile = TestFixtures.completeProfile("ta-1");
        profile.cvFileName = "";
        factory.getProfileService().upsert(profile);
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), ta,
                ServletTestSupport.params(
                        "jobId", "job-1",
                        "moduleName", "CS101",
                        "moduleCode", "CS101",
                        "role", "TA"));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doPost(req, resp);
        verify(resp).sendRedirect(contains("/applications?msg="));
    }
}
