package com.bupt.ta.model;

/**
 * One row in an applicant's education history table on the profile page.
 *
 * <p>Used as a deserialized element from {@link ApplicantProfile#educationJson} and as a
 * view-model row in {@link TaResumeDisplay}. Not persisted as a standalone entity; stored
 * inline inside the profile's JSON blob.
 *
 * <p>Not thread-safe if shared across threads; typically constructed per request.
 */
public class EducationEntry {
    /** Name of the institution or school. */
    public String school;
    /** Degree or qualification obtained (e.g. Bachelor, Master). */
    public String degree;
    /** Major or field of study at that institution. */
    public String major;
    /** Study period, e.g. {@code "2018–2022"} or free text. */
    public String period;

    /** Creates an empty education row. */
    public EducationEntry() {}

    /**
     * Creates an education row with all fields set.
     *
     * @param school name of the institution
     * @param degree degree or qualification
     * @param major  major or field of study
     * @param period study period description
     */
    public EducationEntry(String school, String degree, String major, String period) {
        this.school = school;
        this.degree = degree;
        this.major  = major;
        this.period = period;
    }
}
