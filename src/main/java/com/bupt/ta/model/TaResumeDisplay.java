package com.bupt.ta.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * View model combining a TA {@link User}, optional {@link ApplicantProfile}, and parsed
 * education rows for admin read-only resume listing.
 *
 * <p>Constructed by admin servlets/services at request time; not persisted. {@link #visibleEducation}
 * filters out blank education rows; {@link #hasSavedProfile} indicates whether a non-empty profile
 * exists on disk for the user.
 *
 * <p>Not thread-safe: list fields are unmodifiable or copies; safe to expose to JSP if not
 * mutated after construction.
 */
public class TaResumeDisplay {
    /** Account record for the TA user. */
    public final User user;
    /** Applicant profile; never {@code null} (empty shell used when missing). */
    public final ApplicantProfile profile;
    /** Full parsed education list from the profile JSON. */
    public final List<EducationEntry> education;
    /** Education rows with at least one non-blank field, for compact display. */
    public final List<EducationEntry> visibleEducation;
    /** {@code true} when a persisted profile with meaningful content exists for this user. */
    public final boolean hasSavedProfile;

    /**
     * Builds a display DTO, normalizing null profile and education inputs.
     *
     * @param user      TA user account (required for display)
     * @param profile   applicant profile, or {@code null} if none saved
     * @param education parsed education entries, or {@code null} if none
     */
    public TaResumeDisplay(User user, ApplicantProfile profile, List<EducationEntry> education) {
        this.user = user;
        this.profile = profile != null ? profile : new ApplicantProfile();
        this.education = education != null ? education : Collections.emptyList();
        this.visibleEducation = filterNonBlankEducation(this.education);
        this.hasSavedProfile = profile != null && profile.userId != null && !profile.userId.isEmpty()
                && hasAnyProfileContent(profile);
    }

    private static List<EducationEntry> filterNonBlankEducation(List<EducationEntry> in) {
        List<EducationEntry> out = new ArrayList<>();
        for (EducationEntry e : in) {
            if (e == null) continue;
            if (nonBlank(e.school) || nonBlank(e.degree) || nonBlank(e.major) || nonBlank(e.period)) {
                out.add(e);
            }
        }
        return out;
    }

    private static boolean hasAnyProfileContent(ApplicantProfile p) {
        return nonBlank(p.fullName) || nonBlank(p.email) || nonBlank(p.phone)
                || nonBlank(p.major) || nonBlank(p.skills) || nonBlank(p.courses)
                || nonBlank(p.educationJson) || nonBlank(p.cvFileName);
    }

    private static boolean nonBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /**
     * Returns whether education rows are present for display.
     *
     * @return {@code true} if {@link #visibleEducation} contains at least one row
     */
    public boolean hasEducationRows() {
        return !visibleEducation.isEmpty();
    }
}
