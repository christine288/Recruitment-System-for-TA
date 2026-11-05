package com.bupt.ta.servlet;

import com.bupt.ta.model.ApplicantProfile;
import com.bupt.ta.model.Roles;
import com.bupt.ta.model.User;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.service.ProfileService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * Streams an applicant's CV file to the browser.
 *
 * <p><b>URL pattern:</b> {@code /cv} (mapped in {@code web.xml})
 *
 * <p><b>Role access:</b>
 * <ul>
 *   <li>Authenticated TA — downloads own CV (or pending session CV)</li>
 *   <li>Authenticated MO with {@code userId} parameter — downloads that applicant's CV</li>
 * </ul>
 * Unauthenticated callers are redirected to {@code /login}; unauthorized MO access returns 403.
 *
 * <p>Only GET is supported. Files are served inline with detected content type.
 */
public class CvDownloadServlet extends BaseServlet {
    private static final Pattern USER_ID_SAFE = Pattern.compile("^[a-fA-F0-9\\-]{8,64}$");

    private ProfileService profiles;
    private Path cvDataDir;

    /**
     * Initializes {@link ProfileService} from {@code WEB-INF/data}.
     */
    @Override
    public void init() {
        ServiceFactory f = (ServiceFactory) getServletContext().getAttribute(ServiceFactory.SERVLET_CONTEXT_KEY);
        profiles = f.getProfileService();
        cvDataDir = f.getCvDataDir();
    }

    /**
     * Resolves the target CV (self or MO-specified applicant) and streams the file inline.
     *
     * @param req  the incoming request; optional {@code userId} for MO viewing another applicant
     * @param resp the response; sets content type and Content-Disposition, or forwards to
     *             cv-error.jsp on failure
     * @throws ServletException if error JSP forward fails
     * @throws IOException      if file read or stream write fails
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User u = currentUser(req);
        if (u == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String paramUserId = req.getParameter("userId");
        String targetUserId;
        if (paramUserId != null && !paramUserId.trim().isEmpty()) {
            if (!Roles.MO.equals(u.role)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            String trimmed = paramUserId.trim();
            if (!USER_ID_SAFE.matcher(trimmed).matches()) {
                forwardCvError(req, resp, "Invalid applicant reference.");
                return;
            }
            targetUserId = trimmed;
        } else {
            if (!ensureTa(req, resp)) {
                return;
            }
            targetUserId = u.id;
        }

        ApplicantProfile p = profiles.getByUserId(targetUserId).orElse(null);
        String fileName = (p != null && p.cvFileName != null && !p.cvFileName.trim().isEmpty())
                ? p.cvFileName.trim() : null;

        boolean moViewingApplicant = paramUserId != null && !paramUserId.trim().isEmpty()
                && Roles.MO.equals(u.role);
        if (!moViewingApplicant && !Roles.MO.equals(u.role)) {
            Object pend = req.getSession().getAttribute(ProfileServlet.PENDING_CV_SESSION_ATTR);
            if (pend instanceof String) {
                String ps = ((String) pend).trim();
                if (!ps.isEmpty()) {
                    Path pendFile = cvDataDir.resolve(u.id).resolve(ps).normalize();
                    Path cvRootSelf = cvDataDir.resolve(u.id).normalize();
                    if (pendFile.startsWith(cvRootSelf) && Files.isRegularFile(pendFile)) {
                        fileName = ps;
                    }
                }
            }
        }

        if (fileName == null || fileName.isEmpty()) {
            forwardCvError(req, resp, "No CV is on file for this applicant.");
            return;
        }

        Path cvRoot = cvDataDir.resolve(targetUserId).normalize();
        Path file = cvRoot.resolve(fileName).normalize();
        if (!file.startsWith(cvRoot) || !Files.isRegularFile(file)) {
            forwardCvError(req, resp,
                    "The CV file could not be found. It may have been removed or archived.");
            return;
        }

        String probe = Files.probeContentType(file);
        if (probe != null) {
            resp.setContentType(probe);
        } else {
            resp.setContentType("application/octet-stream");
        }
        resp.setHeader("Content-Disposition", "inline; filename=\"" + fileName.replace("\"", "") + "\"");
        long len = Files.size(file);
        if (len <= Integer.MAX_VALUE) {
            resp.setContentLength((int) len);
        }
        try (OutputStream out = resp.getOutputStream()) {
            Files.copy(file, out);
        } catch (IOException e) {
            if (!resp.isCommitted()) {
                forwardCvError(req, resp, "The CV could not be loaded. Please try again later.");
            }
        }
    }

    private void forwardCvError(HttpServletRequest req, HttpServletResponse resp, String message)
            throws ServletException, IOException {
        req.setAttribute("cvErrorMessage", message);
        req.getRequestDispatcher("/WEB-INF/jsp/cv-error.jsp").forward(req, resp);
    }
}
