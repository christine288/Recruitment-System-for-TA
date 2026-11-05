package com.bupt.ta.model;

import java.util.List;

/**
 * A TA job posting created by a module owner (MO) and published for student applicants.
 *
 * <p>Persisted in {@code WEB-INF/data/jobs.json} through {@link com.bupt.ta.service.JobService}.
 * Access is via JavaBean-style getters and setters for JSON mapping. List fields
 * ({@link #requiredSkills}, {@link #schedule}) may be {@code null} when loaded from sparse JSON.
 *
 * <p>Not thread-safe; the service layer serializes concurrent access to the jobs file.
 */
public class Job {
    private String id;
    private String moduleName;
    private String moduleCode;
    private String description;
    private String activityType;
    private List<String> requiredSkills;
    private String postDate;
    private String duration;
    private String applicationDeadline;
    private String numberOfTAs;
    private List<String> schedule;

    private String status;
    private String createdByMoId;
    private String createdAt;
    private String publishedAt;
    private String workloadHours;

    /** Default constructor for JSON binding and service layer. */
    public Job() {}

    /** Returns unique job identifier. */
    public String getId() { return id; }
    /** Sets unique job identifier. */
    public void setId(String id) { this.id = id; }

    /** Returns human-readable module name shown in job listings. */
    public String getModuleName() { return moduleName; }
    /** Sets human-readable module name. */
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }

    /** Returns module code (paired with module name for application matching). */
    public String getModuleCode() { return moduleCode; }
    /** Sets module code. */
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }

    /** Returns full job description text. */
    public String getDescription() { return description; }
    /** Sets full job description text. */
    public void setDescription(String description) { this.description = description; }

    /** Returns type of teaching activity (e.g. lab, tutorial). */
    public String getActivityType() { return activityType; }
    /** Sets type of teaching activity. */
    public void setActivityType(String activityType) { this.activityType = activityType; }

    /** Returns list of required skills; may be {@code null}. */
    public List<String> getRequiredSkills() { return requiredSkills; }
    /** Sets list of required skills. */
    public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }

    /** Returns posting date string used for sorting and display. */
    public String getPostDate() { return postDate; }
    /** Sets posting date string. */
    public void setPostDate(String postDate) { this.postDate = postDate; }

    /** Returns expected duration of the TA assignment. */
    public String getDuration() { return duration; }
    /** Sets expected duration of the assignment. */
    public void setDuration(String duration) { this.duration = duration; }

    /** Returns application deadline date or text. */
    public String getApplicationDeadline() { return applicationDeadline; }
    /** Sets application deadline. */
    public void setApplicationDeadline(String applicationDeadline) { this.applicationDeadline = applicationDeadline; }

    /** Returns number of TA positions as a string (parsed by {@link JobApplicationStats#parseCapacity(String)}). */
    public String getNumberOfTAs() { return numberOfTAs; }
    /** Sets number of TA positions. */
    public void setNumberOfTAs(String numberOfTAs) { this.numberOfTAs = numberOfTAs; }

    /** Returns weekly or session schedule lines; may be {@code null}. */
    public List<String> getSchedule() { return schedule; }
    /** Sets schedule lines. */
    public void setSchedule(List<String> schedule) { this.schedule = schedule; }

    /** Returns lifecycle status, e.g. draft or published. */
    public String getStatus() { return status; }
    /** Sets lifecycle status. */
    public void setStatus(String status) { this.status = status; }

    /** Returns {@link User#id} of the module owner who created this job. */
    public String getCreatedByMoId() { return createdByMoId; }
    /** Sets module owner user id. */
    public void setCreatedByMoId(String createdByMoId) { this.createdByMoId = createdByMoId; }

    /** Returns ISO timestamp when the job record was created. */
    public String getCreatedAt() { return createdAt; }
    /** Sets creation timestamp. */
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    /** Returns ISO timestamp when the job was published, or {@code null} if not yet published. */
    public String getPublishedAt() { return publishedAt; }
    /** Sets publication timestamp. */
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }

    /** Returns expected workload hours for the role (used in MO workload advice). */
    public String getWorkloadHours() { return workloadHours; }
    /** Sets expected workload hours. */
    public void setWorkloadHours(String workloadHours) { this.workloadHours = workloadHours; }
}
