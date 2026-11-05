package com.bupt.ta.servlet;

import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.service.PasswordResetService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Validates a reset token and sets a new bcrypt-hashed password.
 *
 * <p><b>URL pattern:</b> {@code /reset-password}
 *
 * <p><b>Role access:</b> Public (no login required).
 */
@WebServlet(urlPatterns = {"/reset-password"})
public class ResetPasswordServlet extends BaseServlet {

    private PasswordResetService passwordReset;

    @Override
    public void init() {
        ServiceFactory f = (ServiceFactory) getServletContext().getAttribute(ServiceFactory.SERVLET_CONTEXT_KEY);
        passwordReset = f.getPasswordResetService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String token = req.getParameter("token");
        if (token != null && !token.trim().isEmpty()) {
            req.setAttribute("token", token.trim());
            try {
                if (!passwordReset.isValidToken(token.trim())) {
                    req.setAttribute("error", "This reset link is invalid or has expired.");
                    req.setAttribute("token", "");
                }
            } catch (IOException e) {
                req.setAttribute("error", "Could not validate reset link.");
                req.setAttribute("token", "");
            }
        }
        req.getRequestDispatcher("/WEB-INF/jsp/reset-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String token = n(req.getParameter("token"));
        String password = req.getParameter("password");
        String confirm = req.getParameter("confirm");

        if (token.isEmpty()) {
            req.setAttribute("error", "Missing reset token. Use the link from your email.");
            req.getRequestDispatcher("/WEB-INF/jsp/reset-password.jsp").forward(req, resp);
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            req.setAttribute("token", token);
            req.setAttribute("error", "Password is required.");
            req.getRequestDispatcher("/WEB-INF/jsp/reset-password.jsp").forward(req, resp);
            return;
        }

        if (!password.equals(confirm)) {
            req.setAttribute("token", token);
            req.setAttribute("error", "Passwords do not match.");
            req.getRequestDispatcher("/WEB-INF/jsp/reset-password.jsp").forward(req, resp);
            return;
        }

        try {
            if (passwordReset.resetPassword(token, password)) {
                req.setAttribute("message", "Password reset completed. You may now log in.");
            } else {
                req.setAttribute("error", "This reset link is invalid or has expired.");
            }
        } catch (IOException e) {
            req.setAttribute("error", "Could not reset password. Please try again.");
        }
        req.getRequestDispatcher("/WEB-INF/jsp/reset-password.jsp").forward(req, resp);
    }

    private static String n(String s) {
        return s == null ? "" : s.trim();
    }
}
