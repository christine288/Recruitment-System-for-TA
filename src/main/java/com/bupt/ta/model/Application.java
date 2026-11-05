package com.bupt.ta.model;

/**
 * Represents a TA job application submitted by a student for a specific module and role.
 *
 * <p>Instances are serialized to {@code WEB-INF/data/applications.json} and loaded by
 * {@link com.bupt.ta.service.ApplicationService}. Public fields are used for JSON binding;
 * mutating a loaded instance does not persist until the service writes the file back.
 *
 * <p>Not thread-safe: each request typically works with its own instances or service-level
 * synchronization when updating the shared JSON store.
 */
public class Application {
    /** Unique application identifier (UUID or similar). */
    public String id;
    /** {@link User#id} of the applicant who submitted this application. */
    public String userId;
    /** Human-readable module name; must match the target {@link Job#getModuleName()} for stats. */
    public String moduleName;
    /** Module code; paired with {@link #moduleName} to identify the job posting. */
    public String moduleCode;
    /** Role applied for, e.g. {@code "Teaching Assistant"}. */
    public String role;
    /** Date the application was submitted, ISO format {@code yyyy-MM-dd}. */
    public String applicationDate;
    /**
     * Workflow status: {@code Pending}, {@code Accepted}, {@code Rejected}, or {@code Withdrawn}.
     * Defaults to {@code Pending} for new applications.
     */
    public String status = "Pending";
    /** Optional feedback message from an administrator or module owner. */
    public String feedback;

    /** Creates an empty application with default status {@code Pending}. */
    public Application() {}

    /**
     * Creates an application with core fields; status remains {@code Pending}.
     *
     * @param id              unique application id
     * @param userId          applicant user id
     * @param moduleName      target module name
     * @param moduleCode      target module code
     * @param role            role applied for
     * @param applicationDate submission date ({@code yyyy-MM-dd})
     */
    public Application(String id, String userId, String moduleName, String moduleCode,
                       String role, String applicationDate) {
        this.id              = id;
        this.userId          = userId;
        this.moduleName      = moduleName;
        this.moduleCode      = moduleCode;
        this.role            = role;
        this.applicationDate = applicationDate;
    }
}
