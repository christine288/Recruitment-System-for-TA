package com.bupt.ta.servlet;



import com.bupt.ta.model.Roles;

import com.bupt.ta.model.User;

import com.bupt.ta.persistence.ServiceFactory;

import com.bupt.ta.service.ApplicationService;

import com.bupt.ta.service.AuditService;

import com.bupt.ta.service.AuthService;

import com.bupt.ta.service.ProfileService;



import javax.servlet.ServletException;

import javax.servlet.annotation.WebServlet;

import javax.servlet.http.HttpServletRequest;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import java.nio.file.Files;

import java.nio.file.Path;

import java.util.Comparator;

import java.util.Optional;

import java.util.stream.Stream;



/**

 * Admin POST endpoint for user lifecycle actions (activate, deactivate, delete, create).

 *

 * <p><b>URL pattern:</b> {@code /admin/users}

 *

 * <p><b>Role access:</b> {@link com.bupt.ta.model.Roles#ADMIN} only via {@link #ensureAdmin}.

 */

@WebServlet(urlPatterns = {"/admin/users"})

public class AdminUserServlet extends BaseServlet {



    private AuthService auth;

    private ApplicationService applications;

    private ProfileService profiles;

    private AuditService audit;

    private Path cvDataDir;



    @Override

    public void init() {

        ServiceFactory f = (ServiceFactory) getServletContext().getAttribute(ServiceFactory.SERVLET_CONTEXT_KEY);

        auth = f.getAuthService();

        applications = f.getApplicationService();

        profiles = f.getProfileService();

        audit = f.getAuditService();

        cvDataDir = f.getCvDataDir();

    }



    @Override

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        if (!ensureAdmin(req, resp)) {

            return;

        }



        User admin = currentUser(req);

        String action = req.getParameter("action");

        String ctx = req.getContextPath();



        if ("createUser".equals(action)) {

            String name = req.getParameter("name");

            String email = req.getParameter("email");

            String password = req.getParameter("password");

            String role = req.getParameter("role");

            try {

                User created = auth.createUserByAdmin(name, email, password, role);

                audit.log(admin != null ? admin.id : "", "CREATE_USER", "USER", created.id,

                        "role=" + created.role);

                resp.sendRedirect(ctx + "/admin?msg=" + urlEncode("User created: " + created.email));

            } catch (IllegalArgumentException e) {

                resp.sendRedirect(ctx + "/admin?msg=" + urlEncode(e.getMessage()));

            } catch (Exception e) {

                resp.sendRedirect(ctx + "/admin?msg=" + urlEncode("Could not create user: " + e.getMessage()));

            }

            return;

        }



        String userId = req.getParameter("userId");

        if (userId == null || userId.trim().isEmpty()) {

            resp.sendRedirect(ctx + "/admin?msg=" + urlEncode("Missing user id."));

            return;

        }

        userId = userId.trim();



        if (admin != null && userId.equals(admin.id)) {

            resp.sendRedirect(ctx + "/admin?msg=" + urlEncode("You cannot modify or remove your own account."));

            return;

        }



        Optional<User> targetOpt = auth.findById(userId);

        if (!targetOpt.isPresent()) {

            resp.sendRedirect(ctx + "/admin?msg=" + urlEncode("User not found."));

            return;

        }

        User target = targetOpt.get();



        if (Roles.ADMIN.equals(target.role) && auth.countAdmins() <= 1

                && ("deactivate".equals(action) || "delete".equals(action))) {

            resp.sendRedirect(ctx + "/admin?msg=" + urlEncode("Cannot remove or deactivate the last administrator."));

            return;

        }



        try {

            if ("deactivate".equals(action)) {

                auth.setUserActive(userId, false);

            } else if ("activate".equals(action)) {

                auth.setUserActive(userId, true);

            } else if ("delete".equals(action)) {

                applications.deleteByUserId(userId);

                profiles.deleteByUserId(userId);

                deleteCvDir(cvDataDir, userId);

                auth.removeUserRecord(userId);

                audit.log(admin != null ? admin.id : "", "DELETE_USER", "USER", userId, "");

            } else {

                resp.sendRedirect(ctx + "/admin?msg=" + urlEncode("Unknown action."));

                return;

            }

        } catch (Exception e) {

            resp.sendRedirect(ctx + "/admin?msg=" + urlEncode("Operation failed: " + e.getMessage()));

            return;

        }



        resp.sendRedirect(ctx + "/admin");

    }



    private static void deleteCvDir(Path cvDataDir, String userId) throws IOException {

        Path dir = cvDataDir.resolve(userId);

        if (!Files.exists(dir)) {

            return;

        }

        try (Stream<Path> walk = Files.walk(dir)) {

            walk.sorted(Comparator.reverseOrder()).forEach(p -> {

                try {

                    Files.deleteIfExists(p);

                } catch (IOException ignored) {

                }

            });

        }

    }

}

