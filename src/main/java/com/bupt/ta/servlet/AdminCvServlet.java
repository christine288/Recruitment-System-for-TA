package com.bupt.ta.servlet;

import com.bupt.ta.model.ApplicantProfile;
import com.bupt.ta.model.Roles;
import com.bupt.ta.model.User;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.service.AuthService;
import com.bupt.ta.service.ProfileService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Admin endpoint to download a TA applicant's CV inline.
 *
 * <p><b>URL pattern:</b> {@code /admin/cv}
 *
 * <p><b>Role access:</b> {@link com.bupt.ta.model.Roles#ADMIN} only via {@link #ensureAdmin}.
 *
 * <p>Requires {@code userId} for a TA user with a stored CV. Only GET is supported.
 */
@WebServlet("/admin/cv")
public class AdminCvServlet extends BaseServlet {

    private AuthService authService;
    private ProfileService profileService;
    private Path cvDataDir;

    /**
     * Initializes auth and profile services from {@code WEB-INF/data}.
     */
    @Override
    public void init() {
        ServiceFactory f = (ServiceFactory) getServletContext().getAttribute(ServiceFactory.SERVLET_CONTEXT_KEY);
        this.authService = f.getAuthService();
        this.profileService = f.getProfileService();
        cvDataDir = f.getCvDataDir();
    }

    /**
     * Streams the CV file for the specified TA user.
     *
     * @param req  the incoming request; requires {@code userId}
     * @param resp the response; sets content type inline; 400/404 on invalid or missing CV;
     *             403 when not admin
     * @throws ServletException if dispatch fails
     * @throws IOException      if file read or stream write fails
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!ensureAdmin(req, resp)) {
            return;
        }
        String userId = req.getParameter("userId");
        if (userId == null || userId.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        userId = userId.trim();
        if (userId.contains("..") || userId.indexOf('/') >= 0 || userId.indexOf('\\') >= 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        User target = authService.findById(userId).orElse(null);
        if (target == null || !Roles.TA.equals(target.role)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        ApplicantProfile p = profileService.getByUserId(userId).orElse(null);
        if (p == null || p.cvFileName == null || p.cvFileName.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path cvRoot = cvDataDir.normalize();
        Path userCvDir = cvRoot.resolve(userId).normalize();
        if (!userCvDir.startsWith(cvRoot)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        Path file = userCvDir.resolve(p.cvFileName).normalize();
        if (!file.startsWith(userCvDir) || !Files.isRegularFile(file)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String probe = Files.probeContentType(file);
        if (probe != null) {
            resp.setContentType(probe);
        } else {
            resp.setContentType("application/octet-stream");
        }
        resp.setHeader("Content-Disposition", "inline; filename=\"" + p.cvFileName.replace("\"", "") + "\"");
        long len = Files.size(file);
        if (len <= Integer.MAX_VALUE) {
            resp.setContentLength((int) len);
        }
        try (OutputStream out = resp.getOutputStream()) {
            Files.copy(file, out);
        }
    }
}
