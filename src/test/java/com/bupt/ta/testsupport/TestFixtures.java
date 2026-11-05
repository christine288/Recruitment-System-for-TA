package com.bupt.ta.testsupport;

import com.bupt.ta.model.ApplicantProfile;
import com.bupt.ta.model.Application;
import com.bupt.ta.model.Job;
import com.bupt.ta.model.Roles;
import com.bupt.ta.model.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;

/** Shared factories and seed data for service-layer tests. */
public final class TestFixtures {

    private TestFixtures() {}

    public static void seedEmptyDataDir(Path dataDir) throws IOException {
        Files.createDirectories(dataDir);
        for (String file : new String[]{
                "applications.json", "users.json", "profiles.json",
                "favorites.json", "recently-viewed.json",
                "audit-logs.json", "password-reset-tokens.json"}) {
            Files.write(dataDir.resolve(file), "[]".getBytes(StandardCharsets.UTF_8));
        }
        Files.write(dataDir.resolve("jobs.json"), "[]".getBytes(StandardCharsets.UTF_8));
    }

    public static void writeJobsJson(Path dataDir, String json) throws IOException {
        Files.createDirectories(dataDir);
        Files.write(dataDir.resolve("jobs.json"), json.getBytes(StandardCharsets.UTF_8));
    }

    public static String jobJsonSingle(String id, String moduleName, String moduleCode,
                                       String status, String numberOfTAs) {
        return "[\n"
                + "  {\n"
                + "    \"id\": \"" + id + "\",\n"
                + "    \"moduleName\": \"" + moduleName + "\",\n"
                + "    \"moduleCode\": \"" + moduleCode + "\",\n"
                + "    \"activityType\": \"Lab\",\n"
                + "    \"requiredSkills\": [\"Java\"],\n"
                + "    \"description\": \"Test job\",\n"
                + "    \"postDate\": \"2026-01-01\",\n"
                + "    \"applicationDeadline\": \"2026-12-31\",\n"
                + "    \"duration\": \"One semester\",\n"
                + "    \"numberOfTAs\": \"" + numberOfTAs + "\",\n"
                + "    \"schedule\": [\"Mon 10:00\"],\n"
                + "    \"status\": \"" + status + "\",\n"
                + "    \"createdByMoId\": \"mo1\",\n"
                + "    \"createdAt\": \"2026-01-01\",\n"
                + "    \"publishedAt\": \"2026-01-01\",\n"
                + "    \"workloadHours\": \"4h/week\"\n"
                + "  }\n"
                + "]";
    }

    public static User sampleUser(String id, String email, String role) {
        User u = new User(id, "Test User", validBuptStudentId(), email, "secret123");
        u.role = role;
        u.active = true;
        return u;
    }

    public static User sampleTa(String id, String email) {
        return sampleUser(id, email, Roles.TA);
    }

    public static User sampleAdmin(String id) {
        return sampleUser(id, "admin@test.local", Roles.ADMIN);
    }

    public static User sampleMo(String id) {
        return sampleUser(id, "mo@test.local", Roles.MO);
    }

    public static Application sampleApplication(String id, String userId, String moduleName, String moduleCode) {
        Application a = new Application(id, userId, moduleName, moduleCode, "TA", "2026-05-01");
        a.status = "Pending";
        return a;
    }

    public static Job sampleJob(String id, String moduleName, String moduleCode) {
        Job j = new Job();
        j.setId(id);
        j.setModuleName(moduleName);
        j.setModuleCode(moduleCode);
        j.setActivityType("Lab");
        j.setDescription("Desc");
        j.setNumberOfTAs("2");
        j.setStatus("Published");
        j.setPostDate("2026-01-01");
        j.setApplicationDeadline("2026-12-31");
        j.setCreatedByMoId("mo-1");
        j.setWorkloadHours("4h/week");
        return j;
    }

    public static ApplicantProfile completeProfile(String userId) {
        ApplicantProfile p = new ApplicantProfile();
        p.userId = userId;
        p.fullName = "Test User";
        p.gender = "Male";
        p.degree = "Master";
        p.major = "Computer Science";
        p.studentId = validBuptStudentId();
        p.idCard = validIdCard18();
        p.phone = "+8613800138000";
        p.email = userId + "@bupt.edu.cn";
        p.courses = "CS101";
        p.freeTime = "Evenings";
        p.skills = "Java";
        p.educationJson = "[{\"school\":\"BUPT\",\"degree\":\"BSc\",\"major\":\"CS\",\"period\":\"2020-2024\"}]";
        p.cvFileName = "resume.pdf";
        return p;
    }

    public static String validBuptStudentId() {
        return Year.now().getValue() + "000001";
    }

    public static String validIdCard18() {
        return "11010519491231002X";
    }

    public static String validEmail() {
        return "test.user@bupt.edu.cn";
    }
}
