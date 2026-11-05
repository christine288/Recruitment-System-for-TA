package com.bupt.ta.servlet;

import com.bupt.ta.ai.LmClient;
import com.bupt.ta.ai.LmClientFactory;
import com.bupt.ta.ai.LmConfig;
import com.bupt.ta.service.ai.AiFeatureOutput;
import com.bupt.ta.service.ai.AiFeatureService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Admin-only interactive page for the configured AI provider (Doubao by default).
 *
 * <p><b>URL pattern:</b> {@code /admin/ai-demo}
 *
 * <p><b>Role access:</b> {@link com.bupt.ta.model.Roles#ADMIN} only via {@link #ensureAdmin}.
 *
 * <p>GET shows the demo form; POST invokes skill match, missing-skill, or job-recommendation
 * features and displays structured results. Keeps LM calls out of JSP.
 */
@WebServlet(urlPatterns = {"/admin/ai-demo"})
public class AiDemoServlet extends BaseServlet {

    private LmConfig lmConfig;
    private AiFeatureService aiFeature;

    @Override
    public void init() {
        lmConfig = LmConfig.load(getServletContext());
        LmClient client = LmClientFactory.create(lmConfig);
        aiFeature = new AiFeatureService(lmConfig, client);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!ensureAdmin(req, resp)) {
            return;
        }
        attachCommonAttrs(req);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/ai-demo.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!ensureAdmin(req, resp)) {
            return;
        }
        req.setCharacterEncoding("UTF-8");
        attachCommonAttrs(req);

        String action = req.getParameter("action");
        AiFeatureOutput result = null;
        if ("match".equals(action)) {
            result = aiFeature.matchApplicantSkills(req.getParameter("applicantSkills"), req.getParameter("jobRequirements"));
        } else if ("missing".equals(action)) {
            result = aiFeature.identifyMissingSkills(req.getParameter("candidateSkills"), req.getParameter("requiredSkills"));
        } else if ("recommend".equals(action)) {
            result = aiFeature.recommendJobs(req.getParameter("candidateProfile"), req.getParameter("openPositions"));
        } else {
            result = AiFeatureOutput.error("Unknown action.");
        }
        req.setAttribute("result", result);
        req.setAttribute("lastAction", action);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/ai-demo.jsp").forward(req, resp);
    }

    private void attachCommonAttrs(HttpServletRequest req) {
        req.setAttribute("lmEnabled", lmConfig.isEnabled());
        req.setAttribute("lmProvider", lmConfig.getProviderType().name().toLowerCase());
    }
}
