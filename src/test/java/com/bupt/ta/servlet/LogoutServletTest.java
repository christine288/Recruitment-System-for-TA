package com.bupt.ta.servlet;

import com.bupt.ta.testsupport.ServletTestSupport;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogoutServletTest {

    @Test
    void doGet_invalidatesSessionAndRedirects() throws Exception {
        LogoutServlet servlet = new LogoutServlet();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        when(req.getSession(false)).thenReturn(session);
        when(req.getContextPath()).thenReturn("/ta-recruitment");
        servlet.doGet(req, resp);
        verify(session).invalidate();
        verify(resp).sendRedirect("/ta-recruitment/login");
    }
}
