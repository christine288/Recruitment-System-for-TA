package com.bupt.ta.persistence;

import com.bupt.ta.service.ApplicationService;
import com.bupt.ta.service.AuditService;
import com.bupt.ta.service.AuthService;
import com.bupt.ta.service.FavoriteService;
import com.bupt.ta.service.JobService;
import com.bupt.ta.service.NotificationService;
import com.bupt.ta.service.PasswordResetService;
import com.bupt.ta.service.ProfileService;
import com.bupt.ta.service.RecentlyViewedService;
import com.bupt.ta.service.WorkloadService;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Wires services from JSON data files under {@code WEB-INF/data} and CV storage directory.
 *
 * <p>Created once at startup by {@link AppInitListener} and stored in the servlet context under
 * {@link #SERVLET_CONTEXT_KEY}. Servlets resolve domain services (auth, jobs, applications, AI-related
 * workload helpers) from this factory rather than constructing {@link com.bupt.ta.service.FileStore} directly.
 *
 * <p>Thread-safe for read-only service access after initialization; underlying JSON files are not
 * locked for concurrent writes.
 *
 * @see AppInitListener
 */
public final class ServiceFactory {

    /** Servlet context attribute key for the shared {@link ServiceFactory} instance. */
    public static final String SERVLET_CONTEXT_KEY = "com.bupt.ta.ServiceFactory";

    private final Path dataDir;
    private final Path cvDataDir;
    private final AuthService authService;
    private final ProfileService profileService;
    private final ApplicationService applicationService;
    private final JobService jobService;
    private final FavoriteService favoriteService;
    private final RecentlyViewedService recentlyViewedService;
    private final WorkloadService workloadService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final PasswordResetService passwordResetService;

    /**
     * Wires all domain services and ensures data directories exist.
     *
     * @param dataDir  JSON data root (typically {@code WEB-INF/data})
     * @param cvDataDir uploaded CV storage directory
     */
    public ServiceFactory(Path dataDir, Path cvDataDir) throws IOException {
        this.dataDir = dataDir;
        this.cvDataDir = cvDataDir;
        Files.createDirectories(dataDir);
        Files.createDirectories(cvDataDir);

        this.authService = new AuthService(dataDir);
        this.profileService = new ProfileService(dataDir);
        this.applicationService = new ApplicationService(dataDir);
        this.jobService = new JobService(dataDir.resolve("jobs.json").toString());
        this.favoriteService = new FavoriteService(dataDir);
        this.recentlyViewedService = new RecentlyViewedService(dataDir);
        this.workloadService = new WorkloadService();
        this.auditService = new AuditService(dataDir);
        this.notificationService = new NotificationService();
        this.passwordResetService = new PasswordResetService(dataDir, authService, notificationService);
    }

    /** Builds a factory from deployed {@code WEB-INF/data} paths. */
    public static ServiceFactory fromServletContext(ServletContext ctx) throws IOException {
        String dataPath = ctx.getRealPath("/WEB-INF/data");
        String cvPath = ctx.getRealPath("/WEB-INF/data/cv");
        Path dataDir = dataPath != null ? Paths.get(dataPath) : Paths.get("data");
        Path cvDir = cvPath != null ? Paths.get(cvPath) : dataDir.resolve("cv");
        return new ServiceFactory(dataDir, cvDir);
    }

    /** Test helper with explicit data and CV directories. */
    public static ServiceFactory forTests(Path dataDir, Path cvDir) throws IOException {
        return new ServiceFactory(dataDir, cvDir);
    }

    /** Returns the JSON data root directory. */
    public Path getDataDir() {
        return dataDir;
    }

    /** Returns the CV upload directory. */
    public Path getCvDataDir() {
        return cvDataDir;
    }

    /** Authentication and user account service. */
    public AuthService getAuthService() {
        return authService;
    }

    /** Applicant profile persistence service. */
    public ProfileService getProfileService() {
        return profileService;
    }

    /** TA application pipeline service. */
    public ApplicationService getApplicationService() {
        return applicationService;
    }

    /** Job posting read/write service. */
    public JobService getJobService() {
        return jobService;
    }

    /** Per-user job favorites service. */
    public FavoriteService getFavoriteService() {
        return favoriteService;
    }

    /** Recently viewed jobs service. */
    public RecentlyViewedService getRecentlyViewedService() {
        return recentlyViewedService;
    }

    /** In-memory TA workload aggregation (no JSON file). */
    public WorkloadService getWorkloadService() {
        return workloadService;
    }

    /** Admin audit log append service. */
    public AuditService getAuditService() {
        return auditService;
    }

    /** SMTP notification service (no-op when unconfigured). */
    public NotificationService getNotificationService() {
        return notificationService;
    }

    /** Password reset token lifecycle service. */
    public PasswordResetService getPasswordResetService() {
        return passwordResetService;
    }
}
