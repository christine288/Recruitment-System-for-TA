package com.bupt.ta.servlet;

import com.bupt.ta.model.Job;
import com.bupt.ta.model.User;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.testsupport.FileTestSupport;
import com.bupt.ta.testsupport.ServletTestSupport;
import com.bupt.ta.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;

class JobServletTest {

    private JobServlet servlet;
    private User ta;
    private ServiceFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        factory = FileTestSupport.newFactory();
        Job job = TestFixtures.sampleJob("j1", "Module A", "MA101");
        factory.getJobService().createJob(job);
        FileTestSupport.seedUser("ta-j", "ta-j@bupt.edu.cn");
        servlet = new JobServlet();
        ServletTestSupport.injectField(servlet, "jobService", factory.getJobService());
        ServletTestSupport.injectField(servlet, "favoriteService", factory.getFavoriteService());
        ServletTestSupport.injectField(servlet, "recentlyViewedService", factory.getRecentlyViewedService());
        ServletTestSupport.injectField(servlet, "applicationService", factory.getApplicationService());
        ServletTestSupport.injectField(servlet, "profileService", factory.getProfileService());
        ta = TestFixtures.sampleTa("ta-j", "ta-j@bupt.edu.cn");
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
    void get_moRole_forbidden() throws Exception {
        User mo = TestFixtures.sampleMo("mo-j");
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), mo, null);
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doGet(req, resp);
        verify(resp).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void get_ta_listsJobs() throws Exception {
        ServletContext ctx = ServletTestSupport.mockServletContext(null);
        JobServlet spy = Mockito.spy(servlet);
        Mockito.doReturn(ctx).when(spy).getServletContext();
        HttpServletRequest req = ServletTestSupport.mockRequest(ctx, ta, null);
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        spy.doGet(req, resp);
        verify(req).getRequestDispatcher("/WEB-INF/jsp/jobs.jsp");
    }

    @Test
    void get_ta_jobDetail() throws Exception {
        ServletContext ctx = ServletTestSupport.mockServletContext(null);
        JobServlet spy = Mockito.spy(servlet);
        Mockito.doReturn(ctx).when(spy).getServletContext();
        HttpServletRequest req = ServletTestSupport.mockRequest(ctx, ta,
                ServletTestSupport.params("id", "j1"));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        spy.doGet(req, resp);
        verify(req).getRequestDispatcher("/WEB-INF/jsp/job-detail.jsp");
    }

    @Test
    void post_toggleFavorite_redirects() throws Exception {
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), ta,
                ServletTestSupport.params("action", "toggleFavorite", "jobId", "j1"));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doPost(req, resp);
        verify(resp).sendRedirect(contains("/job"));
    }
}
