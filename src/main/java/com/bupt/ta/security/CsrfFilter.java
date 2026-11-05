package com.bupt.ta.security;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates a per-session CSRF token and validates it on every POST request.
 *
 * <p>On each request, a token is stored in the HTTP session ({@link #SESSION_ATTR}) and exposed
 * to JSPs via {@link #csrfToken(HttpServletRequest)} / {@link #REQUEST_ATTR}. State-changing
 * forms must include {@link #PARAM_NAME}; mismatches yield HTTP 403.
 *
 * <p>Registered in {@code web.xml} for all URL patterns. Safe methods (GET) are not validated.
 *
 * @see com.bupt.ta.servlet.BaseServlet
 */
public class CsrfFilter implements Filter {

    public static final String SESSION_ATTR = "csrfToken";
    public static final String PARAM_NAME = "csrfToken";
    public static final String REQUEST_ATTR = "csrfToken";

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // no-op (required for Servlet 3.0 / Tomcat 7; compile uses Servlet 4.0 defaults)
    }

    @Override
    public void destroy() {
        // no-op
    }

    /**
     * Returns the CSRF token for the current session, creating one if absent.
     */
    public static String csrfToken(HttpServletRequest req) {
        HttpSession session = req.getSession(true);
        Object existing = session.getAttribute(SESSION_ATTR);
        if (existing instanceof String) {
            String token = ((String) existing).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }
        String token = generateToken();
        session.setAttribute(SESSION_ATTR, token);
        return token;
    }

    /**
     * Validates the {@code csrfToken} request parameter against the session value.
     */
    public static boolean validateCsrf(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return false;
        }
        Object expectedObj = session.getAttribute(SESSION_ATTR);
        if (!(expectedObj instanceof String)) {
            return false;
        }
        String expected = ((String) expectedObj).trim();
        if (expected.isEmpty()) {
            return false;
        }
        String submitted = req.getParameter(PARAM_NAME);
        if (submitted == null) {
            return false;
        }
        return expected.equals(submitted.trim());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if ("POST".equalsIgnoreCase(req.getMethod())) {
            if (!validateCsrf(req)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
                return;
            }
        } else {
            csrfToken(req);
        }

        req.setAttribute(REQUEST_ATTR, csrfToken(req));
        chain.doFilter(request, response);
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
