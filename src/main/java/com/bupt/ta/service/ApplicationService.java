package com.bupt.ta.service;

import com.bupt.ta.model.Application;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing TA job applications persisted in {@code applications.json}.
 *
 * <p>Supports listing, submission, withdrawal, and status updates (including MO/admin decisions).
 * Each record links a {@link com.bupt.ta.model.User} to a module/role with {@code Pending},
 * {@code Accepted}, or {@code Rejected} status and optional MO feedback.
 *
 * <p>Not thread-safe: file I/O via {@link FileStore} on a shared JSON file per deployment.
 *
 * @see com.bupt.ta.servlet.ApplicationServlet
 * @see com.bupt.ta.servlet.MoServlet
 */
public class ApplicationService {

    private static final String APPLICATIONS_JSON = "applications.json";
    private final FileStore store;

    /**
     * @param dataDir directory containing {@code applications.json}
     */
    public ApplicationService(Path dataDir) {
        this.store = new FileStore(dataDir);
    }

    private static Application mapToApplication(Map<String, String> m) {
        Application a = new Application();
        a.id = m.getOrDefault("id", "");
        a.userId = m.getOrDefault("userId", "");
        a.moduleName = m.getOrDefault("moduleName", "");
        a.moduleCode = m.getOrDefault("moduleCode", "");
        a.role = m.getOrDefault("role", "");
        a.applicationDate = m.getOrDefault("applicationDate", "");
        a.status = m.getOrDefault("status", "Pending");
        if (a.status.isEmpty()) {
            a.status = "Pending";
        }
        a.feedback = m.getOrDefault("feedback", "");
        return a;
    }

    private static Map<String, String> applicationToMap(Application a) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("id", n(a.id));
        m.put("userId", n(a.userId));
        m.put("moduleName", n(a.moduleName));
        m.put("moduleCode", n(a.moduleCode));
        m.put("role", n(a.role));
        m.put("applicationDate", n(a.applicationDate));
        m.put("status", n(a.status));
        m.put("feedback", n(a.feedback));
        return m;
    }

    private List<Application> loadAll() throws IOException {
        return store.readMaps(APPLICATIONS_JSON).stream()
                .map(ApplicationService::mapToApplication)
                .collect(Collectors.toList());
    }

    private void saveAll(List<Application> apps) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        for (Application a : apps) {
            rows.add(applicationToMap(a));
        }
        store.writeMaps(APPLICATIONS_JSON, rows);
    }

    /** Returns all applications submitted by the given user (empty list when {@code userId} is null). */
    public List<Application> getByUserId(String userId) throws IOException {
        if (userId == null) {
            return new ArrayList<>();
        }
        return loadAll().stream()
                .filter(a -> userId.equals(a.userId))
                .collect(Collectors.toList());
    }

    /** Returns every application row in {@code applications.json}. */
    public List<Application> listAll() throws IOException {
        return loadAll();
    }

    /** Looks up a single application by primary key. */
    public Optional<Application> findById(String appId) throws IOException {
        if (appId == null || appId.isEmpty()) {
            return Optional.empty();
        }
        return loadAll().stream()
                .filter(a -> appId.equals(a.id))
                .findFirst();
    }

    /**
     * Inserts or replaces an application (assigns UUID when {@code app.id} is blank).
     *
     * @return the persisted application with id set
     */
    public Application save(Application app) throws IOException {
        if (app.id == null || app.id.isEmpty()) {
            app.id = UUID.randomUUID().toString();
        }
        List<Application> apps = loadAll();
        apps.removeIf(a -> app.id.equals(a.id));
        apps.add(app);
        saveAll(apps);
        return app;
    }

    /** Updates pipeline status and MO feedback for one application; no-op when id is unknown. */
    public void updateStatus(String appId, String status, String feedback) throws IOException {
        List<Application> apps = loadAll();
        for (Application a : apps) {
            if (appId.equals(a.id)) {
                a.status = status != null ? status : "Pending";
                a.feedback = feedback != null ? feedback : "";
                saveAll(apps);
                return;
            }
        }
    }

    /** Removes all applications owned by {@code userId} (account deletion cascade). */
    public void deleteByUserId(String userId) throws IOException {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        List<Application> apps = loadAll();
        apps.removeIf(a -> userId.equals(a.userId));
        saveAll(apps);
    }

    private static String n(String s) {
        return s != null ? s : "";
    }
}
