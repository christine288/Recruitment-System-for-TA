package com.bupt.ta.testsupport;

import com.bupt.ta.model.Job;
import com.bupt.ta.model.User;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.security.PasswordHasher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Temporary JSON data directory for service-layer tests. */
public final class FileTestSupport {

    private static ServiceFactory lastFactory;

    private FileTestSupport() {}

    public static ServiceFactory newFactory() throws IOException {
        Path dataDir = Files.createTempDirectory("ta-data-");
        Path cvDir = Files.createTempDirectory("ta-cv-");
        TestFixtures.seedEmptyDataDir(dataDir);
        lastFactory = ServiceFactory.forTests(dataDir, cvDir);
        return lastFactory;
    }

    public static void seedUser(String userId, String email) throws IOException {
        if (lastFactory == null) {
            throw new IllegalStateException("Call newFactory() before seedUser()");
        }
        User u = TestFixtures.sampleTa(userId, email);
        u.id = userId;
        u.passwordHash = PasswordHasher.hash("secret123");
        lastFactory.getAuthService().insertUser(u);
    }

    public static void seedJob(String jobId) throws IOException {
        if (lastFactory == null) {
            throw new IllegalStateException("Call newFactory() before seedJob()");
        }
        Job j = TestFixtures.sampleJob(jobId, "Module " + jobId, "MOD" + jobId);
        lastFactory.getJobService().createJob(j);
    }
}
