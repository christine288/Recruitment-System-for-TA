package com.bupt.ta.servlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import com.bupt.ta.model.ApplicantProfile;
import com.bupt.ta.model.EducationEntry;
import com.bupt.ta.model.User;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.service.AuthService;
import com.bupt.ta.service.ProfileService;
import com.bupt.ta.util.ApplicantFieldValidation;

/**
 * Manages TA applicant profile viewing, editing, and CV upload.
 *
 * <p><b>URL pattern:</b> {@code /profile}
 *
 * <p><b>Role access:</b> Authenticated users only (typically {@link com.bupt.ta.model.Roles#TA}).
 * Unauthenticated callers are redirected to {@code /login}.
 *
 * <p>Supports multipart form posts for CV files (PDF, DOC, DOCX). GET shows the profile form;
 * POST saves profile fields, education entries, and optional CV changes.
 */
@WebServlet(urlPatterns = {"/profile"})
@MultipartConfig(
        fileSizeThreshold = 0,
        maxFileSize = 10 * 1024 * 1024,
        maxRequestSize = 12 * 1024 * 1024
)
public class ProfileServlet extends BaseServlet {
    /** Session key: CV file name on disk not yet written to profiles.json (validation failed after upload). */
    public static final String PENDING_CV_SESSION_ATTR = "taPendingCvFileName";

    private ProfileService profiles;
    private AuthService auth;
    private Path cvDataDir;

    /**
     * Initializes profile and auth services from {@code WEB-INF/data}.
     */
    @Override
    public void init() {
        ServiceFactory f = (ServiceFactory) getServletContext().getAttribute(ServiceFactory.SERVLET_CONTEXT_KEY);
        profiles = f.getProfileService();
        auth = f.getAuthService();
        cvDataDir = f.getCvDataDir();
    }

    /**
     * Loads the current user's profile for display or edit mode.
     *
     * @param req  the incoming request; optional {@code edit=1} forces edit mode;
     *             optional {@code msg} shows an info message
     * @param resp the response; redirects to {@code /login} when unauthenticated
     * @throws ServletException if the JSP forward fails
     * @throws IOException      if profile loading fails
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User u = currentUser(req);
        if (!ensureTa(req, resp)) {
            return;
        }
        Optional<ApplicantProfile> existing = profiles.getByUserId(u.id);
        ApplicantProfile p = existing.orElse(new ApplicantProfile(u.id));
        mergeDefaultsFromUser(u, p);
        mapLegacyDegreeForForm(p);
        applyPendingCvFromSession(req, u.id, p);
        boolean profileComplete = ProfileService.isApplicantProfileComplete(p);
        boolean forceEdit = "1".equals(req.getParameter("edit"));
        boolean editable = !profileComplete || forceEdit;
        req.setAttribute("profileComplete", profileComplete);
        String msg = req.getParameter("msg");
        req.setAttribute("profile", p);
        req.setAttribute("user", u);
        req.setAttribute("editable", editable);
        if (msg != null && !msg.trim().isEmpty()) {
            req.setAttribute("infoMessage", escapeHtml(msg.trim()));
        }
        List<EducationEntry> edus = ProfileService.parseEducationJson(p.educationJson);
        if (edus.isEmpty()) {
            edus = new ArrayList<>();
            edus.add(new EducationEntry());
        }
        req.setAttribute("educationList", edus);
        req.getRequestDispatcher("/WEB-INF/jsp/profile.jsp").forward(req, resp);
    }

    private static void mergeDefaultsFromUser(User u, ApplicantProfile p) {
        if ((p.fullName == null || p.fullName.trim().isEmpty()) && u.name != null) {
            p.fullName = u.name;
        }
        if ((p.studentId == null || p.studentId.trim().isEmpty()) && u.studentId != null) {
            p.studentId = u.studentId;
        }
        if ((p.email == null || p.email.trim().isEmpty()) && u.email != null) {
            p.email = u.email;
        }
    }

    /**
     * Maps legacy stored degree labels to the English whitelist for display only (in-memory).
     * Clears values that are not eligible (e.g. undergraduate) or unknown.
     */
    private static void mapLegacyDegreeForForm(ApplicantProfile p) {
        if (p == null || p.degree == null) {
            return;
        }
        String d = p.degree.trim();
        // Legacy DB values (Unicode escapes — stored labels before English-only migration).
        if ("\u7855\u58eb\u7814\u7a76\u751f".equals(d)) {
            p.degree = "Master";
        } else if ("\u535a\u58eb\u7814\u7a76\u751f".equals(d)) {
            p.degree = "Doctoral";
        } else if ("\u672c\u79d1".equals(d) || !ApplicantFieldValidation.isAllowedApplicantDegreeLevel(d)) {
            p.degree = "";
        }
    }

    /**
     * Saves profile changes, handles CV upload/delete, and validates all applicant fields.
     *
     * @param req  the incoming request; multipart form with profile fields; optional
     *             {@code action=deleteCvOnly} removes CV without full save
     * @param resp the response; redirects to {@code /profile} on success or re-forwards with
     *             field errors on validation failure
     * @throws ServletException if the JSP forward fails
     * @throws IOException      if file or persistence operations fail
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        if (!ensureTa(req, resp)) {
            return;
        }
        User u = currentUser(req);

        String action = trim(req.getParameter("action"));
        if ("deleteCvOnly".equals(action)) {
            handleDeleteCvOnly(req, resp, u);
            return;
        }

        Optional<ApplicantProfile> existing = profiles.getByUserId(u.id);
        final String dbCvFileNameAtStart = existing
                .map(e -> e.cvFileName == null ? "" : e.cvFileName.trim())
                .filter(s -> !s.isEmpty())
                .orElse("");

        ApplicantProfile p = new ApplicantProfile(u.id);
        p.fullName = trim(req.getParameter("fullName"));
        p.gender = trim(req.getParameter("gender"));
        p.degree = trim(req.getParameter("degree"));
        p.major = trim(req.getParameter("major"));
        p.studentId = trim(req.getParameter("studentId"));
        p.idCard = trim(req.getParameter("idCard"));
        if (p.idCard != null && p.idCard.length() == 18) {
            p.idCard = p.idCard.toUpperCase();
        }
        p.phone = trim(req.getParameter("phone"));
        String normalizedPhone = ApplicantFieldValidation.normalizeChinaPhone(p.phone);
        if (normalizedPhone != null) {
            p.phone = normalizedPhone;
        }
        p.email = trim(req.getParameter("email"));
        p.courses = req.getParameter("courses");
        if (p.courses != null) {
            p.courses = p.courses.replace("\r\n", "\n").replace("\r", "\n");
        }
        p.freeTime = trim(req.getParameter("freeTime"));
        p.skills = req.getParameter("skills");
        if (p.skills != null) {
            p.skills = p.skills.replace("\r\n", "\n").replace("\r", "\n");
        }

        List<EducationEntry> eduRows = buildEducationFromRequest(req);
        p.educationJson = ProfileService.buildEducationJson(eduRows);

        p.cvFileName = existing.map(e -> e.cvFileName).orElse("");
        p.degreeProgramme = existing.map(e -> e.degreeProgramme).orElse("");
        p.yearOfStudy = existing.map(e -> e.yearOfStudy).orElse("");
        p.availability = existing.map(e -> e.availability).orElse("");
        p.selfIntro = existing.map(e -> e.selfIntro).orElse("");

        Path cvUserDir = cvDataDir.resolve(u.id);
        Part cvPart = req.getPart("cv");
        final boolean newCvUpload = cvPart != null && cvPart.getSize() > 0;
        if (!newCvUpload) {
            mergePendingCvIntoProfileForPost(req, u.id, p, cvUserDir);
        }

        String preDeleteCvName = p.cvFileName == null ? "" : p.cvFileName.trim();
        boolean deleteCv = "1".equals(req.getParameter("deleteCv"));
        if (deleteCv) {
            p.cvFileName = "";
        }

        String uploadedName = null;
        Path uploadedPath = null;
        if (newCvUpload) {
            String submitted = getSubmittedFileNameCompat(cvPart);
            String safe = safeFileName(submitted);
            if (!isAllowedCvFile(safe)) {
                p.cvFileName = preDeleteCvName;
                Map<String, String> cvError = new LinkedHashMap<>();
                cvError.put("cv", "Invalid CV file type. Please upload PDF, DOC, or DOCX.");
                forwardProfileEdit(req, resp, u, p, eduRows, cvError);
                return;
            }
            uploadedName = safe;
            p.cvFileName = safe;
        }

        Map<String, String> fieldErrors = validateProfileInput(p, eduRows);
        if (!fieldErrors.isEmpty()) {
            if (newCvUpload) {
                p.cvFileName = dbCvFileNameAtStart;
                req.getSession().removeAttribute(PENDING_CV_SESSION_ATTR);
            } else {
                setPendingCvSessionIfFileExists(req, u.id, p, cvUserDir);
            }
            forwardProfileEdit(req, resp, u, p, eduRows, fieldErrors);
            return;
        }

        Path rollbackOldPath = null;
        try {
            auth.updateUserBasics(u, p.fullName, p.studentId, p.email);
            req.getSession().setAttribute("user", u);
            if (newCvUpload && uploadedName != null) {
                Files.createDirectories(cvUserDir);
                Path staged = cvUserDir.resolve(".upload-" + System.currentTimeMillis() + "-" + uploadedName);
                cvPart.write(staged.toAbsolutePath().toString());
                uploadedPath = staged;
                rollbackOldPath = backupExistingCv(cvUserDir, dbCvFileNameAtStart);
                Path dest = cvUserDir.resolve(uploadedName);
                Files.move(staged, dest, StandardCopyOption.REPLACE_EXISTING);
                uploadedPath = dest;
            }
            profiles.upsert(p);
            req.getSession().removeAttribute(PENDING_CV_SESSION_ATTR);
            if (!dbCvFileNameAtStart.isEmpty()) {
                boolean deletedWithoutReplace = deleteCv && uploadedName == null;
                if (deletedWithoutReplace) {
                    Files.deleteIfExists(cvUserDir.resolve(dbCvFileNameAtStart));
                }
            }
        } catch (IllegalArgumentException ex) {
            cleanupCancelledUpload(uploadedPath, uploadedName);
            restoreBackedUpCv(rollbackOldPath, cvUserDir, dbCvFileNameAtStart);
            p.cvFileName = dbCvFileNameAtStart;
            if (newCvUpload) {
                req.getSession().removeAttribute(PENDING_CV_SESSION_ATTR);
            } else {
                mergePendingCvIntoProfileForPost(req, u.id, p, cvUserDir);
            }
            Map<String, String> authErrors = new LinkedHashMap<>();
            authErrors.put("email", ex.getMessage());
            List<EducationEntry> edus = ProfileService.parseEducationJson(p.educationJson);
            if (edus.isEmpty()) {
                edus = new ArrayList<>();
                edus.add(new EducationEntry());
            }
            forwardProfileEdit(req, resp, u, p, edus, authErrors);
            return;
        } catch (Exception ex) {
            cleanupCancelledUpload(uploadedPath, uploadedName);
            restoreBackedUpCv(rollbackOldPath, cvUserDir, dbCvFileNameAtStart);
            p.cvFileName = dbCvFileNameAtStart;
            if (newCvUpload) {
                req.getSession().removeAttribute(PENDING_CV_SESSION_ATTR);
            } else {
                mergePendingCvIntoProfileForPost(req, u.id, p, cvUserDir);
            }
            Map<String, String> saveErr = new LinkedHashMap<>();
            saveErr.put("cv", "Could not save your profile. Your CV upload was cancelled. Please try again.");
            forwardProfileEdit(req, resp, u, p, eduRows, saveErr);
            return;
        }

        resp.sendRedirect(req.getContextPath() + "/profile?msg="
                + urlEncode("Profile saved. You are now in view mode; click Edit to make changes."));
    }

    /** Remove CV file(s) from disk and clear {@code cvFileName} without posting the full profile form. */
    private void handleDeleteCvOnly(HttpServletRequest req, HttpServletResponse resp, User u)
            throws IOException {
        String ctx = req.getContextPath();
        Path cvUserDir = cvDataDir.resolve(u.id).normalize();
        HttpSession session = req.getSession(false);
        String pending = "";
        if (session != null) {
            Object o = session.getAttribute(PENDING_CV_SESSION_ATTR);
            if (o instanceof String) {
                pending = ((String) o).trim();
            }
            session.removeAttribute(PENDING_CV_SESSION_ATTR);
        }
        try {
            Optional<ApplicantProfile> ex = profiles.getByUserId(u.id);
            if (ex.isPresent()) {
                ApplicantProfile pr = ex.get();
                String fn = pr.cvFileName == null ? "" : pr.cvFileName.trim();
                deleteCvFileIfSafe(cvUserDir, fn);
                pr.cvFileName = "";
                profiles.upsert(pr);
            }
            if (!pending.isEmpty()) {
                deleteCvFileIfSafe(cvUserDir, pending);
            }
        } catch (Exception e) {
            resp.sendRedirect(ctx + "/profile?edit=1&msg=" + urlEncode("Could not delete CV. Please try again."));
            return;
        }
        resp.sendRedirect(ctx + "/profile?edit=1&msg=" + urlEncode("Your CV has been removed."));
    }

    private static void deleteCvFileIfSafe(Path cvUserDir, String fileName) throws IOException {
        if (fileName == null || fileName.trim().isEmpty()) {
            return;
        }
        String fn = fileName.trim();
        Path root = cvUserDir.normalize();
        Path f = cvUserDir.resolve(fn).normalize();
        if (!f.startsWith(root) || !Files.isRegularFile(f)) {
            return;
        }
        Files.deleteIfExists(f);
    }

    /**
     * When the browser sends no new {@code cv} part, restore the in-memory filename from a prior
     * failed save (session + file on disk) so validation re-runs with the correct CV state.
     */
    private void mergePendingCvIntoProfileForPost(HttpServletRequest req, String userId,
                                                  ApplicantProfile p, Path cvUserDir) {
        HttpSession s = req.getSession(false);
        if (s == null) {
            return;
        }
        Object o = s.getAttribute(PENDING_CV_SESSION_ATTR);
        if (!(o instanceof String)) {
            return;
        }
        String pend = ((String) o).trim();
        if (pend.isEmpty()) {
            return;
        }
        Path pendFile = cvUserDir.resolve(pend);
        try {
            if (!Files.isRegularFile(pendFile)) {
                s.removeAttribute(PENDING_CV_SESSION_ATTR);
                return;
            }
        } catch (Exception e) {
            s.removeAttribute(PENDING_CV_SESSION_ATTR);
            return;
        }
        String dbName = p.cvFileName == null ? "" : p.cvFileName.trim();
        if (dbName.isEmpty() || !dbName.equals(pend)) {
            p.cvFileName = pend;
        }
    }

    private void applyPendingCvFromSession(HttpServletRequest req, String userId, ApplicantProfile p) {
        HttpSession s = req.getSession(false);
        if (s == null) {
            return;
        }
        Object o = s.getAttribute(PENDING_CV_SESSION_ATTR);
        if (!(o instanceof String)) {
            return;
        }
        String pend = ((String) o).trim();
        if (pend.isEmpty()) {
            return;
        }
        Path f = cvDataDir.resolve(userId).resolve(pend);
        try {
            if (Files.isRegularFile(f)) {
                p.cvFileName = pend;
            } else {
                s.removeAttribute(PENDING_CV_SESSION_ATTR);
            }
        } catch (Exception e) {
            s.removeAttribute(PENDING_CV_SESSION_ATTR);
        }
    }

    private static void setPendingCvSessionIfFileExists(HttpServletRequest req, String userId,
                                                        ApplicantProfile p, Path cvUserDir) {
        HttpSession s = req.getSession(true);
        String fn = p.cvFileName == null ? "" : p.cvFileName.trim();
        if (fn.isEmpty()) {
            s.removeAttribute(PENDING_CV_SESSION_ATTR);
            return;
        }
        Path f = cvUserDir.resolve(fn);
        try {
            if (Files.isRegularFile(f)) {
                s.setAttribute(PENDING_CV_SESSION_ATTR, fn);
            } else {
                s.removeAttribute(PENDING_CV_SESSION_ATTR);
            }
        } catch (Exception e) {
            s.removeAttribute(PENDING_CV_SESSION_ATTR);
        }
    }

    private void forwardProfileEdit(HttpServletRequest req, HttpServletResponse resp, User u,
                                    ApplicantProfile p, List<EducationEntry> eduRows,
                                    Map<String, String> fieldErrors) throws ServletException, IOException {
        req.setAttribute("profile", p);
        req.setAttribute("user", u);
        req.setAttribute("editable", true);
        req.setAttribute("educationList", eduRows.isEmpty() ? defaultEduList() : eduRows);
        req.setAttribute("fieldErrors", fieldErrors);
        req.getRequestDispatcher("/WEB-INF/jsp/profile.jsp").forward(req, resp);
    }

    /** Moves the previous CV aside before a replacement so it can be restored if the save fails. */
    private static Path backupExistingCv(Path cvUserDir, String oldName) throws IOException {
        if (oldName == null || oldName.trim().isEmpty()) {
            return null;
        }
        Path oldPath = cvUserDir.resolve(oldName).normalize();
        if (!Files.isRegularFile(oldPath)) {
            return null;
        }
        Path archiveDir = cvUserDir.resolve("_archive");
        Files.createDirectories(archiveDir);
        String stamp = String.valueOf(System.currentTimeMillis());
        Path dest = archiveDir.resolve(stamp + "_" + oldName).normalize();
        Files.move(oldPath, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }

    private static void restoreBackedUpCv(Path backupPath, Path cvUserDir, String oldName) {
        if (backupPath == null || oldName == null || oldName.trim().isEmpty()) {
            return;
        }
        try {
            Path restorePath = cvUserDir.resolve(oldName).normalize();
            Files.createDirectories(restorePath.getParent());
            if (Files.isRegularFile(backupPath)) {
                Files.move(backupPath, restorePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            // best-effort rollback
        }
    }

    private static void cleanupCancelledUpload(Path uploadedPath, String uploadedName) {
        if (uploadedPath == null || uploadedName == null) {
            return;
        }
        Path target = uploadedPath.normalize();
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }

    private static String trim(String s) {
        return s != null ? s.trim() : "";
    }

    private static List<EducationEntry> buildEducationFromRequest(HttpServletRequest req) {
        String[] schools = req.getParameterValues("edu_school");
        if (schools == null) return new ArrayList<>();
        String[] degrees = req.getParameterValues("edu_degree");
        String[] majors = req.getParameterValues("edu_major");
        String[] periods = req.getParameterValues("edu_period");
        List<EducationEntry> list = new ArrayList<>();
        for (int i = 0; i < schools.length; i++) {
            String school = trim(schools[i]);
            String degree = (degrees != null && i < degrees.length) ? trim(degrees[i]) : "";
            String major = (majors != null && i < majors.length) ? trim(majors[i]) : "";
            String period = (periods != null && i < periods.length) ? trim(periods[i]) : "";
            if (school.isEmpty() && degree.isEmpty() && major.isEmpty() && period.isEmpty()) {
                continue;
            }
            list.add(new EducationEntry(school, degree, major, period));
        }
        return list;
    }

    private static List<EducationEntry> defaultEduList() {
        List<EducationEntry> edus = new ArrayList<>();
        edus.add(new EducationEntry());
        return edus;
    }

    private static Map<String, String> validateProfileInput(ApplicantProfile p, List<EducationEntry> edus) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (isBlank(p.fullName) || !ApplicantFieldValidation.isValidFullName(p.fullName)) {
            errors.put("fullName", "Please enter a valid full name (2-60 letters).");
        }
        if (!("Male".equals(p.gender) || "Female".equals(p.gender) || "Other".equals(p.gender))) {
            errors.put("gender", "Please select a valid gender.");
        }
        if (isBlank(p.degree) || !ApplicantFieldValidation.isAllowedApplicantDegreeLevel(p.degree)) {
            errors.put("degree",
                    "Select Master or Doctoral. Undergraduate applicants are not eligible for this system.");
        }
        if (isBlank(p.major)) errors.put("major", "Major is required.");
        if (!ApplicantFieldValidation.isValidBuptTenDigitStudentId(p.studentId)) {
            errors.put("studentId",
                    "Student ID must be exactly 10 digits; the first 4 digits are your admission year (e.g. 2023xxxxxxxx).");
        }
        if (!ApplicantFieldValidation.isValidChineseResidentId18(p.idCard)) {
            errors.put("idCard", "Please enter a valid 18-digit national ID (with correct check digit).");
        }
        if (isBlank(p.phone) || !ApplicantFieldValidation.isValidChinaMobileNormalized(p.phone)) {
            errors.put("phone", "Please enter a China mobile number: +86 and 11 digits (e.g. +8613912345678 or 13912345678).");
        }
        if (isBlank(p.email) || !ApplicantFieldValidation.isValidEmailWithRealDomain(p.email)) {
            errors.put("email", "Please enter a valid email with a real domain (e.g. name@bupt.edu.cn).");
        }
        if (isBlank(p.courses)) errors.put("courses", "Courses completed is required.");
        if (isBlank(p.freeTime)) errors.put("freeTime", "Availability is required.");
        if (isBlank(p.skills)) errors.put("skills", "Skills is required.");

        if (edus == null || edus.isEmpty()) {
            errors.put("education", "Please add at least one education background record.");
            return errors;
        }
        for (int i = 0; i < edus.size(); i++) {
            EducationEntry e = edus.get(i);
            if (isBlank(e.school)) errors.put("edu_school_" + i, "School is required.");
            if (isBlank(e.degree)) errors.put("edu_degree_" + i, "Degree is required.");
            if (isBlank(e.major)) errors.put("edu_major_" + i, "Major is required.");
            if (isBlank(e.period)) errors.put("edu_period_" + i, "Period is required.");
        }
        return errors;
    }

    private static boolean isAllowedCvFile(String fileName) {
        if (fileName == null) return false;
        String n = fileName.toLowerCase();
        return n.endsWith(".pdf") || n.endsWith(".doc") || n.endsWith(".docx");
    }

    private static String getSubmittedFileNameCompat(Part part) {
        if (part == null) {
            return null;
        }
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition == null) {
            return null;
        }
        String[] tokens = contentDisposition.split(";");
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.startsWith("filename=")) {
                String name = trimmed.substring("filename=".length()).trim();
                if (name.length() >= 2 && name.startsWith("\"") && name.endsWith("\"")) {
                    name = name.substring(1, name.length() - 1);
                }
                return name;
            }
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String safeFileName(String submitted) {
        if (submitted == null) return "cv.pdf";
        String name = submitted;
        int p = Math.max(name.lastIndexOf('\\'), name.lastIndexOf('/'));
        if (p >= 0) {
            name = name.substring(p + 1);
        }
        name = name.trim();
        if (name.isEmpty()) return "cv.pdf";
        if (name.contains("..")) {
            name = name.replace("..", "_");
        }
        return name;
    }
}
