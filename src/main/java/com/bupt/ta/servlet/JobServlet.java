package com.bupt.ta.servlet;

import com.bupt.ta.ai.LmConfig;
import com.bupt.ta.model.Application;
import com.bupt.ta.model.ApplicantProfile;
import com.bupt.ta.model.Job;
import com.bupt.ta.model.JobApplicationStats;
import com.bupt.ta.model.User;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.service.ApplicationService;
import com.bupt.ta.service.FavoriteService;
import com.bupt.ta.service.JobService;
import com.bupt.ta.service.ProfileService;
import com.bupt.ta.service.RecentlyViewedService;
import com.bupt.ta.util.JobListFilters;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Serves the TA job listing, job detail pages, and favorite toggling.
 *
 * <p><b>URL pattern:</b> {@code /job}
 *
 * <p><b>Role access:</b> Authenticated users only (typically TA). Unauthenticated callers are
 * redirected to {@code /login}.
 *
 * <ul>
 *   <li>{@code GET /job} — published job list with search, sort, favorites, and AI state</li>
 *   <li>{@code GET /job?id=xxx} — single job detail with application context</li>
 *   <li>{@code POST /job?action=toggleFavorite} — save or unsave a job</li>
 * </ul>
 */
@WebServlet("/job")
public class JobServlet extends BaseServlet {

    private JobService jobService;
    private FavoriteService favoriteService;
    private RecentlyViewedService recentlyViewedService;
    private ProfileService profileService;
    private ApplicationService applicationService;

    /**
     * Initializes job, favorite, profile, and application services from {@code WEB-INF/data}.
     *
     * @throws ServletException if servlet initialization fails
     */
    @Override
    public void init() throws ServletException {
        ServiceFactory f = (ServiceFactory) getServletContext().getAttribute(ServiceFactory.SERVLET_CONTEXT_KEY);
        this.jobService = f.getJobService();
        this.favoriteService = f.getFavoriteService();
        this.recentlyViewedService = f.getRecentlyViewedService();
        this.profileService = f.getProfileService();
        this.applicationService = f.getApplicationService();
    }

    /**
     * Renders the job list or a single job detail depending on the {@code id} parameter.
     *
     * @param req  the incoming request; optional {@code id}, {@code q}, {@code sortBy}
     * @param resp the response; redirects to {@code /login} when unauthenticated
     * @throws ServletException if the JSP forward fails
     * @throws IOException      if service calls fail
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!ensureTa(req, resp)) {
            return;
        }
        User user = currentUser(req);

        String id = req.getParameter("id");

        if (id == null) {
            // ── LIST PAGE ──
            String q = safe(req.getParameter("q"));
            String sortBy = safe(req.getParameter("sortBy"));
            if (sortBy.isEmpty()) {
                sortBy = "postingDate";
            }

            Set<String> favoriteIds = favoriteService.getFavoriteJobIds(user.id);
            List<Job> jobs = JobListFilters.apply(jobService.listPublishedJobs(), favoriteIds, q, sortBy);
            List<String> recentJobIds = recentlyViewedService.getRecentJobIds(user.id);
            jobs = JobListFilters.promoteRecentlyViewed(jobs, recentJobIds);

            req.setAttribute("jobs", jobs);
            req.setAttribute("recentViewedJobIds", new HashSet<>(recentJobIds));
            req.setAttribute("q", q);
            req.setAttribute("sortBy", sortBy);
            attachAiState(req, user);

            req.getRequestDispatcher("/WEB-INF/jsp/jobs.jsp")
                    .forward(req, resp);

        } else {
            // ── DETAIL PAGE ──
            Job job = jobService.getJobById(id);
            if (job != null && job.getId() != null) {
                recentlyViewedService.recordView(user.id, job.getId());
            }
            req.setAttribute("job", job);
            if (job != null && job.getId() != null) {
                req.setAttribute("jobFavorited", favoriteService.isFavorite(user.id, job.getId()));
            } else {
                req.setAttribute("jobFavorited", Boolean.FALSE);
            }

            attachJobApplicationContext(req, user, job);

            req.getRequestDispatcher("/WEB-INF/jsp/job-detail.jsp")
                    .forward(req, resp);
        }
    }

    /**
     * Toggles favorite status for a job ({@code action=toggleFavorite}).
     *
     * @param req  the incoming request; expects {@code action}, {@code jobId}, optional
     *             {@code returnTo}, {@code q}, {@code sortBy}
     * @param resp the response; redirects back to list or detail; returns 405 for unknown actions
     * @throws IOException if favorite persistence fails
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ctx = req.getContextPath();
        if (!ensureTa(req, resp)) {
            return;
        }
        User user = currentUser(req);

        if (!"toggleFavorite".equals(req.getParameter("action"))) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        String jobId = safe(req.getParameter("jobId"));
        if (jobId.isEmpty()) {
            resp.sendRedirect(ctx + "/job");
            return;
        }

        try {
            favoriteService.toggleFavorite(user.id, jobId);
        } catch (IOException e) {
            resp.sendRedirect(ctx + "/job?id=" + urlEncode(jobId) + "&err=" + urlEncode("Could not update favorites."));
            return;
        }

        String returnTo = safe(req.getParameter("returnTo"));
        if ("list".equals(returnTo)) {
            String q = safe(req.getParameter("q"));
            String sortBy = safe(req.getParameter("sortBy"));
            if (sortBy.isEmpty()) {
                sortBy = "postingDate";
            }
            StringBuilder url = new StringBuilder(ctx).append("/job?sortBy=").append(urlEncode(sortBy));
            if (!q.isEmpty()) {
                url.append("&q=").append(urlEncode(q));
            }
            resp.sendRedirect(url.toString());
            return;
        }

        resp.sendRedirect(ctx + "/job?id=" + urlEncode(jobId));
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private void attachJobApplicationContext(HttpServletRequest req, User user, Job job) {
        if (job == null) {
            return;
        }
        try {
            List<Application> allApps = applicationService.listAll();
            JobApplicationStats stats = JobApplicationStats.forJob(allApps, job.getModuleName(), job.getModuleCode());
            int cap = JobApplicationStats.parseCapacity(job.getNumberOfTAs());
            req.setAttribute("jobAppStats", stats);
            req.setAttribute("jobTaCapacity", cap);
            req.setAttribute("jobSlotsFull", stats.accepted >= cap);

            List<Application> mine = applicationService.getByUserId(user.id);
            boolean activeApp = mine.stream().anyMatch(a ->
                    JobApplicationStats.matchesJob(a, job.getModuleName(), job.getModuleCode()) &&
                            !"Withdrawn".equalsIgnoreCase(a.status) &&
                            !"Rejected".equalsIgnoreCase(a.status));
            req.setAttribute("userActiveApplicationForJob", activeApp);

            Optional<ApplicantProfile> prof = profileService.getByUserId(user.id);
            boolean profileComplete = prof.isPresent() && ProfileService.isApplicantProfileComplete(prof.get());
            req.setAttribute("taProfileComplete", profileComplete);
            req.setAttribute("jobAppStatsError", Boolean.FALSE);
        } catch (IOException e) {
            req.setAttribute("jobAppStats", JobApplicationStats.forJob(Collections.emptyList(), "", ""));
            req.setAttribute("jobTaCapacity", 1);
            req.setAttribute("jobSlotsFull", Boolean.FALSE);
            req.setAttribute("userActiveApplicationForJob", Boolean.FALSE);
            req.setAttribute("taProfileComplete", Boolean.FALSE);
            req.setAttribute("jobAppStatsError", Boolean.TRUE);
        }
    }

    private void attachAiState(HttpServletRequest req, User user) throws IOException {
        boolean aiEnabled = LmConfig.load(getServletContext()).isEnabled();
        Optional<ApplicantProfile> profileOpt = profileService.getByUserId(user.id);
        boolean profileExists = profileOpt.isPresent();
        boolean profileComplete = profileExists && ProfileService.isApplicantProfileComplete(profileOpt.get());
        boolean profileHasSkills = profileExists && ProfileService.hasAiMatchingInput(profileOpt.get());

        req.setAttribute("aiEnabled", aiEnabled);
        req.setAttribute("aiProfileExists", profileExists);
        req.setAttribute("aiProfileComplete", profileComplete);
        req.setAttribute("aiProfileHasSkills", profileHasSkills);
    }
}
