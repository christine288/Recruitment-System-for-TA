package com.bupt.ta.servlet;



import java.io.IOException;

import java.time.LocalDate;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.List;

import java.util.Map;

import java.util.Optional;

import java.util.stream.Collectors;



import javax.servlet.ServletException;

import javax.servlet.http.HttpServletRequest;

import javax.servlet.http.HttpServletResponse;



import com.bupt.ta.ai.LmConfig;

import com.bupt.ta.model.Application;

import com.bupt.ta.model.Job;

import com.bupt.ta.model.JobApplicationStats;

import com.bupt.ta.model.MoWorkloadSnapshot;

import com.bupt.ta.model.User;

import com.bupt.ta.persistence.ServiceFactory;

import com.bupt.ta.service.ApplicationService;

import com.bupt.ta.service.AuditService;

import com.bupt.ta.service.AuthService;

import com.bupt.ta.service.JobService;

import com.bupt.ta.service.NotificationService;

import com.bupt.ta.service.ProfileService;

import com.bupt.ta.service.WorkloadService;



/**

 * Module owner (MO) dashboard and job/application management.

 *

 * <p><b>URL pattern:</b> {@code /mo} (mapped in {@code web.xml})

 *

 * <p><b>Role access:</b> {@link com.bupt.ta.model.Roles#MO} only via {@link #ensureMo}.

 *

 * <p>GET renders the MO dashboard with jobs, applications, workload snapshots, and CV links.

 * POST handles job creation, publishing, editing, closing, and application approve/reject actions.

 */

public class MoServlet extends BaseServlet {

    private ApplicationService applications;

    private JobService jobs;

    private AuthService auth;

    private WorkloadService workloadService;

    private ProfileService profiles;

    private NotificationService notifications;

    private AuditService audit;



    @Override

    public void init() {

        ServiceFactory f = (ServiceFactory) getServletContext().getAttribute(ServiceFactory.SERVLET_CONTEXT_KEY);

        applications = f.getApplicationService();

        auth = f.getAuthService();

        jobs = f.getJobService();

        profiles = f.getProfileService();

        workloadService = f.getWorkloadService();

        notifications = f.getNotificationService();

        audit = f.getAuditService();

    }



    @Override

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)

            throws ServletException, IOException {

        if (!ensureMo(req, resp)) {

            return;

        }



        User mo = currentUser(req);

        String moId = mo == null ? "" : n(mo.id);



        String msg = req.getParameter("msg");

        if (msg != null && !msg.trim().isEmpty()) {

            req.setAttribute("moMessage", msg.trim());

        }



        List<Job> moJobs = jobs.getJobsByMoId(moId);

        req.setAttribute("jobs", moJobs);



        List<Application> allApps = applications.listAll();

        List<Application> moApps = filterApplicationsForMo(allApps, moJobs);

        req.setAttribute("applications", moApps);



        Map<String, String> applicantNamesByUserId = new HashMap<>();

        for (Application a : moApps) {

            if (a == null || a.userId == null || applicantNamesByUserId.containsKey(a.userId)) {

                continue;

            }

            String uid = a.userId.trim();

            String name = auth.findById(uid).map(u -> u.name).orElse(null);

            if (name == null || name.trim().isEmpty()) {

                name = profiles.getByUserId(uid).map(p -> p.fullName).orElse(uid);

            }

            applicantNamesByUserId.put(uid, name);

        }

        Map<String, MoWorkloadSnapshot> workloadSnapshots =

                workloadService.buildSnapshotsForPendingApplications(moApps, moJobs, applicantNamesByUserId);

        req.setAttribute("workloadSnapshots", workloadSnapshots);

        req.setAttribute("applicantNamesByUserId", applicantNamesByUserId);

        req.setAttribute("aiEnabled", LmConfig.load(getServletContext()).isEnabled());



        Map<String, String> cvByUserId = new HashMap<>();

        for (Application a : moApps) {

            if (a == null || a.userId == null || cvByUserId.containsKey(a.userId)) {

                continue;

            }

            String fn = profiles.getByUserId(a.userId)

                    .map(pr -> pr.cvFileName)

                    .filter(s -> s != null && !s.trim().isEmpty())

                    .orElse(null);

            cvByUserId.put(a.userId, fn);

        }

        req.setAttribute("cvByUserId", cvByUserId);



        req.getRequestDispatcher("/WEB-INF/jsp/mo/dashboard.jsp").forward(req, resp);

    }



    @Override

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)

            throws ServletException, IOException {

        if (!ensureMo(req, resp)) {

            return;

        }

        req.setCharacterEncoding("UTF-8");



        String action = n(req.getParameter("action"));

        String ctx = req.getContextPath();

        User mo = currentUser(req);
        String moId = mo == null ? "" : n(mo.id);

        if ("createJob".equals(action)) {

            String moduleName = n(req.getParameter("moduleName"));

            String moduleCode = n(req.getParameter("moduleCode"));

            String activityType = n(req.getParameter("activityType"));

            String description = n(req.getParameter("description"));

            String deadline = n(req.getParameter("applicationDeadline"));

            String numberOfTAs = n(req.getParameter("numberOfTAs"));

            String workload = n(req.getParameter("workloadHours"));

            String duration = n(req.getParameter("duration"));

            boolean publishNow = "1".equals(req.getParameter("publishNow"));



            if (moduleName.isEmpty() || moduleCode.isEmpty() || description.isEmpty()) {

                resp.sendRedirect(ctx + "/mo?msg=" + urlEncode("Module name, code and description are required."));

                return;

            }



            Job j = new Job();

            String today = LocalDate.now().toString();

            j.setModuleName(moduleName);

            j.setModuleCode(moduleCode);

            j.setActivityType(activityType.isEmpty() ? "Teaching Assistant" : activityType);

            j.setDescription(description);

            j.setApplicationDeadline(deadline);

            j.setNumberOfTAs(numberOfTAs.isEmpty() ? "2" : numberOfTAs);

            j.setDuration(duration.isEmpty() ? "One semester" : duration);

            j.setWorkloadHours(workload);

            j.setRequiredSkills(splitSkills(req.getParameter("requiredSkills")));

            j.setCreatedByMoId(moId);

            j.setCreatedAt(today);



            if (publishNow) {

                j.setStatus("Published");

                j.setPostDate(today);

                j.setPublishedAt(today);

            } else {

                j.setStatus("Draft");

                j.setPostDate("");

                j.setPublishedAt("");

            }



            jobs.createJob(j);

            resp.sendRedirect(ctx + "/mo?msg=" + urlEncode(publishNow ? "Job published." : "Draft saved."));

            return;

        }



        if ("publishJob".equals(action)) {

            String jobId = n(req.getParameter("jobId"));

            if (jobId.isEmpty()) {

                resp.sendRedirect(ctx + "/mo?msg=" + urlEncode("Missing job id."));

                return;

            }

            if (!jobs.isOwnedByMo(jobId, moId)) {

                resp.sendRedirect(ctx + "/mo?msg=" + urlEncode("You can only publish your own jobs."));

                return;

            }

            boolean ok = jobs.publishJob(jobId, moId);

            resp.sendRedirect(ctx + "/mo?msg=" + urlEncode(ok ? "Job published successfully." : "Job not found."));

            return;

        }



        if ("editJob".equals(action)) {

            String jobId = n(req.getParameter("jobId"));

            if (jobId.isEmpty()) {

                resp.sendRedirect(ctx + "/mo?msg=" + urlEncode("Missing job id."));

                return;

            }

            boolean ok = jobs.updateJobFields(

                    jobId,

                    moId,

                    n(req.getParameter("moduleName")),

                    n(req.getParameter("moduleCode")),

                    n(req.getParameter("description")),

                    req.getParameter("applicationDeadline"),

                    splitSkills(req.getParameter("requiredSkills")));

            resp.sendRedirect(ctx + "/mo?msg=" + urlEncode(ok ? "Job updated." : "Job not found or cannot be edited."));

            return;

        }



        if ("closeJob".equals(action)) {

            String jobId = n(req.getParameter("jobId"));

            if (jobId.isEmpty()) {

                resp.sendRedirect(ctx + "/mo?msg=" + urlEncode("Missing job id."));

                return;

            }

            boolean ok = jobs.closeJob(jobId, moId);

            resp.sendRedirect(ctx + "/mo?msg=" + urlEncode(ok ? "Job closed." : "Job not found or access denied."));

            return;

        }



        if ("approveApp".equals(action) || "rejectApp".equals(action)) {

            String appId = n(req.getParameter("appId"));

            String feedback = n(req.getParameter("feedback"));

            if (appId.isEmpty()) {

                resp.sendRedirect(ctx + "/mo?msg=" + urlEncode("Missing application id."));

                return;

            }



            Optional<Application> appOpt = applications.findById(appId);

            if (!appOpt.isPresent()) {

                resp.sendRedirect(ctx + "/mo?msg=" + urlEncode("Application not found."));

                return;

            }

            Application app = appOpt.get();

            List<Job> moJobs = jobs.getJobsByMoId(moId);

            if (!applicationBelongsToMo(app, moJobs)) {

                resp.sendRedirect(ctx + "/mo?msg=" + urlEncode("You can only review applications for your own jobs."));

                return;

            }



            String newStatus = "approveApp".equals(action) ? "Accepted" : "Rejected";

            applications.updateStatus(appId, newStatus, feedback);



            String auditAction = "approveApp".equals(action) ? "APPROVE_APPLICATION" : "REJECT_APPLICATION";

            audit.log(moId, auditAction, "APPLICATION", appId, feedback);



            notifyApplicantStatusChange(app, newStatus, feedback);



            resp.sendRedirect(ctx + "/mo?msg=" + urlEncode("Application updated to " + newStatus + "."));

            return;

        }



        resp.sendRedirect(ctx + "/mo?msg=" + urlEncode("Unknown action."));

    }



    private void notifyApplicantStatusChange(Application app, String newStatus, String feedback)

            throws IOException {

        if (app == null || app.userId == null) {

            return;

        }

        String email = auth.findById(app.userId).map(u -> u.email).orElse("");

        String name = profiles.getByUserId(app.userId).map(p -> p.fullName).orElse("");

        if (name == null || name.trim().isEmpty()) {

            name = auth.findById(app.userId).map(u -> u.name).orElse("Applicant");

        }

        String moduleLabel = app.moduleName + (app.moduleCode == null || app.moduleCode.isEmpty()

                ? "" : " (" + app.moduleCode + ")");

        notifications.sendStatusChangeEmail(email, name, moduleLabel, newStatus, feedback);

    }



    private static List<Application> filterApplicationsForMo(List<Application> all, List<Job> moJobs) {

        if (all == null || all.isEmpty() || moJobs == null || moJobs.isEmpty()) {

            return new ArrayList<>();

        }

        return all.stream()

                .filter(a -> applicationBelongsToMo(a, moJobs))

                .collect(Collectors.toList());

    }



    private static boolean applicationBelongsToMo(Application app, List<Job> moJobs) {

        if (app == null || moJobs == null) {

            return false;

        }

        for (Job j : moJobs) {

            if (j == null) {

                continue;

            }

            String jn = j.getModuleName() == null ? "" : j.getModuleName().trim();

            String jc = j.getModuleCode() == null ? "" : j.getModuleCode().trim();

            if (JobApplicationStats.matchesJob(app, jn, jc)) {

                return true;

            }

        }

        return false;

    }

    private static String n(String s) {

        return s == null ? "" : s.trim();

    }



    private static List<String> splitSkills(String raw) {

        List<String> out = new ArrayList<>();

        if (raw == null) return out;

        String[] arr = raw.split("[,\\n\\r]");

        for (String p : arr) {

            String t = p == null ? "" : p.trim();

            if (!t.isEmpty()) out.add(t);

        }

        return out;

    }

}

