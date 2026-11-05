package com.bupt.ta.service;

import com.bupt.ta.model.Job;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for reading and writing TA job postings from {@code jobs.json}.
 *
 * <p>Jobs are stored as a JSON array with nested fields (skills, schedule, capacity). MO users
 * create and publish vacancies; TAs browse published jobs via {@link com.bupt.ta.servlet.JobServlet}.
 * Admin can inspect or delete entries from the same file.
 *
 * <p>Uses hand-rolled JSON read/write (no external library). Not thread-safe for concurrent writes.
 *
 * @see com.bupt.ta.model.Job
 * @see com.bupt.ta.servlet.MoServlet
 */
public class JobService {

    private final String jobsJsonPath;

    /** @param jobsJsonPath absolute or relative path to {@code jobs.json} */
    public JobService(String jobsJsonPath) {
        this.jobsJsonPath = jobsJsonPath;
    }

    /** Convenience constructor resolving {@code dataDir/jobs.json}. */
    public JobService(Path dataDir) {
        this(dataDir.resolve("jobs.json").toString());
    }

    /** Loads every job from disk (returns empty list when the file is missing). */
    public List<Job> getAllJobs() {
        List<Job> jobs = new ArrayList<>();
        try {
            Path path = Paths.get(jobsJsonPath);
            if (!Files.exists(path)) {
                return jobs;
            }
            String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            for (String obj : splitTopLevelObjects(json)) {
                Job job = parseJobObject(obj);
                applyDefaults(job);
                jobs.add(job);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jobs;
    }

    /** Removes one job by id when present. */
    public void deleteJobById(String id) throws IOException {
        if (id == null || id.isEmpty()) {
            return;
        }
        List<Job> jobs = getAllJobs();
        boolean changed = false;
        for (Iterator<Job> it = jobs.iterator(); it.hasNext();) {
            Job j = it.next();
            if (id.equals(j.getId())) {
                it.remove();
                changed = true;
                break;
            }
        }
        if (changed) {
            writeJobs(jobs);
        }
    }

    /** Returns a job by id, or {@code null} when not found. */
    public Job getJobById(String id) {
        if (id == null) {
            return null;
        }
        for (Job job : getAllJobs()) {
            if (id.equals(job.getId())) {
                return job;
            }
        }
        return null;
    }

    /** Lists jobs visible to applicants (excludes Draft and Closed). */
    public List<Job> listPublishedJobs() {
        return getAllJobs().stream()
                .filter(j -> {
                    String st = firstNonEmpty(j.getStatus(), "Published");
                    return !"Draft".equalsIgnoreCase(st) && !"Closed".equalsIgnoreCase(st);
                })
                .collect(Collectors.toList());
    }

    /** Returns all jobs created by the given module owner. */
    public List<Job> getJobsByMoId(String moId) {
        if (moId == null || moId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String id = moId.trim();
        return getAllJobs().stream()
                .filter(j -> id.equals(n(j.getCreatedByMoId())))
                .collect(Collectors.toList());
    }

    /** Returns {@code true} when {@code jobId} belongs to {@code moId}. */
    public boolean isOwnedByMo(String jobId, String moId) throws IOException {
        if (jobId == null || jobId.trim().isEmpty() || moId == null || moId.trim().isEmpty()) {
            return false;
        }
        Job j = getJobById(jobId.trim());
        return j != null && moId.trim().equals(n(j.getCreatedByMoId()));
    }

    /**
     * Updates editable fields on an MO-owned job that is not closed.
     *
     * @return {@code false} when ownership fails, job is missing, or status is Closed
     */
    public boolean updateJobFields(String jobId, String moId, String moduleName, String moduleCode,
                                   String description, String deadline, List<String> skills) throws IOException {
        if (!isOwnedByMo(jobId, moId)) {
            return false;
        }
        List<Job> jobs = getAllJobs();
        Job j = findJobInList(jobs, jobId.trim());
        if (j == null) {
            return false;
        }
        String st = firstNonEmpty(j.getStatus(), "Published");
        if ("Closed".equalsIgnoreCase(st)) {
            return false;
        }
        if (moduleName != null && !moduleName.trim().isEmpty()) {
            j.setModuleName(moduleName.trim());
        }
        if (moduleCode != null && !moduleCode.trim().isEmpty()) {
            j.setModuleCode(moduleCode.trim());
        }
        if (description != null && !description.trim().isEmpty()) {
            j.setDescription(description.trim());
        }
        if (deadline != null) {
            j.setApplicationDeadline(deadline.trim());
        }
        if (skills != null) {
            j.setRequiredSkills(skills);
        }
        applyDefaults(j);
        writeJobs(jobs);
        return true;
    }

    /** Sets status to Closed when the MO owns the job. */
    public boolean closeJob(String jobId, String moId) throws IOException {
        if (!isOwnedByMo(jobId, moId)) {
            return false;
        }
        List<Job> jobs = getAllJobs();
        Job j = findJobInList(jobs, jobId.trim());
        if (j == null) {
            return false;
        }
        j.setStatus("Closed");
        applyDefaults(j);
        writeJobs(jobs);
        return true;
    }

    /**
     * Appends a new job with default timestamps and status.
     *
     * @return the persisted job (id assigned when blank)
     */
    public Job createJob(Job job) throws IOException {
        String today = LocalDate.now().toString();
        if (job.getId() == null || job.getId().trim().isEmpty()) {
            job.setId(UUID.randomUUID().toString());
        }
        if (job.getStatus() == null || job.getStatus().trim().isEmpty()) {
            job.setStatus("Draft");
        }
        if (job.getCreatedAt() == null || job.getCreatedAt().trim().isEmpty()) {
            job.setCreatedAt(today);
        }
        if ("Published".equalsIgnoreCase(job.getStatus())) {
            if (job.getPostDate() == null || job.getPostDate().trim().isEmpty()) {
                job.setPostDate(today);
            }
            if (job.getPublishedAt() == null || job.getPublishedAt().trim().isEmpty()) {
                job.setPublishedAt(today);
            }
        }
        applyDefaults(job);
        List<Job> jobs = getAllJobs();
        jobs.add(job);
        writeJobs(jobs);
        return job;
    }

    /** Transitions a job to Published and fills post/publish dates when absent. */
    public boolean publishJob(String jobId, String moId) throws IOException {
        if (jobId == null || jobId.trim().isEmpty()) {
            return false;
        }
        List<Job> jobs = getAllJobs();
        Job j = findJobInList(jobs, jobId.trim());
        if (j == null) {
            return false;
        }
        String today = LocalDate.now().toString();
        j.setStatus("Published");
        if (j.getPostDate() == null || j.getPostDate().trim().isEmpty()) {
            j.setPostDate(today);
        }
        if (j.getPublishedAt() == null || j.getPublishedAt().trim().isEmpty()) {
            j.setPublishedAt(today);
        }
        if (j.getCreatedByMoId() == null || j.getCreatedByMoId().trim().isEmpty()) {
            j.setCreatedByMoId(moId == null ? "" : moId);
        }
        applyDefaults(j);
        writeJobs(jobs);
        return true;
    }

    private static Job findJobInList(List<Job> jobs, String jobId) {
        for (Job j : jobs) {
            if (jobId.equals(j.getId())) {
                return j;
            }
        }
        return null;
    }

    private void writeJobs(List<Job> jobs) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < jobs.size(); i++) {
            if (i > 0) {
                sb.append(",\n");
            }
            sb.append(jobToJson(jobs.get(i)));
        }
        sb.append("\n]");
        Path path = Paths.get(jobsJsonPath);
        Files.createDirectories(path.getParent());
        Files.write(path, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String jobToJson(Job job) {
        StringBuilder sb = new StringBuilder();
        sb.append("  {\n");
        sb.append("    \"id\": \"").append(esc(job.getId())).append("\",\n");
        sb.append("    \"moduleName\": \"").append(esc(job.getModuleName())).append("\",\n");
        sb.append("    \"moduleCode\": \"").append(esc(job.getModuleCode())).append("\",\n");
        sb.append("    \"activityType\": \"").append(esc(job.getActivityType())).append("\",\n");
        sb.append("    \"requiredSkills\": ");
        sb.append(listToJson(job.getRequiredSkills()));
        sb.append(",\n");
        sb.append("    \"description\": \"").append(esc(job.getDescription())).append("\",\n");
        sb.append("    \"postDate\": \"").append(esc(job.getPostDate())).append("\",\n");
        sb.append("    \"applicationDeadline\": \"").append(esc(job.getApplicationDeadline())).append("\",\n");
        sb.append("    \"duration\": \"").append(esc(job.getDuration())).append("\",\n");
        sb.append("    \"numberOfTAs\": \"").append(esc(job.getNumberOfTAs())).append("\",\n");
        sb.append("    \"schedule\": ").append(listToJson(job.getSchedule())).append(",\n");
        sb.append("    \"status\": \"").append(esc(firstNonEmpty(job.getStatus(), "Published"))).append("\",\n");
        sb.append("    \"createdByMoId\": \"").append(esc(job.getCreatedByMoId())).append("\",\n");
        sb.append("    \"createdAt\": \"").append(esc(job.getCreatedAt())).append("\",\n");
        sb.append("    \"publishedAt\": \"").append(esc(job.getPublishedAt())).append("\",\n");
        sb.append("    \"workloadHours\": \"").append(esc(job.getWorkloadHours())).append("\"\n");
        sb.append("  }");
        return sb.toString();
    }

    private static String listToJson(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        if (values != null) {
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("\"").append(esc(values.get(i))).append("\"");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static Job parseJobObject(String obj) {
        Job job = new Job();
        job.setId(extractString(obj, "id"));
        job.setModuleName(extractString(obj, "moduleName"));
        job.setModuleCode(extractString(obj, "moduleCode"));
        job.setActivityType(extractString(obj, "activityType"));
        job.setDescription(extractString(obj, "description"));
        job.setRequiredSkills(extractStringArray(obj, "requiredSkills"));
        job.setPostDate(extractString(obj, "postDate"));
        job.setApplicationDeadline(firstNonEmpty(
                extractString(obj, "applicationDeadline"),
                extractString(obj, "deadline"),
                ""));
        job.setDuration(firstNonEmpty(extractString(obj, "duration"), "One semester"));
        job.setNumberOfTAs(firstNonEmpty(extractString(obj, "numberOfTAs"), "2"));
        job.setStatus(firstNonEmpty(extractString(obj, "status"), "Published"));
        job.setCreatedByMoId(extractString(obj, "createdByMoId"));
        job.setCreatedAt(extractString(obj, "createdAt"));
        job.setPublishedAt(extractString(obj, "publishedAt"));
        job.setWorkloadHours(extractString(obj, "workloadHours"));
        List<String> schedule = extractStringArray(obj, "schedule");
        if (schedule.isEmpty()) {
            schedule = buildDefaultSchedule(job.getActivityType());
        }
        job.setSchedule(schedule);
        return job;
    }

    private static List<String> splitTopLevelObjects(String json) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    objects.add(json.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return objects;
    }

    private static final Pattern STRING_FIELD =
            Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern ARRAY_FIELD =
            Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\\[[^\\]]*\\])");

    private static String extractString(String obj, String field) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
                .matcher(obj);
        if (m.find()) {
            return unesc(m.group(1));
        }
        return "";
    }

    private static List<String> extractStringArray(String obj, String field) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(\\[[^\\]]*\\])")
                .matcher(obj);
        if (!m.find()) {
            return out;
        }
        Matcher sm = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"").matcher(m.group(1));
        while (sm.find()) {
            out.add(unesc(sm.group(1)));
        }
        return out;
    }

    private static String unesc(String s) {
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r");
    }

    private Job applyDefaults(Job job) {
        if (job.getApplicationDeadline() == null || job.getApplicationDeadline().trim().isEmpty()) {
            job.setApplicationDeadline("");
        }
        if (job.getDuration() == null || job.getDuration().trim().isEmpty()) {
            job.setDuration("One semester");
        }
        if (job.getNumberOfTAs() == null || job.getNumberOfTAs().trim().isEmpty()) {
            job.setNumberOfTAs("2");
        }
        if (job.getStatus() == null || job.getStatus().trim().isEmpty()) {
            job.setStatus("Published");
        }
        List<String> schedule = job.getSchedule();
        if (schedule == null || schedule.isEmpty()) {
            job.setSchedule(buildDefaultSchedule(job.getActivityType()));
        }
        if (job.getRequiredSkills() == null) {
            job.setRequiredSkills(new ArrayList<>());
        }
        return job;
    }

    private static String n(String s) {
        return s != null ? s : "";
    }

    private static String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return "";
    }

    private static List<String> buildDefaultSchedule(String activityType) {
        List<String> slots = new ArrayList<>();
        String t = activityType == null ? "" : activityType.toLowerCase();
        if (t.contains("lab")) {
            slots.add("Monday 14:00 - 16:00");
            slots.add("Wednesday 14:00 - 16:00");
        } else if (t.contains("tutorial")) {
            slots.add("Tuesday 10:00 - 11:30");
            slots.add("Thursday 10:00 - 11:30");
        } else if (t.contains("invigil")) {
            slots.add("Exam period (dates announced by module leader)");
        } else {
            slots.add("Schedule to be confirmed");
        }
        return slots;
    }
}
