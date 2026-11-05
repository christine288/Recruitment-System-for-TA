package com.bupt.ta.servlet;

import com.bupt.ta.model.Application;
import com.bupt.ta.model.User;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.service.ApplicationService;
import com.bupt.ta.service.AuthService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Admin CSV export of users or applications.
 *
 * <p><b>URL pattern:</b> {@code /admin/export}
 *
 * <p><b>Role access:</b> {@link com.bupt.ta.model.Roles#ADMIN} only via {@link #ensureAdmin}.
 *
 * <p>GET {@code ?type=users} (default) or {@code ?type=applications} downloads a UTF-8 CSV with BOM.
 */
@WebServlet(urlPatterns = {"/admin/export"})
public class AdminExportServlet extends BaseServlet {

    private AuthService auth;
    private ApplicationService applications;

    /**
     * Initializes auth and application services from {@code WEB-INF/data}.
     */
    @Override
    public void init() {
        ServiceFactory f = (ServiceFactory) getServletContext().getAttribute(ServiceFactory.SERVLET_CONTEXT_KEY);
        auth = f.getAuthService();
        applications = f.getApplicationService();
    }

    /**
     * Writes a CSV attachment for users or applications.
     *
     * @param req  the incoming request; optional {@code type} ({@code users} or {@code applications})
     * @param resp the response; sets CSV headers and body; 403 when not admin
     * @throws ServletException if dispatch fails
     * @throws IOException      if export writing fails
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!ensureAdmin(req, resp)) {
            return;
        }

        String type = req.getParameter("type");
        if (type == null || type.isEmpty()) {
            type = "users";
        }

        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("text/csv; charset=UTF-8");
        if ("applications".equalsIgnoreCase(type)) {
            resp.setHeader("Content-Disposition", "attachment; filename=\"applications.csv\"");
            exportApplications(resp);
        } else {
            resp.setHeader("Content-Disposition", "attachment; filename=\"users.csv\"");
            exportUsers(resp);
        }
    }

    private void exportUsers(HttpServletResponse resp) throws IOException {
        List<User> rows = auth.listAllUsers();
        try (PrintWriter w = resp.getWriter()) {
            w.write('\uFEFF');
            w.println("id,name,studentId,email,role,active");
            for (User u : rows) {
                w.println(String.join(",",
                        csv(u.id),
                        csv(u.name),
                        csv(u.studentId),
                        csv(u.email),
                        csv(u.role),
                        u.active ? "true" : "false"));
            }
        }
    }

    private void exportApplications(HttpServletResponse resp) throws IOException {
        List<Application> rows = applications.listAll();
        try (PrintWriter w = resp.getWriter()) {
            w.write('\uFEFF');
            w.println("id,userId,moduleName,moduleCode,role,applicationDate,status,feedback");
            for (Application a : rows) {
                w.println(String.join(",",
                        csv(a.id),
                        csv(a.userId),
                        csv(a.moduleName),
                        csv(a.moduleCode),
                        csv(a.role),
                        csv(a.applicationDate),
                        csv(a.status),
                        csv(a.feedback)));
            }
        }
    }

    /** CSV field: quote if needed, escape internal quotes. */
    private static String csv(String s) {
        if (s == null) {
            return "\"\"";
        }
        String t = s.replace("\"", "\"\"");
        if (t.contains(",") || t.contains("\"") || t.contains("\n") || t.contains("\r")) {
            return "\"" + t + "\"";
        }
        return t;
    }
}
