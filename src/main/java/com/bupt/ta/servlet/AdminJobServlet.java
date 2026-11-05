package com.bupt.ta.servlet;

import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.service.JobService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Admin POST endpoint for job deletion.
 *
 * <p><b>URL pattern:</b> {@code /admin/jobs}
 *
 * <p><b>Role access:</b> {@link com.bupt.ta.model.Roles#ADMIN} only via {@link #ensureAdmin}.
 *
 * <p>Only {@code action=delete} with a valid {@code jobId} is supported.
 */
@WebServlet(urlPatterns = {"/admin/jobs"})
public class AdminJobServlet extends BaseServlet {

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
     * Deletes a job by id and redirects back to the admin dashboard.
     *
     * @param req  the incoming request; expects {@code action=delete} and {@code jobId}
     * @param resp the response; redirects to {@code /admin} with a flash message
     * @throws ServletException if dispatch fails
     * @throws IOException      if job deletion fails
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!ensureAdmin(req, resp)) {
            return;
        }
        String action = req.getParameter("action");
        String jobId = req.getParameter("jobId");
        String ctx = req.getContextPath();

        if (!"delete".equals(action) || jobId == null || jobId.trim().isEmpty()) {
            resp.sendRedirect(ctx + "/admin?msg=" + urlEncode("Invalid job action."));
            return;
        }

        try {
            jobService.deleteJobById(jobId.trim());
        } catch (IOException e) {
            resp.sendRedirect(ctx + "/admin?msg=" + urlEncode("Failed to delete job: " + e.getMessage()));
            return;
        }
        resp.sendRedirect(ctx + "/admin");
    }
}
