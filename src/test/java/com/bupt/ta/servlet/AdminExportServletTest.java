package com.bupt.ta.servlet;

import com.bupt.ta.model.User;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.testsupport.FileTestSupport;
import com.bupt.ta.testsupport.ServletTestSupport;
import com.bupt.ta.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminExportServletTest {

    private AdminExportServlet servlet;
    private ServiceFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        factory = FileTestSupport.newFactory();
        servlet = new AdminExportServlet();
        ServletTestSupport.injectField(servlet, "auth", factory.getAuthService());
        ServletTestSupport.injectField(servlet, "applications", factory.getApplicationService());
    }

    @Test
    void get_nonAdmin_forbidden() throws Exception {
        User ta = TestFixtures.sampleTa("ta-e", "ta-e@bupt.edu.cn");
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), ta,
                ServletTestSupport.params("type", "users"));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doGet(req, resp);
        verify(resp).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void get_admin_usersCsv() throws Exception {
        User admin = TestFixtures.sampleAdmin("admin-e");
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), admin,
                ServletTestSupport.params("type", "users"));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));
        servlet.doGet(req, resp);
        verify(resp).setContentType(contains("text/csv"));
    }

    @Test
    void get_admin_applicationsCsv() throws Exception {
        User admin = TestFixtures.sampleAdmin("admin-e2");
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), admin,
                ServletTestSupport.params("type", "applications"));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));
        servlet.doGet(req, resp);
        verify(resp).setContentType(contains("text/csv"));
    }
}
