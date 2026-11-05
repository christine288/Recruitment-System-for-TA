package com.bupt.ta.servlet;

import com.bupt.ta.model.Application;
import com.bupt.ta.model.ApplicantProfile;
import com.bupt.ta.model.Job;
import com.bupt.ta.model.JobApplicationStats;
import com.bupt.ta.model.User;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.service.ApplicationService;
import com.bupt.ta.service.AuthService;
import com.bupt.ta.service.JobService;
import com.bupt.ta.service.NotificationService;
import com.bupt.ta.service.ProfileService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lists the current user's job applications and handles submit/withdraw actions.
 *
 * <p><b>URL pattern:</b> {@code /applications}
 *
 * <p><b>Role access:</b> Authenticated users only (typically TA). Unauthenticated callers are
 * redirected to {@code /login}.
 *
 * <p>GET shows filtered application status; POST submits a new application or withdraws an
 * existing one owned by the caller.
 */
@WebServlet(urlPatterns = {"/applications"})
public class ApplicationServlet extends BaseServlet {
    private ApplicationService appService;
    private ProfileService profiles;
    private JobService jobService;
    private AuthService auth;
    private NotificationService notifications;

    /**
     * Initializes application, profile, and job services from {@code WEB-INF/data}.
     */
    @Override
    public void init() {
        ServiceFactory f = (ServiceFactory) getServletContext().getAttribute(ServiceFactory.SERVLET_CONTEXT_KEY);
        appService = f.getApplicationService();
        profiles = f.getProfileService();
        jobService = f.getJobService();
        auth = f.getAuthService();
        notifications = f.getNotificationService();
    }

    /**
     * Displays the applicant's applications, optionally filtered by status.
     *
     * @param req  the incoming request; optional {@code filter} (All, Pending, Accepted,
     *             Rejected, Withdrawn) and {@code msg}
     * @param resp the response; redirects to {@code /login} when unauthenticated
     * @throws ServletException if the JSP forward fails
     * @throws IOException      if application loading fails
     */
    // ------------------------------------------------------------------ GET
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!ensureTa(req, resp)) {
            return;
        }
        User u = currentUser(req);
        String filter = req.getParameter("filter"); // All | Pending | Accepted | Rejected | Withdrawn
        if (filter == null || filter.trim().isEmpty()) filter = "All";

        List<Application> apps = appService.getByUserId(u.id);

        long pending  = apps.stream().filter(a -> "Pending".equals(a.status)).count();
        long accepted = apps.stream().filter(a -> "Accepted".equals(a.status)).count();
        long rejected = apps.stream().filter(a -> "Rejected".equals(a.status)).count();
        long withdrawn = apps.stream().filter(a -> "Withdrawn".equals(a.status)).count();
        long countAll = apps.size();

        String msg = req.getParameter("msg");
        if (msg != null && !msg.trim().isEmpty()) {
            req.setAttribute("infoMessage", escapeHtml(msg.trim()));
        }

        List<Application> filtered;
        if ("All".equals(filter)) {
            filtered = apps;
        } else {
            final String f = filter;
            filtered = apps.stream().filter(a -> f.equals(a.status))
                           .collect(Collectors.toList());
        }

        req.setAttribute("applications", filtered);
        req.setAttribute("allApps",      apps);
        req.setAttribute("filter",       filter);
        req.setAttribute("countPending",  pending);
        req.setAttribute("countAccepted", accepted);
        req.setAttribute("countRejected", rejected);
        req.setAttribute("countWithdrawn", withdrawn);
        req.setAttribute("countAll", countAll);
        req.getRequestDispatcher("/WEB-INF/jsp/application-status.jsp").forward(req, resp);
    }

    /**
     * Withdraws an owned application or submits a new one for a published job.
     *
     * @param req  the incoming request; {@code action=withdraw} with {@code appId}, or a new
     *             application with jobId, moduleName, moduleCode, role
     * @param resp the response; redirects with flash messages on success or failure
     * @throws ServletException if dispatch fails
     * @throws IOException      if persistence fails
     */
    // ------------------------------------------------------------------ POST
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!ensureTa(req, resp)) {
            return;
        }
        User u = currentUser(req);

        String action = req.getParameter("action");

        // --- Withdraw an existing application ---
        if ("withdraw".equals(action)) {
            String appId = req.getParameter("appId");
            if (appId != null && !appId.trim().isEmpty()) {
                // Only allow the owner to withdraw
                List<Application> mine = appService.getByUserId(u.id);
                boolean owns = mine.stream().anyMatch(a -> appId.trim().equals(a.id));
                if (owns) {
                    appService.updateStatus(appId.trim(), "Withdrawn", "");
                    resp.sendRedirect(req.getContextPath() + "/applications?msg=" + urlEncode("Application withdrawn."));
                    return;
                }
            }
            resp.sendRedirect(req.getContextPath() + "/applications");
            return;
        }

        // --- Submit a new application ---
        String jobId     = req.getParameter("jobId");
        String moduleName = req.getParameter("moduleName");
        String moduleCode = req.getParameter("moduleCode");
        String role       = req.getParameter("role");
        String ctx = req.getContextPath();

        if (moduleName == null || moduleName.trim().isEmpty()) {
            resp.sendRedirect(ctx + "/job");
            return;
        }

        // Require completed profile before applying for a job.
        ApplicantProfile profile = profiles.getByUserId(u.id).orElse(null);
        if (!isProfileComplete(profile)) {
            resp.sendRedirect(ctx + "/profile?msg="
                    + urlEncode("Please complete your profile before applying for a job."));
            return;
        }

        if (jobId == null || jobId.trim().isEmpty()) {
            resp.sendRedirect(ctx + "/job?err=" + urlEncode("Missing job; open the listing and apply again."));
            return;
        }

        Job job = jobService.getJobById(jobId.trim());
        if (job == null) {
            resp.sendRedirect(ctx + "/job?err=" + urlEncode("That job listing no longer exists."));
            return;
        }

        if (isPastDeadline(job.getApplicationDeadline())) {
            resp.sendRedirect(ctx + "/job?id=" + urlEncode(jobId.trim()) + "&err=" + urlEncode(
                    "The application deadline for this job has passed."));
            return;
        }

        String jn = job.getModuleName() == null ? "" : job.getModuleName().trim();
        String jc = job.getModuleCode() == null ? "" : job.getModuleCode().trim();
        String pn = moduleName.trim();
        String pc = moduleCode != null ? moduleCode.trim() : "";
        if (!jn.equals(pn) || !jc.equals(pc)) {
            resp.sendRedirect(ctx + "/job?id=" + urlEncode(jobId.trim()) + "&err=" + urlEncode(
                    "Job details do not match this listing; please apply from the job page."));
            return;
        }

        List<Application> allForJob = appService.listAll();
        JobApplicationStats stats = JobApplicationStats.forJob(allForJob, jn, jc);
        int capacity = JobApplicationStats.parseCapacity(job.getNumberOfTAs());
        if (stats.accepted >= capacity) {
            resp.sendRedirect(ctx + "/job?id=" + urlEncode(jobId.trim()) + "&err=" + urlEncode(
                    "All TA slots for this job are filled; new applications are not accepted."));
            return;
        }

        // Prevent duplicate active applications for the same job (module + code)
        List<Application> existing = appService.getByUserId(u.id);
        boolean duplicate = existing.stream().anyMatch(a ->
                JobApplicationStats.matchesJob(a, jn, jc) &&
                !"Withdrawn".equalsIgnoreCase(a.status) &&
                !"Rejected".equalsIgnoreCase(a.status)
        );
        if (duplicate) {
            String encodedMsg = urlEncode(
                    "You already have an active application for this job.");
            resp.sendRedirect(ctx + "/job?id=" + urlEncode(jobId.trim()) + "&err=" + encodedMsg);
            return;
        }

        Application app = new Application(
            UUID.randomUUID().toString(),
            u.id,
            moduleName.trim(),
            moduleCode != null ? moduleCode.trim() : "",
            role != null && !role.trim().isEmpty() ? role.trim() : "Teaching Assistant",
            LocalDate.now().toString()
        );
        try {
            appService.save(app);
        } catch (IOException e) {
            resp.sendRedirect(ctx + "/job?id=" + urlEncode(jobId.trim()) + "&err=" + urlEncode(
                    "Could not save your application. Please try again."));
            return;
        }

        String moduleLabel = pn + (pc.isEmpty() ? "" : " (" + pc + ")");
        String applicantEmail = profile.email != null && !profile.email.trim().isEmpty()
                ? profile.email.trim()
                : auth.findById(u.id).map(user -> user.email).orElse("");
        String applicantName = profile.fullName != null ? profile.fullName : u.name;
        notifications.sendStatusChangeEmail(applicantEmail, applicantName, moduleLabel, "Pending", "");

        resp.sendRedirect(ctx + "/applications?msg=" + urlEncode(
                "Application submitted for " + pn + (pc.isEmpty() ? "" : " (" + pc + ")") + "."));
    }

    private static boolean isPastDeadline(String deadline) {
        if (deadline == null || deadline.trim().isEmpty()) {
            return false;
        }
        try {
            LocalDate d = LocalDate.parse(deadline.trim());
            return LocalDate.now().isAfter(d);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean isProfileComplete(ApplicantProfile p) {
        return ProfileService.isApplicantProfileComplete(p);
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
