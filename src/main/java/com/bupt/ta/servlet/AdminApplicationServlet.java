package com.bupt.ta.servlet;



import com.bupt.ta.model.Application;

import com.bupt.ta.model.User;

import com.bupt.ta.persistence.ServiceFactory;

import com.bupt.ta.service.ApplicationService;

import com.bupt.ta.service.AuditService;

import com.bupt.ta.service.AuthService;

import com.bupt.ta.service.NotificationService;

import com.bupt.ta.service.ProfileService;



import javax.servlet.ServletException;

import javax.servlet.annotation.WebServlet;

import javax.servlet.http.HttpServletRequest;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import java.util.Optional;



/**

 * Admin override for application status transitions (force accept, reject, or pending).

 *

 * <p><b>URL pattern:</b> {@code /admin/applications}

 */

@WebServlet(urlPatterns = {"/admin/applications"})

public class AdminApplicationServlet extends BaseServlet {



    private ApplicationService applications;

    private AuthService auth;

    private ProfileService profiles;

    private NotificationService notifications;

    private AuditService audit;



    @Override

    public void init() {

        ServiceFactory f = (ServiceFactory) getServletContext().getAttribute(ServiceFactory.SERVLET_CONTEXT_KEY);

        applications = f.getApplicationService();

        auth = f.getAuthService();

        profiles = f.getProfileService();

        notifications = f.getNotificationService();

        audit = f.getAuditService();

    }



    @Override

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        if (!ensureAdmin(req, resp)) {

            return;

        }

        req.setCharacterEncoding("UTF-8");

        String ctx = req.getContextPath();

        User admin = currentUser(req);

        String action = req.getParameter("action");

        String appId = req.getParameter("appId");

        String feedback = req.getParameter("feedback");

        String fb = feedback == null ? "" : feedback.trim();

        String returnStatus = normalizeReturnStatus(req.getParameter("returnStatus"));

        String listUrl = ctx + "/admin/applications/by-status?status=" + returnStatus;



        if (appId == null || appId.trim().isEmpty()) {

            resp.sendRedirect(listUrl + "&msg=" + urlEncode("Missing application id."));

            return;

        }

        appId = appId.trim();



        Optional<Application> opt = applications.findById(appId);

        if (!opt.isPresent()) {

            resp.sendRedirect(listUrl + "&msg=" + urlEncode("Application not found."));

            return;

        }

        Application app = opt.get();

        String raw = app.status == null ? "" : app.status.trim();

        boolean isAccepted = "Accepted".equalsIgnoreCase(raw);

        boolean isRejected = "Rejected".equalsIgnoreCase(raw);

        boolean isPending = !isAccepted && !isRejected;



        String newStatus = null;

        String auditAction = null;



        try {

            if ("forceAccept".equals(action)) {

                if (!isPending && !isRejected) {

                    resp.sendRedirect(listUrl + "&msg=" + urlEncode("Only pending or rejected applications can be accepted."));

                    return;

                }

                newStatus = "Accepted";

                auditAction = "FORCE_ACCEPT";

                applications.updateStatus(appId, newStatus, fb);

            } else if ("forceReject".equals(action)) {

                if (!isPending && !isAccepted) {

                    resp.sendRedirect(listUrl + "&msg=" + urlEncode("Only pending or accepted applications can be rejected."));

                    return;

                }

                newStatus = "Rejected";

                auditAction = "FORCE_REJECT";

                applications.updateStatus(appId, newStatus, fb);

            } else if ("forcePend".equals(action)) {

                if (!isAccepted && !isRejected) {

                    resp.sendRedirect(listUrl + "&msg=" + urlEncode("Only accepted or rejected applications can be moved back to pending."));

                    return;

                }

                newStatus = "Pending";

                auditAction = "FORCE_PEND";

                applications.updateStatus(appId, newStatus, fb);

            } else {

                resp.sendRedirect(listUrl + "&msg=" + urlEncode("Unknown action."));

                return;

            }



            audit.log(admin != null ? admin.id : "", auditAction, "APPLICATION", appId, fb);

            notifyApplicantStatusChange(app, newStatus, fb);

        } catch (IOException e) {

            resp.sendRedirect(listUrl + "&msg=" + urlEncode("Update failed: " + e.getMessage()));

            return;

        }

        resp.sendRedirect(listUrl + "&msg=" + urlEncode("Application status updated."));

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



    private static String normalizeReturnStatus(String raw) {

        if (raw == null) return "pending";

        String s = raw.trim().toLowerCase();

        if ("pending".equals(s) || "accepted".equals(s) || "rejected".equals(s)) {

            return s;

        }

        return "pending";

    }

}

