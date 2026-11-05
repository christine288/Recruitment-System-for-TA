package com.bupt.ta.servlet;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bupt.ta.model.ApplicantProfile;
import com.bupt.ta.model.EducationEntry;
import com.bupt.ta.model.Roles;
import com.bupt.ta.model.User;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.service.AuthService;
import com.bupt.ta.service.ProfileService;
import com.bupt.ta.util.ApplicantFieldValidation;

/**
 * Read-only MO view of a TA applicant's full profile.
 *
 * <p><b>URL pattern:</b> {@code /mo/applicant-profile} (mapped in {@code web.xml})
 *
 * <p><b>Role access:</b> {@link com.bupt.ta.model.Roles#MO} only via {@link #ensureMo}.
 *
 * <p>Only GET is supported; requires {@code userId} for a TA with an existing profile.
 */
public class MoApplicantProfileServlet extends BaseServlet {
    private ProfileService profiles;
    private AuthService auth;

    /**
     * Initializes profile and auth services from {@code WEB-INF/data}.
     */
    @Override
    public void init() {
        ServiceFactory f = (ServiceFactory) getServletContext().getAttribute(ServiceFactory.SERVLET_CONTEXT_KEY);
        profiles = f.getProfileService();
        auth = f.getAuthService();
    }

    /**
     * Loads an applicant profile for MO review and forwards to the applicant-profile JSP.
     *
     * @param req  the incoming request; requires {@code userId}
     * @param resp the response; redirects to {@code /mo} with message on error; 403 when not MO
     * @throws ServletException if the JSP forward fails
     * @throws IOException      if redirect or data loading fails
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!ensureMo(req, resp)) {
            return;
        }

        String userId = trim(req.getParameter("userId"));
        if (userId.isEmpty()) {
            redirectBackWithMessage(req, resp, "Missing applicant reference.");
            return;
        }

        Optional<User> applicantOpt = auth.findById(userId);
        if (!applicantOpt.isPresent() || !Roles.TA.equals(applicantOpt.get().role)) {
            redirectBackWithMessage(req, resp, "Applicant not found.");
            return;
        }

        Optional<ApplicantProfile> profileOpt = profiles.getByUserId(userId);
        if (!profileOpt.isPresent()) {
            redirectBackWithMessage(req, resp, "Applicant profile not found.");
            return;
        }

        User applicant = applicantOpt.get();
        ApplicantProfile profile = profileOpt.get();
        mergeDefaultsFromUser(applicant, profile);
        mapLegacyDegreeForDisplay(profile);

        List<EducationEntry> educationList = ProfileService.parseEducationJson(profile.educationJson);
        req.setAttribute("applicantUser", applicant);
        req.setAttribute("profile", profile);
        req.setAttribute("educationList", educationList);
        req.getRequestDispatcher("/WEB-INF/jsp/mo/applicant-profile.jsp").forward(req, resp);
    }

    private void redirectBackWithMessage(HttpServletRequest req, HttpServletResponse resp, String message)
            throws IOException {
        resp.sendRedirect(req.getContextPath() + "/mo?msg=" + urlEncode(message));
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static void mergeDefaultsFromUser(User u, ApplicantProfile p) {
        if ((p.fullName == null || p.fullName.trim().isEmpty()) && u.name != null) {
            p.fullName = u.name;
        }
        if ((p.studentId == null || p.studentId.trim().isEmpty()) && u.studentId != null) {
            p.studentId = u.studentId;
        }
        if ((p.email == null || p.email.trim().isEmpty()) && u.email != null) {
            p.email = u.email;
        }
    }

    private static void mapLegacyDegreeForDisplay(ApplicantProfile p) {
        if (p == null || p.degree == null) {
            return;
        }
        String degree = p.degree.trim();
        if ("\u7855\u58eb\u7814\u7a76\u751f".equals(degree)) {
            p.degree = "Master";
        } else if ("\u535a\u58eb\u7814\u7a76\u751f".equals(degree)) {
            p.degree = "Doctoral";
        } else if ("\u672c\u79d1".equals(degree) || !ApplicantFieldValidation.isAllowedApplicantDegreeLevel(degree)) {
            p.degree = "";
        }
    }
}
