package com.bupt.ta.servlet;

import com.bupt.ta.model.Job;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.service.JobService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Read-only admin view of a single job posting.
 *
 * <p><b>URL pattern:</b> {@code /admin/job-view}
 *
 * <p><b>Role access:</b> {@link com.bupt.ta.model.Roles#ADMIN} only via {@link #ensureAdmin}.
 *
 * <p>Only GET is supported; requires {@code id} query parameter.
 */
@WebServlet("/admin/job-view")
public class AdminJobViewServlet extends BaseServlet {

    private JobService jobService;

    /**
     * Initializes {@link JobService} from {@code WEB-INF/data/jobs.json}.
     */
    @Override
    public void init() {
        ServiceFactory f = (ServiceFactory) getServletContext().getAttribute(ServiceFactory.SERVLET_CONTEXT_KEY);
        this.jobService = f.getJobService();
    }

    /**
     * Loads a job by id and forwards to the admin job-view JSP.
     *
     * @param req  the incoming request; requires {@code id}
     * @param resp the response; redirects to {@code /admin} when id is missing; 403 when not admin
     * @throws ServletException if the JSP forward fails
     * @throws IOException      if authorization or redirect fails
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!ensureAdmin(req, resp)) {
            return;
        }
        String id = req.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/admin?msg=" + urlEncode("Missing job id."));
            return;
        }
        Job job = jobService.getJobById(id.trim());
        req.setAttribute("job", job);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/job-view.jsp").forward(req, resp);
    }
}
