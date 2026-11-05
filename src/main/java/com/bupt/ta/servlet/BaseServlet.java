package com.bupt.ta.servlet;

import com.bupt.ta.model.Roles;
import com.bupt.ta.model.User;
import com.bupt.ta.security.CsrfFilter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Abstract base class for all application servlets.
 *
 * <p>Provides shared session helpers and role-based access guards used by admin and MO endpoints.
 * Concrete servlets extend this class and declare their own {@code @WebServlet} mappings or
 * {@code web.xml} entries.
 *
 * <p>Not mapped to a URL directly; subclasses supply URL patterns and authorization rules.
 */
public abstract class BaseServlet extends HttpServlet {

    /**
     * URL-encodes a string for safe use in query parameters (e.g. {@code /admin?msg=...}).
     *
     * @param s the raw message or parameter value; {@code null} is treated as an empty string
     * @return the UTF-8 URL-encoded form of {@code s}
     */
    protected static String urlEncode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    /**
     * Returns the currently logged-in user from the HTTP session, if any.
     *
     * @param req the incoming request whose session is inspected
     * @return the {@link User} stored under session attribute {@code "user"}, or {@code null}
     *         when there is no session or no valid user object
     */
    protected User currentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object u = session.getAttribute("user");
        return (u instanceof User) ? (User) u : null;
    }

    /**
     * Ensures the caller is authenticated and has the {@link Roles#ADMIN} role.
     *
     * <p>Unauthenticated users are redirected to {@code /login}. Authenticated non-admins receive
     * HTTP 403 Forbidden.
     *
     * @param req  the incoming request
     * @param resp the response used for redirect or error
     * @return {@code true} if the caller is an admin and may continue; {@code false} if the
     *         response has already been committed (redirect or 403)
     * @throws IOException if sending the redirect or error fails
     */
    protected boolean ensureAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = currentUser(req);
        if (u == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return false;
        }
        if (!Roles.ADMIN.equals(u.role)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

    /**
     * Ensures the caller is authenticated and has the {@link Roles#MO} (module owner) role.
     *
     * <p>Unauthenticated users are redirected to {@code /login}. Authenticated non-MO users receive
     * HTTP 403 Forbidden.
     *
     * @param req  the incoming request
     * @param resp the response used for redirect or error
     * @return {@code true} if the caller is an MO and may continue; {@code false} if the response
     *         has already been committed (redirect or 403)
     * @throws IOException if sending the redirect or error fails
     */
    protected boolean ensureMo(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = currentUser(req);
        if (u == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return false;
        }
        if (!Roles.MO.equals(u.role)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

    /**
     * Ensures the caller is authenticated and has the {@link Roles#TA} role.
     *
     * <p>Unauthenticated users are redirected to {@code /login}. Authenticated non-TA users receive
     * HTTP 403 Forbidden.
     *
     * @param req  the incoming request
     * @param resp the response used for redirect or error
     * @return {@code true} if the caller is a TA and may continue; {@code false} if the response
     *         has already been committed (redirect or 403)
     * @throws IOException if sending the redirect or error fails
     */
    protected boolean ensureTa(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = currentUser(req);
        if (u == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return false;
        }
        if (!Roles.TA.equals(u.role)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

    /**
     * Returns the CSRF token for the current session (see {@link CsrfFilter}).
     *
     * @param req the incoming request
     * @return session CSRF token, or {@code null} when no session exists
     */
    protected String csrfToken(HttpServletRequest req) {
        return CsrfFilter.csrfToken(req);
    }

    /**
     * Validates the submitted CSRF token against the session value.
     *
     * @param req the incoming request (form field or header)
     * @return {@code true} when the token matches the session value
     */
    protected boolean validateCsrf(HttpServletRequest req) {
        return CsrfFilter.validateCsrf(req);
    }
}
