package com.bupt.ta.servlet;

import com.bupt.ta.model.ApplicantProfile;
import com.bupt.ta.model.Roles;
import com.bupt.ta.model.TaResumeDisplay;
import com.bupt.ta.model.User;
import com.bupt.ta.model.EducationEntry;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.service.AuthService;
import com.bupt.ta.service.ProfileService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Admin browse view of all TA applicant profiles and education summaries.
 *
 * <p><b>URL pattern:</b> {@code /admin/ta-profiles}
 *
 * <p><b>Role access:</b> {@link com.bupt.ta.model.Roles#ADMIN} only via {@link #ensureAdmin}.
 *
 * <p>Only GET is supported. Rows are sorted by display name.
 */
@WebServlet("/admin/ta-profiles")
public class AdminTaProfilesServlet extends BaseServlet {

    private AuthService authService;
    private ProfileService profileService;

    /**
     * Initializes auth and profile services from {@code WEB-INF/data}.
     */
    @Override
    public void init() {
        ServiceFactory f = (ServiceFactory) getServletContext().getAttribute(ServiceFactory.SERVLET_CONTEXT_KEY);
        this.authService = f.getAuthService();
        this.profileService = f.getProfileService();
    }

    /**
     * Builds TA resume display rows and forwards to the ta-profiles JSP.
     *
     * @param req  the incoming request
     * @param resp the response; 403 or redirect when not admin
     * @throws ServletException if the JSP forward fails
     * @throws IOException      if data loading fails
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!ensureAdmin(req, resp)) {
            return;
        }
        List<User> all = authService.listAllUsers();
        List<TaResumeDisplay> rows = new ArrayList<>();
        for (User u : all) {
            if (u == null || !Roles.TA.equals(u.role)) {
                continue;
            }
            Optional<ApplicantProfile> opt = profileService.getByUserId(u.id);
            ApplicantProfile p = opt.orElse(null);
            List<EducationEntry> edu = p != null
                    ? ProfileService.parseEducationJson(p.educationJson)
                    : new ArrayList<>();
            rows.add(new TaResumeDisplay(u, p, edu));
        }
        rows.sort(Comparator.comparing(AdminTaProfilesServlet::sortKey, String.CASE_INSENSITIVE_ORDER));
        req.setAttribute("taResumes", rows);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/ta-profiles.jsp").forward(req, resp);
    }

    private static String sortKey(TaResumeDisplay t) {
        if (t.profile.fullName != null && !t.profile.fullName.trim().isEmpty()) {
            return t.profile.fullName.trim();
        }
        if (t.user.name != null && !t.user.name.trim().isEmpty()) {
            return t.user.name.trim();
        }
        return t.user.email != null ? t.user.email : "";
    }
}
