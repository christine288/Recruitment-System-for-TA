package com.bupt.ta.servlet;

import com.bupt.ta.model.Roles;
import com.bupt.ta.model.User;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BaseServletTest {

    private static class TestServlet extends BaseServlet {
        boolean checkAdmin(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
            return ensureAdmin(req, resp);
        }

        boolean checkMo(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
            return ensureMo(req, resp);
        }

        boolean checkTa(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
            return ensureTa(req, resp);
        }

        User user(HttpServletRequest req) {
            return currentUser(req);
        }
    }

    private final TestServlet servlet = new TestServlet();

    @Test
    void urlEncode_nullAndSpecialChars() {
        assertEquals("", BaseServlet.urlEncode(null));
        assertEquals("hello+world", BaseServlet.urlEncode("hello world"));
    }

    @Test
    void currentUser_noSession_null() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getSession(false)).thenReturn(null);
        assertNull(servlet.user(req));
    }

    @Test
    void ensureAdmin_noUser_redirectsLogin() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(req.getSession(false)).thenReturn(null);
        when(req.getContextPath()).thenReturn("/ta-recruitment");
        assertEquals(false, servlet.checkAdmin(req, resp));
        verify(resp).sendRedirect("/ta-recruitment/login");
    }

    @Test
    void ensureAdmin_taRole_forbidden() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        User ta = new User("u1", "TA", "2021000001", "t@bupt.edu.cn", "x");
        ta.role = Roles.TA;
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("user")).thenReturn(ta);
        when(req.getSession(false)).thenReturn(session);
        assertEquals(false, servlet.checkAdmin(req, resp));
        verify(resp).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void ensureAdmin_admin_ok() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        User admin = new User("a1", "Admin", "2021000001", "a@bupt.edu.cn", "x");
        admin.role = Roles.ADMIN;
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("user")).thenReturn(admin);
        when(req.getSession(false)).thenReturn(session);
        assertEquals(true, servlet.checkAdmin(req, resp));
    }

    @Test
    void ensureMo_noUser_redirectsLogin() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(req.getSession(false)).thenReturn(null);
        when(req.getContextPath()).thenReturn("/ta-recruitment");
        assertEquals(false, servlet.checkMo(req, resp));
        verify(resp).sendRedirect("/ta-recruitment/login");
    }

    @Test
    void ensureMo_adminRole_forbidden() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        User admin = new User("a1", "Admin", "2021000001", "a@bupt.edu.cn", "x");
        admin.role = Roles.ADMIN;
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("user")).thenReturn(admin);
        when(req.getSession(false)).thenReturn(session);
        assertEquals(false, servlet.checkMo(req, resp));
        verify(resp).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void ensureTa_noUser_redirectsLogin() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(req.getSession(false)).thenReturn(null);
        when(req.getContextPath()).thenReturn("/ta-recruitment");
        assertEquals(false, servlet.checkTa(req, resp));
        verify(resp).sendRedirect("/ta-recruitment/login");
    }

    @Test
    void ensureTa_moRole_forbidden() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        User mo = new User("m1", "MO", "MO001", "mo@bupt.local", "x");
        mo.role = Roles.MO;
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("user")).thenReturn(mo);
        when(req.getSession(false)).thenReturn(session);
        assertEquals(false, servlet.checkTa(req, resp));
        verify(resp).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void ensureTa_taRole_ok() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        User ta = new User("t1", "TA", "2021000001", "t@bupt.edu.cn", "x");
        ta.role = Roles.TA;
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("user")).thenReturn(ta);
        when(req.getSession(false)).thenReturn(session);
        assertEquals(true, servlet.checkTa(req, resp));
    }
}
