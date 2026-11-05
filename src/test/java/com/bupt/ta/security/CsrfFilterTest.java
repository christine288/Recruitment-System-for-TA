package com.bupt.ta.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CsrfFilterTest {

    @Mock HttpServletRequest req;
    @Mock HttpServletResponse resp;
    @Mock FilterChain chain;
    @Mock HttpSession session;

    private CsrfFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new CsrfFilter();
        when(req.getSession(true)).thenReturn(session);
        when(req.getSession(false)).thenReturn(session);
    }

    @Test
    void getRequest_generatesToken() throws Exception {
        when(req.getMethod()).thenReturn("GET");
        when(session.getAttribute(CsrfFilter.SESSION_ATTR)).thenReturn(null);
        filter.doFilter(req, resp, chain);
        verify(session, org.mockito.Mockito.atLeastOnce()).setAttribute(
                org.mockito.ArgumentMatchers.eq(CsrfFilter.SESSION_ATTR),
                org.mockito.ArgumentMatchers.anyString());
        verify(chain).doFilter(req, resp);
    }

    @Test
    void postRequest_validToken_passes() throws Exception {
        when(req.getMethod()).thenReturn("POST");
        when(session.getAttribute(CsrfFilter.SESSION_ATTR)).thenReturn("tok123");
        when(req.getParameter("csrfToken")).thenReturn("tok123");
        filter.doFilter(req, resp, chain);
        verify(chain).doFilter(req, resp);
    }

    @Test
    void postRequest_invalidToken_forbidden() throws Exception {
        when(req.getMethod()).thenReturn("POST");
        when(session.getAttribute(CsrfFilter.SESSION_ATTR)).thenReturn("tok123");
        when(req.getParameter("csrfToken")).thenReturn("bad");
        filter.doFilter(req, resp, chain);
        verify(resp).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
        verify(chain, never()).doFilter(req, resp);
    }

    @Test
    void postRequest_missingToken_forbidden() throws Exception {
        when(req.getMethod()).thenReturn("POST");
        when(session.getAttribute(CsrfFilter.SESSION_ATTR)).thenReturn("tok123");
        when(req.getParameter("csrfToken")).thenReturn(null);
        filter.doFilter(req, resp, chain);
        verify(resp).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
    }

    @Test
    void csrfToken_reusesExisting() {
        when(session.getAttribute(CsrfFilter.SESSION_ATTR)).thenReturn("existing");
        String token = CsrfFilter.csrfToken(req);
        org.junit.jupiter.api.Assertions.assertEquals("existing", token);
    }
}
