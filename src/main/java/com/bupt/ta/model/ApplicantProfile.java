package com.bupt.ta.model;

/**
 * Applicant profile for TA recruitment: personal data, education, courses, availability,
 * skills, and CV reference.
 *
 * <p>Persisted in {@code WEB-INF/data/profiles.json} (one row per user) via
 * {@link com.bupt.ta.service.ProfileService}. Public fields support direct JSON serialization.
 * Legacy field names ({@link #degreeProgramme}, {@link #yearOfStudy}, etc.) remain for
 * backward compatibility with older profile records.
 *
 * <p>Not thread-safe; concurrent edits to the same profile should go through the service layer.
 */
public class ApplicantProfile {
    /** {@link User#id} this profile belongs to; primary key in profiles.json. */
    public String userId;

    // --- Personal information
    /** Applicant's full legal or preferred name. */
    public String fullName;
    /** Gender as entered on the profile form. */
    public String gender;
    /**
     * Current study level: {@code Master} or {@code Doctoral} (English values only).
     * Validated by {@link com.bupt.ta.util.ApplicantFieldValidation#isAllowedApplicantDegreeLevel(String)}.
     */
    public String degree;
    /** Academic major or field of study. */
    public String major;
    /** BUPT-style 10-digit student number. */
    public String studentId;
    /** 18-digit PRC resident ID number. */
    public String idCard;
    /** Mobile phone, typically normalized to {@code +86} format. */
    public String phone;
    /** Contact email address. */
    public String email;

    /**
     * JSON array of flat education objects, e.g.
     * {@code [{"school":"...","degree":"...","major":"...","period":"..."},...]}.
     * Parsed into {@link EducationEntry} lists for display.
     */
    public String educationJson;

    /** Courses completed, one course name per line. */
    public String courses;

    /** Availability or free-time description for TA duties. */
    public String freeTime;

    /** Comma- or line-separated skills relevant to TA roles. */
    public String skills;

    /**
     * Stored CV file name under {@code WEB-INF/data/cv/{userId}/}.
     * The binary file is managed separately from profiles.json.
     */
    public String cvFileName;

    // --- legacy fields (older profiles.json)
    /** Legacy degree programme label from pre-refactor profiles. */
    public String degreeProgramme;
    /** Legacy year-of-study text from pre-refactor profiles. */
    public String yearOfStudy;
    /** Legacy availability field; superseded by {@link #freeTime} in newer forms. */
    public String availability;
    /** Short self-introduction or personal statement. */
    public String selfIntro;

    /** Creates an empty profile with no user id. */
    public ApplicantProfile() {}

    /**
     * Creates a profile shell bound to the given user.
     *
     * @param userId owning {@link User#id}
     */
    public ApplicantProfile(String userId) {
        this.userId = userId;
    }
}
