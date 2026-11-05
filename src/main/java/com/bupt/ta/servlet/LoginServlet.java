package com.bupt.ta.servlet;

import com.bupt.ta.model.Roles;
import com.bupt.ta.model.User;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.service.AuthService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;

/**
 * Handles user authentication and session creation.
 *
 * <p><b>URL pattern:</b> {@code /login}
 *
 * <p><b>Role access:</b> Public (no login required). On success, redirects by role:
 * ADMIN → {@code /admin}, MO → {@code /mo}, TA → {@code /job}.
 *
 * <p>GET displays the login form; POST validates credentials and establishes the session.
 */
@WebServlet(urlPatterns = {"/login"})
public class LoginServlet extends BaseServlet {
    private AuthService auth;

    /**
     * Initializes {@link AuthService} using the {@code WEB-INF/data} directory.
     */
    @Override
    public void init() {
        ServiceFactory f = (ServiceFactory) getServletContext().getAttribute(ServiceFactory.SERVLET_CONTEXT_KEY);
        auth = f.getAuthService();
    }

    /**
     * Forwards to the login JSP.
     *
     * @param req  the incoming request
     * @param resp the response
     * @throws ServletException if the JSP forward fails
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(req, resp);
    }

    /**
     * Validates email and password, rejects deactivated accounts, and redirects by role on success.
     *
     * @param req  the incoming request; expects {@code email} and {@code password} parameters
     * @param resp the response; redirects to the role-specific home on success, or re-forwards
     *             to the login JSP with an error attribute on failure
     * @throws ServletException if the JSP forward fails
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String email    = req.getParameter("email");
        String password = req.getParameter("password");
        Optional<User> u = auth.verifyCredentials(email, password);
        if (!u.isPresent()) {
            req.setAttribute("error", "Invalid email or password");
            req.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(req, resp);
            return;
        }
        User candidate = u.get();
        if (!candidate.active) {
            req.setAttribute("error", "This account has been deactivated.");
            req.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(req, resp);
            return;
        }
        User loggedIn = candidate;
        HttpSession oldSession = req.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession session = req.getSession(true);
        session.setAttribute("user", loggedIn);

        String next;
        if (Roles.ADMIN.equals(loggedIn.role)) {
            next = "/admin";
        } else if (Roles.MO.equals(loggedIn.role)) {
            next = "/mo";
        } else {
            next = "/job";
        }

        resp.sendRedirect(req.getContextPath() + next);
    }
}
