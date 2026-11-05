package com.bupt.ta.servlet;

import com.bupt.ta.model.Roles;
import com.bupt.ta.model.User;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.service.AuthService;
import com.bupt.ta.testsupport.FileTestSupport;
import com.bupt.ta.testsupport.ServletTestSupport;
import com.bupt.ta.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;

class AdminUserServletTest {

    private AdminUserServlet servlet;
    private AuthService auth;
    private ServiceFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        factory = FileTestSupport.newFactory();
        auth = factory.getAuthService();
        servlet = new AdminUserServlet();
        ServletTestSupport.injectField(servlet, "cvDataDir", factory.getCvDataDir());
        ServletTestSupport.injectField(servlet, "auth", auth);
        ServletTestSupport.injectField(servlet, "applications", factory.getApplicationService());
        ServletTestSupport.injectField(servlet, "profiles", factory.getProfileService());
        ServletTestSupport.injectField(servlet, "audit", factory.getAuditService());
    }

    @Test
    void post_activateUser() throws Exception {
        seedUser("u2", Roles.TA, false);
        User admin = TestFixtures.sampleAdmin("admin1");
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), admin,
                ServletTestSupport.params("action", "activate", "userId", "u2"));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doPost(req, resp);
        verify(resp).sendRedirect(contains("/admin"));
        assertTrue(auth.findById("u2").get().active);
    }

    @Test
    void post_cannotModifySelf() throws Exception {
        User admin = TestFixtures.sampleAdmin("admin1");
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), admin,
                ServletTestSupport.params("action", "deactivate", "userId", "admin1"));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doPost(req, resp);
        verify(resp).sendRedirect(contains("/admin?msg="));
    }

    @Test
    void post_nonAdmin_forbidden() throws Exception {
        User ta = TestFixtures.sampleTa("u1", "ta@bupt.edu.cn");
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), ta,
                ServletTestSupport.params("action", "activate", "userId", "u2"));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doPost(req, resp);
        verify(resp).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void post_deleteUser_removesFromAuth() throws Exception {
        seedUser("u2", Roles.TA, true);
        User admin = TestFixtures.sampleAdmin("admin1");
        HttpServletRequest req = ServletTestSupport.mockRequest(
                ServletTestSupport.mockServletContext(null), admin,
                ServletTestSupport.params("action", "delete", "userId", "u2"));
        HttpServletResponse resp = ServletTestSupport.mockResponse();
        servlet.doPost(req, resp);
        assertFalse(auth.findById("u2").isPresent());
    }

    private void seedUser(String id, String role, boolean active) throws Exception {
        User u = TestFixtures.sampleUser(id, id + "@bupt.edu.cn", role);
        u.passwordHash = "x";
        u.active = active;
        auth.insertUser(u);
    }
}
