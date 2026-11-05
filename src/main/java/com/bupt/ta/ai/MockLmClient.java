package com.bupt.ta.ai;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Offline, deterministic {@link LmClient} for coursework demos, tests, and safe defaults.
 *
 * <p>{@link MockLmClient} is selected by {@link LmClientFactory} when {@link LmConfig} specifies
 * {@link LmProviderType#MOCK}, when AI is disabled ({@link LmConfig#isEnabled()} {@code false}),
 * when HTTP credentials are missing, or when configuration is {@code null}. It performs no
 * network I/O and produces stable, rule-based output for the same inputs—ideal for local
 * development without API keys.
 *
 * <p>Routing uses {@link LmRequest#getFeatureName()} against {@link AiFeatureNames}:
 * skill match, missing skills, job recommendation, and workload advice each have dedicated
 * handlers; unknown features fall back to a generic echo of the user prompt.
 *
 * <p>Responses use provider label {@code "mock"} and model {@code "mock-model"} when no
 * per-request model is set. Unlike {@link HttpLmClient}, errors for disabled AI are returned
 * as unsuccessful {@link LmResponse} instances rather than {@link LmException}.
 *
 * @see LmClientFactory#create(LmConfig)
 * @see AiFeatureNames
 */
public final class MockLmClient implements LmClient {
    /** Provider label returned in {@link LmResponse#getProvider()} for all mock completions. */
    private static final String PROVIDER = "mock";
    private final boolean forceDisabled;

    /**
     * Creates a mock client with AI features enabled (normal mock behaviour).
     */
    public MockLmClient() {
        this(false);
    }

    /**
     * Creates a mock client that optionally reports AI as globally disabled.
     *
     * @param forceDisabled when {@code true}, {@link #generate(LmRequest)} returns
     *                      {@code success=false} with a message indicating
     *                      {@code LM_ENABLED=false}; used by {@link LmClientFactory} when
     *                      {@link LmConfig#isEnabled()} is {@code false}
     */
    public MockLmClient(boolean forceDisabled) {
        this.forceDisabled = forceDisabled;
    }

    /**
     * Simulates streaming by chunking the result of {@link #generate(LmRequest)}.
     *
     * <p>Emits 28-character deltas, then {@link LmStreamListener#onComplete(String)} or
     * {@link LmStreamListener#onError(String)}. Does not throw {@link LmException}.
     *
     * @param request  completion request; must not be {@code null}
     * @param listener streaming callbacks; must not be {@code null}
     */
    @Override
    public void stream(LmRequest request, LmStreamListener listener) {
        LmResponse r = generate(request);
        if (!r.isSuccess()) {
            listener.onError(r.getErrorMessage() != null ? r.getErrorMessage() : "Generation failed");
            return;
        }
        String t = r.getText();
        if (t == null) {
            t = "";
        }
        int step = 28;
        for (int i = 0; i < t.length(); i += step) {
            listener.onDelta(t.substring(i, Math.min(t.length(), i + step)));
        }
        listener.onComplete(r.getModel());
    }

    /**
     * Generates a deterministic completion based on {@link LmRequest#getFeatureName()}.
     *
     * <p>When {@code forceDisabled} is {@code true}, returns an unsuccessful response explaining
     * that AI is disabled. Otherwise dispatches to feature-specific mock logic for
     * {@link AiFeatureNames} constants, or {@link #genericEcho} for unknown features.
     *
     * @param request prompts, feature name, and optional model override; must not be {@code null}
     * @return a successful {@link LmResponse} with mock assistant text, or {@code success=false}
     *         when AI is force-disabled; never throws {@link LmException}
     */
    @Override
    public LmResponse generate(LmRequest request) {
        String model = request.getModel() != null && !request.getModel().isEmpty()
                ? request.getModel()
                : "mock-model";
        if (forceDisabled) {
            return new LmResponse(
                    "",
                    PROVIDER,
                    model,
                    false,
                    null,
                    "AI features are disabled (LM_ENABLED=false). Enable them via environment or WEB-INF/lm.properties.");
        }
        String fn = request.getFeatureName() == null ? "" : request.getFeatureName();
        switch (fn) {
            case AiFeatureNames.SKILL_MATCH:
                return skillMatch(request, model);
            case AiFeatureNames.MISSING_SKILLS:
                return missingSkills(request, model);
            case AiFeatureNames.JOB_RECOMMENDATION:
                return jobRecommendation(request, model);
            case AiFeatureNames.WORKLOAD_ADVICE:
                return workloadAdvice(request, model);
            case AiFeatureNames.DECISION_REVIEW:
                return decisionReview(request, model);
            case AiFeatureNames.ADMIN_ANALYTICS:
                return adminAnalytics(request, model);
            default:
                return genericEcho(request, model);
        }
    }

    private static LmResponse genericEcho(LmRequest request, String model) {
        String up = request.getUserPrompt() != null ? request.getUserPrompt() : "";
        String text = "[Mock LM] No specific handler for feature \""
                + request.getFeatureName()
                + "\". Echo user prompt:\n\n"
                + up;
        return new LmResponse(text, PROVIDER, model, true, null, null);
    }

    private static LmResponse skillMatch(LmRequest request, String model) {
        String up = request.getUserPrompt() != null ? request.getUserPrompt() : "";
        String appBlock = extractSection(up, "Applicant skills:", "Job requirements:");
        String jobBlock = extractSection(up, "Job requirements:", null);
        Set<String> app = splitSkills(normalizeCapabilityText(appBlock.isEmpty() ? up : appBlock));
        Set<String> job = splitSkills(jobBlock);
        if (job.isEmpty()) {
            job = splitSkills(up);
        }

        Set<String> matched = new LinkedHashSet<>(app);
        matched.retainAll(job);
        Set<String> suggested = new LinkedHashSet<>(job);
        suggested.removeAll(app);

        int denom = Math.max(job.size(), 1);
        int score = (int) Math.round((matched.size() * 100.0) / denom);
        score = Math.min(100, Math.max(0, score));

        StringBuilder sb = new StringBuilder();
        sb.append("Match score: ").append(score).append("%\n");
        sb.append("Matched skills: ").append(joinReadable(matched)).append("\n");
        sb.append("Suggested skills to strengthen: ").append(joinReadable(suggested)).append("\n");
        sb.append("\n(Deterministic mock output — replace provider with a real LM for production use.)");
        return new LmResponse(sb.toString().trim(), PROVIDER, model, true, null, null);
    }

    private static LmResponse missingSkills(LmRequest request, String model) {
        String up = request.getUserPrompt() != null ? request.getUserPrompt() : "";
        String cand = extractSection(up, "Candidate skills:", "Required job skills:");
        String reqd = extractSection(up, "Required job skills:", null);
        Set<String> c = splitSkills(normalizeCapabilityText(cand.isEmpty() ? up : cand));
        Set<String> r = splitSkills(reqd);
        if (r.isEmpty()) {
            r = splitSkills(up);
        }
        Set<String> missing = new LinkedHashSet<>(r);
        missing.removeAll(c);

        StringBuilder sb = new StringBuilder();
        sb.append("Missing compared to job requirements:\n");
        if (missing.isEmpty()) {
            sb.append("- (none — candidate covers all listed skills)\n");
        } else {
            for (String m : missing) {
                sb.append("- ").append(m).append("\n");
            }
        }
        sb.append("\n(Deterministic mock output — verify with module organisers before decisions.)");
        return new LmResponse(sb.toString().trim(), PROVIDER, model, true, null, null);
    }

    private static LmResponse jobRecommendation(LmRequest request, String model) {
        String up = request.getUserPrompt() != null ? request.getUserPrompt() : "";
        String profile = extractSection(up, "Candidate profile:", "Open positions:");
        String jobs = extractSection(up, "Open positions:", null);
        List<String> lines = new ArrayList<>();
        for (String line : jobs.split("\\r?\\n")) {
            String t = line.trim();
            if (!t.isEmpty()) {
                lines.add(t);
            }
        }
        if (lines.isEmpty()) {
            for (String line : up.split("\\r?\\n")) {
                String t = line.trim();
                if (!t.isEmpty() && !t.startsWith("Candidate profile:")) {
                    lines.add(t);
                }
            }
        }

        List<RankedJobLine> ranked = new ArrayList<>();
        for (String jobLine : lines) {
            ranked.add(new RankedJobLine(jobLine, scoreJobMatch(profile, jobLine)));
        }
        ranked.sort((a, b) -> Integer.compare(b.score, a.score));

        StringBuilder sb = new StringBuilder();
        sb.append("## Top Recommendations\n");
        if (ranked.isEmpty()) {
            sb.append("- (no job lines parsed — check open positions list)\n");
        } else {
            int rank = 1;
            for (int i = 0; i < Math.min(3, ranked.size()); i++) {
                RankedJobLine row = ranked.get(i);
                sb.append(rank++).append(". **").append(row.line).append("**\n");
                sb.append("   - Match score: ").append(row.score).append("\n");
                sb.append("   - Why: ").append(buildMockReason(profile, row.line, row.score)).append("\n");
            }
        }
        sb.append("\n## Why These Fit\n");
        sb.append("Rankings use your skills, courses, and keywords against each posting (mock rules).\n");
        sb.append("\n## Risks / Notes\n");
        sb.append("(Deterministic mock output — confirm fit with module organisers before applying.)\n");
        return new LmResponse(sb.toString().trim(), PROVIDER, model, true, null, null);
    }

    private static final class RankedJobLine {
        private final String line;
        private final int score;

        private RankedJobLine(String line, int score) {
            this.line = line;
            this.score = score;
        }
    }

    private static int scoreJobMatch(String profile, String jobLine) {
        String p = profile == null ? "" : profile.toLowerCase(Locale.ROOT);
        String j = jobLine == null ? "" : jobLine.toLowerCase(Locale.ROOT);
        if (p.isEmpty() || j.isEmpty()) {
            return 0;
        }
        int score = 0;
        LinkedHashSet<String> tokens = splitSkills(normalizeCapabilityText(profile));
        for (String token : tokens) {
            if (token.length() < 3) {
                continue;
            }
            if (j.contains(token)) {
                score += 2;
            }
        }
        if (containsAny(p, "machine learning", "ml") && containsAny(j, "machine learning", "data301")) {
            score += 5;
        }
        if (containsAny(p, "data structure", "data structures") && containsAny(j, "cs50", "computer science")) {
            score += 4;
        }
        if (p.contains("python") && j.contains("python")) {
            score += 3;
        }
        if (p.contains("java") && j.contains("java")) {
            score += 3;
        }
        if (containsAny(p, "linear algebra", "math") && containsAny(j, "math201", "linear algebra")) {
            score += 4;
        }
        if (p.contains("english") && containsAny(j, "eng101", "writing", "invigilation")) {
            score += 3;
        }
        if (containsAny(p, "physics", "mechanics") && containsAny(j, "phys150", "physics")) {
            score += 4;
        }
        if (containsAny(p, "statistics", "jupyter") && containsAny(j, "data301", "statistics", "jupyter")) {
            score += 3;
        }
        return score;
    }

    private static boolean containsAny(String text, String... needles) {
        if (text == null) {
            return false;
        }
        for (String needle : needles) {
            if (needle != null && text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static LmResponse workloadAdvice(LmRequest request, String model) {
        String up = request.getUserPrompt() != null ? request.getUserPrompt() : "";
        int potential = parseIntAfterMarker(up, "Potential load if approve:");
        int threshold = parseIntAfterMarker(up, "Warning threshold:");
        if (threshold <= 0) {
            threshold = 3;
        }
        int accepted = parseIntAfterMarker(up, "Accepted count:");
        int pending = parseIntAfterMarker(up, "Pending count:");

        String recommendation;
        if (potential >= threshold + 1) {
            recommendation = "Reject";
        } else if (potential >= threshold) {
            recommendation = "Caution";
        } else {
            recommendation = "Approve";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## Workload Summary\n");
        sb.append("- Accepted: ").append(accepted).append("\n");
        sb.append("- Pending: ").append(pending).append("\n");
        sb.append("- Potential load if approve: **").append(potential).append("**\n");
        sb.append("- Warning threshold: ").append(threshold).append("\n\n");

        sb.append("## Recommendation (Approve / Reject / Caution)\n");
        sb.append("**").append(recommendation).append("**\n\n");

        sb.append("## Reasoning\n");
        if ("Reject".equals(recommendation)) {
            sb.append("Potential load exceeds the safe threshold. Approving would likely overload this TA.\n");
        } else if ("Caution".equals(recommendation)) {
            sb.append("Potential load meets or exceeds the warning threshold. Review schedule overlap before approving.\n");
        } else {
            sb.append("Current accepted + pending workload is below the warning threshold. Approval is reasonable from a load perspective.\n");
        }
        sb.append("\n## If Approved — Expected Load\n");
        sb.append("The TA would carry **").append(potential).append("** active assignments (accepted + pending).\n");
        sb.append("\n(Deterministic mock output — MO retains final decision authority.)");
        return new LmResponse(sb.toString().trim(), PROVIDER, model, true, null, null);
    }

    private static LmResponse decisionReview(LmRequest request, String model) {
        String up = request.getUserPrompt() != null ? request.getUserPrompt() : "";
        String decision = extractLineValue(up, "Decision recorded:");
        if (decision.isEmpty()) {
            decision = "Unknown";
        }
        String profileBlock = extractSection(up, "Applicant profile:", null);
        String skillsLine = extractLineValue(up, "- Skills:");
        String appBlock = profileBlock.isEmpty() ? up : profileBlock;

        Set<String> appSkills = splitSkills(normalizeCapabilityText(appBlock));
        Set<String> jobSkills = splitSkills(skillsLine);
        Set<String> matched = new LinkedHashSet<>(appSkills);
        matched.retainAll(jobSkills);
        int denom = Math.max(jobSkills.size(), 1);
        int score = (int) Math.round((matched.size() * 100.0) / denom);
        score = Math.min(100, Math.max(0, score));
        boolean strongFit = score >= 60 || matched.size() >= 2;

        String assessment;
        if ("Withdrawn".equalsIgnoreCase(decision)) {
            assessment = "Withdrawn";
        } else if ("Accepted".equalsIgnoreCase(decision)) {
            assessment = strongFit ? "Aligned" : "Questionable";
        } else if ("Rejected".equalsIgnoreCase(decision)) {
            assessment = strongFit ? "Questionable" : "Aligned";
        } else {
            assessment = "Review";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## Fit Summary\n");
        sb.append("- Estimated skill overlap: **").append(score).append("%**\n");
        if (!matched.isEmpty()) {
            sb.append("- Matching skills: ").append(String.join(", ", matched)).append("\n");
        }
        sb.append("- Recorded decision: **").append(decision).append("**\n\n");

        sb.append("## Decision Assessment (Aligned / Questionable / Withdrawn)\n");
        sb.append("**").append(assessment).append("**\n\n");

        sb.append("## Profile Highlights\n");
        if (appBlock.toLowerCase(Locale.ROOT).contains("skill")) {
            sb.append("- Applicant profile lists relevant skills/courses for review.\n");
        } else {
            sb.append("- Profile data is limited; decision should rely on CV and interview notes.\n");
        }
        sb.append("\n## Notes for Future Roles\n");
        if ("Questionable".equals(assessment)) {
            sb.append("- Consider documenting clearer criteria when profile fit and outcome differ.\n");
        } else if ("Aligned".equals(assessment)) {
            sb.append("- Outcome appears consistent with the profile and role requirements.\n");
        } else if ("Withdrawn".equals(assessment)) {
            sb.append("- Applicant withdrew; no MO approval action required.\n");
        } else {
            sb.append("- Review applicant profile before similar decisions.\n");
        }
        sb.append("\n(Deterministic mock output — MO retains final decision authority.)");
        return new LmResponse(sb.toString().trim(), PROVIDER, model, true, null, null);
    }

    private static LmResponse adminAnalytics(LmRequest request, String model) {
        String up = request.getUserPrompt() != null ? request.getUserPrompt() : "";
        int totalUsers = parseIntAfterMarker(up, "Total users:");
        int totalApps = parseIntAfterMarker(up, "Total applications:");
        int highLoad = parseIntAfterMarker(up, "TAs at/above workload warning:");

        StringBuilder sb = new StringBuilder();
        sb.append("## Executive Summary\n");
        sb.append("The platform serves **").append(totalUsers).append("** registered users with **")
                .append(totalApps).append("** total applications on record.\n\n");

        sb.append("## User & Applicant Trends\n");
        if (up.contains("Cumulative applicant pool")) {
            sb.append("- Applicant pool growth is visible in the cumulative curve; review months with sharp increases.\n");
        } else {
            sb.append("- Limited historical applicant data; encourage more registrations to improve trend visibility.\n");
        }
        sb.append("- Monitor TA vs MO role balance when planning module coverage.\n\n");

        sb.append("## Application Pipeline Health\n");
        int pending = parseStatusCount(up, "Pending");
        int accepted = parseStatusCount(up, "Accepted");
        int rejected = parseStatusCount(up, "Rejected");
        sb.append("- Pending: **").append(pending).append("**, Accepted: **").append(accepted)
                .append("**, Rejected: **").append(rejected).append("**.\n");
        if (pending > accepted) {
            sb.append("- Backlog is weighted toward pending review — MOs may need follow-up.\n");
        } else {
            sb.append("- Pipeline throughput looks balanced relative to accepted volume.\n");
        }
        sb.append("\n## Workload & Capacity Alerts\n");
        if (highLoad > 0) {
            sb.append("- **").append(highLoad).append("** TA(s) at or above the assigned-job warning threshold.\n");
        } else {
            sb.append("- No TAs currently flagged for excessive assigned workload.\n");
        }
        sb.append("\n## Recommended Actions\n");
        sb.append("1. Review top modules with highest application counts for staffing.\n");
        sb.append("2. Export CSV snapshots before term rollover.\n");
        sb.append("3. Deactivate unused demo accounts if preparing a live demo.\n");
        sb.append("\n(Deterministic mock output — verify figures against dashboard charts.)");
        return new LmResponse(sb.toString().trim(), PROVIDER, model, true, null, null);
    }

    private static int parseStatusCount(String text, String status) {
        int i = text.indexOf("- " + status + ":");
        if (i < 0) return 0;
        return parseIntAfterMarker(text.substring(i), "- " + status + ":");
    }

    private static String extractLineValue(String text, String marker) {
        if (text == null || marker == null) {
            return "";
        }
        int i = text.indexOf(marker);
        if (i < 0) {
            return "";
        }
        int start = i + marker.length();
        int end = text.indexOf('\n', start);
        if (end < 0) {
            end = text.length();
        }
        return text.substring(start, end).trim();
    }

    private static int parseIntAfterMarker(String text, String marker) {
        int i = text.indexOf(marker);
        if (i < 0) {
            return 0;
        }
        int start = i + marker.length();
        StringBuilder num = new StringBuilder();
        for (int j = start; j < text.length(); j++) {
            char c = text.charAt(j);
            if (Character.isDigit(c)) {
                num.append(c);
            } else if (num.length() > 0) {
                break;
            }
        }
        if (num.length() == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(num.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String buildMockReason(String profile, String jobLine, int score) {
        String p = profile == null ? "" : profile.toLowerCase(Locale.ROOT);
        String j = jobLine.toLowerCase(Locale.ROOT);
        if (score >= 5) {
            return "Strong overlap between your skills/courses and this posting.";
        }
        if (p.contains("python") && j.contains("python")) {
            return "Your profile mentions Python, which this role requires.";
        }
        if (containsAny(p, "machine learning", "ml") && containsAny(j, "machine learning", "data301")) {
            return "Machine learning background aligns with this module.";
        }
        if (p.contains("java") && j.contains("java")) {
            return "Profile mentions Java and the role highlights Java-related work.";
        }
        if (containsAny(p, "data structure", "data structures") && containsAny(j, "cs50", "computer science")) {
            return "Computer science coursework aligns with this introductory programming lab role.";
        }
        if (p.contains("lab") && j.contains("lab")) {
            return "Lab experience in the profile aligns with a lab-focused posting.";
        }
        if (score > 0) {
            return "Some keyword overlap detected — review required skills before applying.";
        }
        return "Limited direct overlap — consider building relevant skills or checking prerequisites.";
    }

    private static String extractSection(String full, String startMarker, String endMarker) {
        if (full == null) {
            return "";
        }
        int i = full.indexOf(startMarker);
        if (i < 0) {
            return "";
        }
        int start = i + startMarker.length();
        if (endMarker != null) {
            int j = full.indexOf(endMarker, start);
            if (j < 0) {
                return full.substring(start).trim();
            }
            return full.substring(start, j).trim();
        }
        return full.substring(start).trim();
    }

    private static String normalizeCapabilityText(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("(?i)(name|major|degree|skills|courses|availability):", " ")
                .replaceAll("[\\r\\n]+", ",")
                .trim();
    }

    private static LinkedHashSet<String> splitSkills(String raw) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (raw == null) {
            return set;
        }
        for (String part : raw.split("[,;\\n]")) {
            String t = part.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty()) {
                set.add(t);
            }
        }
        return set;
    }

    private static String joinReadable(Set<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return "(none)";
        }
        return skills.stream().sorted().collect(Collectors.joining(", "));
    }
}
