package com.bupt.ta.servlet;

import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.service.PasswordResetService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Displays the forgot-password form and initiates a password reset token.
 *
 * <p><b>URL pattern:</b> {@code /forgot-password}
 *
 * <p><b>Role access:</b> Public (no login required).
 *
 * <p>POST always shows a generic message to avoid leaking whether an email is registered.
 * When SMTP is not configured, the reset token is logged server-side for development.
 */
@WebServlet(urlPatterns = {"/forgot-password"})
public class ForgotPasswordServlet extends BaseServlet {

    private PasswordResetService passwordReset;

    @Override
    public void init() {
        ServiceFactory f = (ServiceFactory) getServletContext().getAttribute(ServiceFactory.SERVLET_CONTEXT_KEY);
        passwordReset = f.getPasswordResetService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/forgot-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String email = req.getParameter("email");
        if (email != null && !email.trim().isEmpty()) {
            try {
                passwordReset.createTokenForEmail(email.trim());
            } catch (Exception ignored) {
                // generic response regardless of outcome
            }
        }
        req.setAttribute("message",
                "If that email is registered, reset instructions have been sent.");
        req.getRequestDispatcher("/WEB-INF/jsp/forgot-password.jsp").forward(req, resp);
    }
}
