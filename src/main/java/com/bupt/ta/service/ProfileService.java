package com.bupt.ta.service;

import com.bupt.ta.model.ApplicantProfile;
import com.bupt.ta.model.EducationEntry;
import com.bupt.ta.util.ApplicantFieldValidation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Persistence and validation helpers for applicant profiles in {@code profiles.json}.
 *
 * <p>Stores personal fields, education JSON, skills/courses, availability, and CV filename.
 * On save, basic identity fields can be synced back to {@code users.json} via {@link AuthService}.
 * Profile completeness gates job application and AI matching features.
 *
 * <p>Also provides {@link #buildAiCapabilityText} and {@link #hasAiMatchingInput} for LM prompts.
 *
 * @see com.bupt.ta.servlet.ProfileServlet
 * @see com.bupt.ta.model.ApplicantProfile
 */
public class ProfileService {

    private static final String PROFILES_JSON = "profiles.json";
    private final FileStore store;

    /** @param dataDir directory containing {@code profiles.json} */
    public ProfileService(Path dataDir) {
        this.store = new FileStore(dataDir);
    }

    /** Parses the {@code educationJson} column into structured rows. */
    public static List<EducationEntry> parseEducationJson(String json) {
        List<EducationEntry> out = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) {
            return out;
        }
        String trimmed = json.trim();
        if (!trimmed.startsWith("[")) {
            return out;
        }
        try {
            List<Map<String, String>> rows = FileStore.parseJsonArray(trimmed);
            for (Map<String, String> row : rows) {
                EducationEntry e = new EducationEntry();
                e.school = row.getOrDefault("school", "");
                e.degree = row.getOrDefault("degree", "");
                e.major = row.getOrDefault("major", "");
                e.period = row.getOrDefault("period", "");
                out.add(e);
            }
        } catch (Exception ignored) {
            // keep empty
        }
        return out;
    }

    /** Serializes education rows to JSON stored in {@link ApplicantProfile#educationJson}. */
    public static String buildEducationJson(List<EducationEntry> entries) {
        List<Map<String, String>> maps = new ArrayList<>();
        for (EducationEntry e : entries) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("school", n(e.school));
            row.put("degree", n(e.degree));
            row.put("major", n(e.major));
            row.put("period", n(e.period));
            maps.add(row);
        }
        return FileStore.toJsonArrayOfObjects(maps);
    }

    /** Returns {@code true} when all required applicant fields are present (CV is optional). */
    public static boolean isApplicantProfileComplete(ApplicantProfile p) {
        if (p == null) {
            return false;
        }
        if (isBlank(p.fullName) || isBlank(p.gender) || isBlank(p.degree) || isBlank(p.major)) {
            return false;
        }
        if (!ApplicantFieldValidation.isAllowedApplicantDegreeLevel(p.degree)) {
            return false;
        }
        if (isBlank(p.studentId) || isBlank(p.idCard) || isBlank(p.phone) || isBlank(p.email)) {
            return false;
        }
        if (isBlank(p.courses) || isBlank(p.freeTime) || isBlank(p.skills)) {
            return false;
        }
        if (isBlank(p.educationJson)) {
            return false;
        }
        return !parseEducationJson(p.educationJson).isEmpty();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Returns {@code true} when skills or courses are populated for AI matching. */
    public static boolean hasAiMatchingInput(ApplicantProfile p) {
        if (p == null) {
            return false;
        }
        return !isBlank(p.skills) || !isBlank(p.courses);
    }

    /** Builds a plain-text capability summary for LM prompts. */
    public static String buildAiCapabilityText(ApplicantProfile p) {
        if (p == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (!isBlank(p.fullName)) {
            sb.append("Name: ").append(p.fullName.trim()).append("\n");
        }
        if (!isBlank(p.major)) {
            sb.append("Major: ").append(p.major.trim()).append("\n");
        }
        if (!isBlank(p.degree)) {
            sb.append("Degree: ").append(p.degree.trim()).append("\n");
        }
        if (!isBlank(p.skills)) {
            sb.append("Skills: ").append(p.skills.trim()).append("\n");
        }
        if (!isBlank(p.courses)) {
            sb.append("Courses: ").append(p.courses.trim()).append("\n");
        }
        if (!isBlank(p.freeTime)) {
            sb.append("Availability: ").append(p.freeTime.trim()).append("\n");
        }
        return sb.toString().trim();
    }

    private static ApplicantProfile mapToProfile(Map<String, String> m) {
        ApplicantProfile p = new ApplicantProfile();
        p.userId = m.getOrDefault("userId", "");
        p.fullName = n(m.get("fullName"));
        p.gender = n(m.get("gender"));
        p.degree = n(m.get("degree"));
        p.major = n(m.get("major"));
        p.studentId = n(m.get("studentId"));
        p.idCard = n(m.get("idCard"));
        p.phone = n(m.get("phone"));
        p.email = n(m.get("email"));
        p.educationJson = n(m.get("educationJson"));
        p.courses = n(m.get("courses"));
        p.freeTime = n(m.get("freeTime"));
        p.skills = n(m.get("skills"));
        p.cvFileName = n(m.get("cvFileName"));
        p.degreeProgramme = n(m.get("degreeProgramme"));
        p.yearOfStudy = n(m.get("yearOfStudy"));
        p.availability = n(m.get("availability"));
        p.selfIntro = n(m.get("selfIntro"));
        if (p.freeTime.isEmpty() && !p.availability.isEmpty()) {
            p.freeTime = p.availability;
        }
        return p;
    }

    private static Map<String, String> profileToMap(ApplicantProfile p) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("userId", n(p.userId));
        m.put("fullName", n(p.fullName));
        m.put("gender", n(p.gender));
        m.put("degree", n(p.degree));
        m.put("major", n(p.major));
        m.put("studentId", n(p.studentId));
        m.put("idCard", n(p.idCard));
        m.put("phone", n(p.phone));
        m.put("email", n(p.email));
        m.put("educationJson", n(p.educationJson));
        m.put("courses", n(p.courses));
        m.put("freeTime", n(p.freeTime));
        m.put("skills", n(p.skills));
        m.put("cvFileName", n(p.cvFileName));
        m.put("degreeProgramme", n(p.degreeProgramme));
        m.put("yearOfStudy", n(p.yearOfStudy));
        m.put("availability", n(p.availability));
        m.put("selfIntro", n(p.selfIntro));
        return m;
    }

    /** Loads the profile row for one user, if any. */
    public Optional<ApplicantProfile> getByUserId(String userId) throws IOException {
        if (userId == null || userId.isEmpty()) {
            return Optional.empty();
        }
        return store.readMaps(PROFILES_JSON).stream()
                .filter(m -> userId.equals(m.get("userId")))
                .map(ProfileService::mapToProfile)
                .findFirst();
    }

    /** Inserts or replaces the profile row keyed by {@code profile.userId}. */
    public ApplicantProfile upsert(ApplicantProfile profile) throws IOException {
        List<Map<String, String>> rows = store.readMaps(PROFILES_JSON);
        rows.removeIf(m -> profile.userId != null && profile.userId.equals(m.get("userId")));
        rows.add(profileToMap(profile));
        store.writeMaps(PROFILES_JSON, rows);
        return profile;
    }

    /** Removes the profile row when a user account is deleted. */
    public void deleteByUserId(String userId) throws IOException {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        List<Map<String, String>> rows = store.readMaps(PROFILES_JSON);
        rows.removeIf(m -> userId.equals(m.get("userId")));
        store.writeMaps(PROFILES_JSON, rows);
    }

    private static String n(String s) {
        return s != null ? s : "";
    }
}
