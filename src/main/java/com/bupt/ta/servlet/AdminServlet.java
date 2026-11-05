package com.bupt.ta.servlet;

import com.bupt.ta.ai.LmConfig;
import com.bupt.ta.model.Application;
import com.bupt.ta.model.Job;
import com.bupt.ta.model.Roles;
import com.bupt.ta.model.TaWorkloadStats;
import com.bupt.ta.model.User;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.service.ApplicationService;
import com.bupt.ta.service.AuthService;
import com.bupt.ta.service.JobService;
import com.bupt.ta.service.WorkloadService;
import com.bupt.ta.service.admin.AdminDashboardMetrics;
import com.bupt.ta.service.admin.AdminMetricsBuilder;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders the administrator dashboard with users, jobs, applications, and TA workload stats.
 *
 * <p><b>URL pattern:</b> {@code /admin}
 *
 * <p><b>Role access:</b> {@link com.bupt.ta.model.Roles#ADMIN} only via {@link #ensureAdmin}.
 *
 * <p>Only GET is supported; POST actions are handled by other admin servlets.
 */
@WebServlet(urlPatterns = {"/admin"})
public class AdminServlet extends BaseServlet {
    private AuthService auth;
    private ApplicationService applications;
    private JobService jobs;
    private WorkloadService workloadService;

    /**
     * Initializes auth, application, job, and workload services from {@code WEB-INF/data}.
     */
    @Override
    public void init() {
        ServiceFactory f = (ServiceFactory) getServletContext().getAttribute(ServiceFactory.SERVLET_CONTEXT_KEY);
        auth = f.getAuthService();
        applications = f.getApplicationService();
        jobs = f.getJobService();
        workloadService = f.getWorkloadService();
    }

    /**
     * Loads dashboard data and forwards to the admin dashboard JSP.
     *
     * @param req  the incoming request; optional {@code msg} flash message
     * @param resp the response; 403 or redirect when not admin
     * @throws ServletException if the JSP forward fails
     * @throws IOException      if data loading or authorization fails
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!ensureAdmin(req, resp)) {
            return;
        }
        String msg = req.getParameter("msg");
        if (msg != null && !msg.trim().isEmpty()) {
            req.setAttribute("adminMessage", msg);
        }
        List<User> users = auth.listAllUsers();
        List<Application> appList = applications.listAll();
        List<Job> allJobs = jobs.getAllJobs();
        req.setAttribute("applications", appList);
        req.setAttribute("users", users);
        req.setAttribute("jobs", allJobs);

        Map<String, TaWorkloadStats> taWorkload = workloadService.buildTaWorkloadStats(users, appList);
        req.setAttribute("taWorkload", taWorkload);

        AdminDashboardMetrics metrics = AdminMetricsBuilder.build(users, appList, allJobs, taWorkload);
        req.setAttribute("chartDataJson", AdminMetricsBuilder.toChartJson(metrics));
        req.setAttribute("aiEnabled", LmConfig.load(getServletContext()).isEnabled());
        req.setAttribute("highWorkloadTaCount", metrics.highWorkloadTaCount);

        List<User> taUsersWorkloadOrder = users.stream()
                .filter(user -> user != null && Roles.TA.equals(user.role)
                        && user.id != null && !user.id.trim().isEmpty())
                .sorted(Comparator
                        .comparingInt((User user) -> {
                            TaWorkloadStats row = taWorkload.get(user.id.trim());
                            return row == null ? 0 : row.accepted;
                        }).reversed()
                        .thenComparingInt((User user) -> {
                            TaWorkloadStats row = taWorkload.get(user.id.trim());
                            return row == null ? 0 : row.total;
                        }).reversed()
                        .thenComparing(user -> user.name != null ? user.name : "",
                                String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        req.setAttribute("taUsersWorkloadOrder", taUsersWorkloadOrder);

        req.getRequestDispatcher("/WEB-INF/jsp/admin/dashboard.jsp").forward(req, resp);
    }
}
