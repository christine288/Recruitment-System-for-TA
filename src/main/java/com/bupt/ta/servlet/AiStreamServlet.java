package com.bupt.ta.servlet;

import com.bupt.ta.ai.LmClient;
import com.bupt.ta.ai.LmClientFactory;
import com.bupt.ta.ai.LmConfig;
import com.bupt.ta.ai.LmException;
import com.bupt.ta.ai.LmRequest;
import com.bupt.ta.ai.LmStreamListener;
import com.bupt.ta.model.ApplicantProfile;
import com.bupt.ta.model.Job;
import com.bupt.ta.model.MoProcessedReviewContext;
import com.bupt.ta.model.MoWorkloadSnapshot;
import com.bupt.ta.model.Roles;
import com.bupt.ta.model.User;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.service.ApplicationService;
import com.bupt.ta.service.AuthService;
import com.bupt.ta.service.FavoriteService;
import com.bupt.ta.service.JobService;
import com.bupt.ta.service.ProfileService;
import com.bupt.ta.service.WorkloadService;
import com.bupt.ta.util.JobListFilters;
import com.bupt.ta.service.ai.MissingSkillService;
import com.bupt.ta.service.ai.RecommendationService;
import com.bupt.ta.service.ai.SkillMatchService;
import com.bupt.ta.service.ai.AdminAnalyticsService;
import com.bupt.ta.service.ai.ProcessedDecisionReviewService;
import com.bupt.ta.service.ai.WorkloadAdviceService;
import com.bupt.ta.service.admin.AdminDashboardMetrics;
import com.bupt.ta.service.admin.AdminMetricsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Server-Sent Events (SSE) endpoint streaming AI completion deltas for TA and MO features.
 *
 * <p><b>URL pattern:</b> {@code /api/ai/stream} (mapped in {@code web.xml})
 *
 * <p><b>Role access:</b>
 * <ul>
 *   <li>Authenticated TA — {@code recommendation}, {@code skillMatch}, {@code missingSkills}</li>
 *   <li>Authenticated MO — {@code moWorkloadAdvice}, {@code moDecisionReview} (require {@code applicationId})</li>
 *   <li>Authenticated Admin — {@code adminAnalytics}</li>
 * </ul>
 * Unauthenticated callers receive 401; non-MO callers requesting MO features receive 403.
 *
 * <p>Protocol: {@code data: {"type":"meta"|"delta"|"done"|"error", "b64": "..."}} with UTF-8
 * text encoded in Base64. Only GET is supported.
 */
public class AiStreamServlet extends BaseServlet {

    private JobService jobService;
    private FavoriteService favoriteService;
    private ProfileService profileService;
    private RecommendationService recommendationService;
    private SkillMatchService skillMatchService;
    private MissingSkillService missingSkillService;
    private WorkloadAdviceService workloadAdviceService;
    private ProcessedDecisionReviewService processedDecisionReviewService;
    private AdminAnalyticsService adminAnalyticsService;
    private ApplicationService applicationService;
    private AuthService authService;
    private WorkloadService workloadService;

    /**
     * Initializes job, profile, application, auth, and AI advice services from {@code WEB-INF/data}.
     *
     * @throws ServletException if servlet initialization fails
     */
    @Override
    public void init() throws ServletException {
        ServiceFactory f = (ServiceFactory) getServletContext().getAttribute(ServiceFactory.SERVLET_CONTEXT_KEY);
        this.jobService = f.getJobService();
        this.favoriteService = f.getFavoriteService();
        this.profileService = f.getProfileService();
        this.applicationService = f.getApplicationService();
        this.authService = f.getAuthService();
        this.workloadService = f.getWorkloadService();

        LmConfig lmConfig = LmConfig.load(getServletContext());
        LmClient client = LmClientFactory.create(lmConfig);
        this.recommendationService = new RecommendationService(client, lmConfig);
        this.skillMatchService = new SkillMatchService(client, lmConfig);
        this.missingSkillService = new MissingSkillService(client, lmConfig);
        this.workloadAdviceService = new WorkloadAdviceService(client, lmConfig);
        this.processedDecisionReviewService = new ProcessedDecisionReviewService(client, lmConfig);
        this.adminAnalyticsService = new AdminAnalyticsService(client, lmConfig);
    }

    /**
     * Opens an SSE stream for the requested AI feature.
     *
     * @param req  the incoming request; {@code feature} selects the stream type; additional
     *             query params vary by feature (e.g. {@code jobId}, {@code applicationId}, {@code q})
     * @param resp the response; content type {@code text/event-stream}; 401/403 on auth failure
     * @throws IOException if streaming or service calls fail
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = currentUser(req);
        if (user == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        LmConfig lmConfig = LmConfig.load(getServletContext());
        if (!lmConfig.isEnabled()) {
            beginSse(resp);
            writeError(resp.getWriter(), "AI features are disabled (LM_ENABLED=false).");
            return;
        }

        LmClient client = LmClientFactory.create(lmConfig);
        String feature = safe(req.getParameter("feature"));

        if ("adminAnalytics".equals(feature)) {
            if (!ensureAdminForStream(req, resp)) {
                return;
            }
            beginSse(resp);
            PrintWriter out = resp.getWriter();
            try {
                streamAdminAnalytics(req, client, out);
            } catch (LmException e) {
                writeError(out, e.getMessage());
            } catch (Exception e) {
                String msg = e.getMessage();
                writeError(out, msg != null && !msg.isEmpty() ? msg : "AI stream failed");
            }
            return;
        }

        if ("moWorkloadAdvice".equals(feature)) {
            if (!ensureMoForStream(req, resp)) {
                return;
            }
            beginSse(resp);
            PrintWriter out = resp.getWriter();
            try {
                streamMoWorkloadAdvice(req, client, out);
            } catch (LmException e) {
                writeError(out, e.getMessage());
            } catch (Exception e) {
                String msg = e.getMessage();
                writeError(out, msg != null && !msg.isEmpty() ? msg : "AI stream failed");
            }
            return;
        }

        if ("moDecisionReview".equals(feature)) {
            if (!ensureMoForStream(req, resp)) {
                return;
            }
            beginSse(resp);
            PrintWriter out = resp.getWriter();
            try {
                streamMoDecisionReview(req, client, out);
            } catch (LmException e) {
                writeError(out, e.getMessage());
            } catch (Exception e) {
                String msg = e.getMessage();
                writeError(out, msg != null && !msg.isEmpty() ? msg : "AI stream failed");
            }
            return;
        }

        if (!ensureTaForStream(req, resp)) {
            return;
        }

        beginSse(resp);
        PrintWriter out = resp.getWriter();

        try {
            switch (feature) {
                case "recommendation":
                    streamRecommendation(req, client, out);
                    break;
                case "skillMatch":
                    streamSkillMatch(req, client, out);
                    break;
                case "missingSkills":
                    streamMissingSkills(req, client, out);
                    break;
                default:
                    writeError(out, "Unknown feature. Use recommendation, skillMatch, missingSkills, moWorkloadAdvice, moDecisionReview, or adminAnalytics.");
            }
        } catch (LmException e) {
            writeError(out, e.getMessage());
        } catch (Exception e) {
            String msg = e.getMessage();
            writeError(out, msg != null && !msg.isEmpty() ? msg : "AI stream failed");
        }
    }

    private static void beginSse(HttpServletResponse resp) {
        resp.setContentType("text/event-stream;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache, no-store");
        resp.setHeader("Connection", "keep-alive");
        resp.setHeader("X-Accel-Buffering", "no");
    }

    private void streamRecommendation(HttpServletRequest req, LmClient client, PrintWriter out)
            throws IOException, LmException {
        User user = currentUser(req);
        String q = safe(req.getParameter("q"));
        String sortBy = safe(req.getParameter("sortBy"));
        if (sortBy.isEmpty()) {
            sortBy = "postingDate";
        }
        Set<String> favoriteIds = favoriteService.getFavoriteJobIds(user.id);
        List<Job> jobs = JobListFilters.apply(jobService.listPublishedJobs(), favoriteIds, q, sortBy);

        Optional<ApplicantProfile> profileOpt = profileService.getByUserId(user.id);
        if (!profileOpt.isPresent() || !ProfileService.hasAiMatchingInput(profileOpt.get())) {
            writeError(out, "Please add skills or completed courses in your profile first to get AI recommendations.");
            return;
        }
        String candidateInfo = ProfileService.buildAiCapabilityText(profileOpt.get());
        String positionsInfo = buildPositionsInfo(jobs);
        LmRequest lmReq = recommendationService.buildRecommendRequest(candidateInfo, positionsInfo);
        writeMeta(out, lmReq.getModel());
        client.stream(lmReq, sseListener(out));
    }

    private void streamSkillMatch(HttpServletRequest req, LmClient client, PrintWriter out)
            throws IOException, LmException {
        User user = currentUser(req);
        String jobId = safe(req.getParameter("jobId"));
        Job job = jobService.getJobById(jobId);
        if (job == null) {
            writeError(out, "Job not found.");
            return;
        }
        Optional<ApplicantProfile> profileOpt = profileService.getByUserId(user.id);
        if (!profileOpt.isPresent() || !ProfileService.hasAiMatchingInput(profileOpt.get())) {
            writeError(out, "Please add skills or completed courses in your profile first to get AI skill analysis.");
            return;
        }
        ApplicantProfile profile = profileOpt.get();
        String userSkills = ProfileService.buildAiCapabilityText(profile);
        String jobSkills = job.getRequiredSkills() != null
                ? String.join(", ", job.getRequiredSkills()) : "";
        LmRequest lmReq = skillMatchService.buildMatchRequest(userSkills, jobSkills);
        writeMeta(out, lmReq.getModel());
        client.stream(lmReq, sseListener(out));
    }

    private void streamMissingSkills(HttpServletRequest req, LmClient client, PrintWriter out)
            throws IOException, LmException {
        User user = currentUser(req);
        String jobId = safe(req.getParameter("jobId"));
        Job job = jobService.getJobById(jobId);
        if (job == null) {
            writeError(out, "Job not found.");
            return;
        }
        Optional<ApplicantProfile> profileOpt = profileService.getByUserId(user.id);
        if (!profileOpt.isPresent() || !ProfileService.hasAiMatchingInput(profileOpt.get())) {
            writeError(out, "Please add skills or completed courses in your profile first to get AI skill analysis.");
            return;
        }
        ApplicantProfile profile = profileOpt.get();
        String userSkills = ProfileService.buildAiCapabilityText(profile);
        String jobSkills = job.getRequiredSkills() != null
                ? String.join(", ", job.getRequiredSkills()) : "";
        LmRequest lmReq = missingSkillService.buildMissingRequest(userSkills, jobSkills);
        writeMeta(out, lmReq.getModel());
        client.stream(lmReq, sseListener(out));
    }

    private void streamMoWorkloadAdvice(HttpServletRequest req, LmClient client, PrintWriter out)
            throws IOException, LmException {
        String applicationId = safe(req.getParameter("applicationId"));
        if (applicationId.isEmpty()) {
            writeError(out, "Missing applicationId.");
            return;
        }

        List<com.bupt.ta.model.Application> applications = applicationService.listAll();
        List<Job> allJobs = jobService.getAllJobs();
        String applicantName = resolveApplicantName(applicationId, applications);
        MoWorkloadSnapshot snapshot = workloadService.buildSnapshotForApplication(
                applicationId, applications, allJobs, applicantName);
        if (snapshot == null) {
            writeError(out, "Application not found or not pending.");
            return;
        }

        LmRequest lmReq = workloadAdviceService.buildAdviceRequest(snapshot);
        writeMeta(out, lmReq.getModel());
        client.stream(lmReq, sseListener(out));
    }

    private void streamAdminAnalytics(HttpServletRequest req, LmClient client, PrintWriter out)
            throws IOException, LmException {
        List<User> users = authService.listAllUsers();
        List<com.bupt.ta.model.Application> appList = applicationService.listAll();
        List<Job> allJobs = jobService.getAllJobs();
        Map<String, com.bupt.ta.model.TaWorkloadStats> taWorkload =
                workloadService.buildTaWorkloadStats(users, appList);
        AdminDashboardMetrics metrics = AdminMetricsBuilder.build(users, appList, allJobs, taWorkload);
        LmRequest lmReq = adminAnalyticsService.buildRequest(metrics);
        writeMeta(out, lmReq.getModel());
        client.stream(lmReq, sseListener(out));
    }

    private boolean ensureAdminForStream(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = currentUser(req);
        if (u == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        if (!Roles.ADMIN.equals(u.role)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

    private void streamMoDecisionReview(HttpServletRequest req, LmClient client, PrintWriter out)
            throws IOException, LmException {
        String applicationId = safe(req.getParameter("applicationId"));
        if (applicationId.isEmpty()) {
            writeError(out, "Missing applicationId.");
            return;
        }

        com.bupt.ta.model.Application target = findApplication(applicationId, applicationService.listAll());
        if (target == null) {
            writeError(out, "Application not found.");
            return;
        }

        String status = target.status == null ? "" : target.status.trim();
        if ("Pending".equalsIgnoreCase(status)) {
            writeError(out, "Application is still pending. Use moWorkloadAdvice instead.");
            return;
        }
        if (!isProcessedStatus(status)) {
            writeError(out, "Application is not in a processed state.");
            return;
        }

        Job job = findJobForApplication(target, jobService.getAllJobs());
        String applicantName = resolveApplicantName(applicationId, applicationService.listAll());
        Optional<ApplicantProfile> profileOpt = target.userId != null
                ? profileService.getByUserId(target.userId.trim())
                : Optional.empty();

        MoProcessedReviewContext context = ProcessedDecisionReviewService.buildContext(
                target,
                job,
                profileOpt.orElse(null),
                applicantName);

        LmRequest lmReq = processedDecisionReviewService.buildReviewRequest(context);
        writeMeta(out, lmReq.getModel());
        client.stream(lmReq, sseListener(out));
    }

    private static com.bupt.ta.model.Application findApplication(
            String applicationId, List<com.bupt.ta.model.Application> applications) {
        if (applicationId == null || applications == null) {
            return null;
        }
        for (com.bupt.ta.model.Application app : applications) {
            if (app != null && applicationId.equals(app.id)) {
                return app;
            }
        }
        return null;
    }

    private static boolean isProcessedStatus(String status) {
        return "Accepted".equalsIgnoreCase(status)
                || "Rejected".equalsIgnoreCase(status)
                || "Withdrawn".equalsIgnoreCase(status);
    }

    private static Job findJobForApplication(com.bupt.ta.model.Application app, List<Job> jobs) {
        if (app == null || jobs == null || app.moduleCode == null) {
            return null;
        }
        String code = app.moduleCode.trim().toUpperCase();
        for (Job job : jobs) {
            if (job != null && job.getModuleCode() != null
                    && code.equals(job.getModuleCode().trim().toUpperCase())) {
                return job;
            }
        }
        return null;
    }

    private String resolveApplicantName(String applicationId,
                                        List<com.bupt.ta.model.Application> applications)
            throws IOException {
        com.bupt.ta.model.Application target = null;
        for (com.bupt.ta.model.Application app : applications) {
            if (app != null && applicationId.equals(app.id)) {
                target = app;
                break;
            }
        }
        if (target == null || target.userId == null) {
            return "";
        }
        String userId = target.userId.trim();
        Optional<User> userOpt = authService.findById(userId);
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            if (u.name != null && !u.name.trim().isEmpty()) {
                return u.name.trim();
            }
            return u.id != null ? u.id : userId;
        }
        Optional<ApplicantProfile> profileOpt = profileService.getByUserId(userId);
        if (profileOpt.isPresent()) {
            ApplicantProfile p = profileOpt.get();
            if (p.fullName != null && !p.fullName.trim().isEmpty()) {
                return p.fullName.trim();
            }
        }
        return userId;
    }

    /** MO-only check for SSE; returns false if response already committed. */
    private boolean ensureMoForStream(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = currentUser(req);
        if (u == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        if (!Roles.MO.equals(u.role)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

    /** TA-only check for SSE; returns false if response already committed. */
    private boolean ensureTaForStream(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = currentUser(req);
        if (u == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        if (!Roles.TA.equals(u.role)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

    private static LmStreamListener sseListener(PrintWriter out) {
        return new LmStreamListener() {
            @Override
            public void onDelta(String text) {
                writeDelta(out, text);
                out.flush();
            }

            @Override
            public void onComplete(String model) {
                writeDone(out);
                out.flush();
            }

            @Override
            public void onError(String message) {
                writeError(out, message);
                out.flush();
            }
        };
    }

    private static void writeMeta(PrintWriter out, String model) {
        String b64 = Base64.getEncoder().encodeToString(
                (model != null ? model : "").getBytes(StandardCharsets.UTF_8));
        out.write("data: {\"type\":\"meta\",\"b64\":\"" + b64 + "\"}\n\n");
        out.flush();
    }

    private static void writeDelta(PrintWriter out, String delta) {
        String b64 = Base64.getEncoder().encodeToString(delta.getBytes(StandardCharsets.UTF_8));
        out.write("data: {\"type\":\"delta\",\"b64\":\"" + b64 + "\"}\n\n");
    }

    private static void writeDone(PrintWriter out) {
        out.write("data: {\"type\":\"done\"}\n\n");
    }

    private static void writeError(PrintWriter out, String message) {
        String b64 = Base64.getEncoder().encodeToString(
                (message != null ? message : "").getBytes(StandardCharsets.UTF_8));
        out.write("data: {\"type\":\"error\",\"b64\":\"" + b64 + "\"}\n\n");
        out.flush();
    }

    private String buildPositionsInfo(List<Job> jobs) {
        StringBuilder sb = new StringBuilder();
        for (Job j : jobs) {
            sb.append("- ").append(j.getModuleCode()).append(": ").append(j.getModuleName());
            if (j.getActivityType() != null) sb.append(" (").append(j.getActivityType()).append(")");
            if (j.getRequiredSkills() != null) {
                sb.append(" | Skills: ").append(String.join(", ", j.getRequiredSkills()));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

}
