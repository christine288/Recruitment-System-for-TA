package com.bupt.ta.servlet;

import com.bupt.ta.model.User;
import com.bupt.ta.service.AuthService;
import com.bupt.ta.testsupport.FileTestSupport;
import com.bupt.ta.testsupport.ServletTestSupport;
import com.bupt.ta.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginServletTest {

    private AuthService auth;
    private LoginServlet servlet;

    @BeforeEach
    void setUp() throws Exception {
        auth = FileTestSupport.newFactory().getAuthService();
        auth.register("TA User", "2021000001", "ta@bupt.edu.cn", "pass123");
        servlet = new LoginServlet();
        ServletTestSupport.injectField(servlet, "auth", auth);
    }

    @Test
    void post_validCredentials_redirectsToJob() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        when(req.getParameter("email")).thenReturn("ta@bupt.edu.cn");
        when(req.getParameter("password")).thenReturn("pass123");
        when(req.getSession(true)).thenReturn(session);
        when(req.getContextPath()).thenReturn("/ta-recruitment");
        servlet.doPost(req, resp);
        verify(session).setAttribute(eq("user"), org.mockito.ArgumentMatchers.any(User.class));
        verify(resp).sendRedirect("/ta-recruitment/job");
    }

    @Test
    void post_admin_redirectsToAdmin() throws Exception {
        auth = FileTestSupport.newFactory().getAuthService();
        User admin = TestFixtures.sampleAdmin("a1");
        admin.email = "admin@bupt.edu.cn";
        admin.passwordHash = "admin123";
        auth.insertUser(admin);
        servlet = new LoginServlet();
        ServletTestSupport.injectField(servlet, "auth", auth);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        when(req.getParameter("email")).thenReturn("admin@bupt.edu.cn");
        when(req.getParameter("password")).thenReturn("admin123");
        when(req.getSession(true)).thenReturn(session);
        when(req.getContextPath()).thenReturn("/ta-recruitment");
        servlet.doPost(req, resp);
        verify(resp).sendRedirect("/ta-recruitment/admin");
    }

    @Test
    void post_inactiveAccount_forwardsWithError() throws Exception {
        User u = auth.register("TA", "2021000002", "inactive@bupt.edu.cn", "pass");
        auth.setUserActive(u.id, false);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(req.getParameter("email")).thenReturn("inactive@bupt.edu.cn");
        when(req.getParameter("password")).thenReturn("pass");
        when(req.getRequestDispatcher("/WEB-INF/jsp/login.jsp")).thenReturn(dispatcher);
        servlet.doPost(req, resp);
        verify(req).setAttribute(eq("error"), eq("This account has been deactivated."));
    }

    @Test
    void post_wrongPassword_forwardsWithError() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(req.getParameter("email")).thenReturn("ta@bupt.edu.cn");
        when(req.getParameter("password")).thenReturn("wrong");
        when(req.getRequestDispatcher("/WEB-INF/jsp/login.jsp")).thenReturn(dispatcher);
        servlet.doPost(req, resp);
        verify(req).setAttribute(eq("error"), eq("Invalid email or password"));
    }
}
