package com.bupt.ta.servlet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Terminates the current HTTP session and returns the user to the login page.
 *
 * <p><b>URL pattern:</b> {@code /logout}
 *
 * <p><b>Role access:</b> Public (any authenticated or anonymous caller may invoke logout).
 *
 * <p>Only GET is supported; POST is not handled.
 */
@WebServlet(urlPatterns = {"/logout"})
public class LogoutServlet extends BaseServlet {

    /**
     * Invalidates the current session, if one exists, and redirects to {@code /login}.
     *
     * @param req  the incoming request
     * @param resp the response; always redirects to the login page
     * @throws IOException if the redirect fails
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (req.getSession(false) != null) {
            req.getSession(false).invalidate();
        }
        resp.sendRedirect(req.getContextPath() + "/login");
    }
}
