package com.bupt.ta.util;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Loads optional {@code WEB-INF/*.properties} files and resolves configuration with a fixed priority:
 * environment variable → JVM system property → properties file → default value.
 *
 * <p>Used at servlet startup and by AI/LM integration to read secrets and feature flags without
 * hard-coding. Missing or unreadable property files are treated as non-fatal empty properties.
 *
 * <p>Thread-safe for {@link #resolve(String, String[], Properties, String, String)}: reads only
 * immutable environment and system properties. {@link #loadWebInfProperties} should be called once
 * per context during initialization.
 */
public final class AppConfig {

    private AppConfig() {}

    /**
     * Loads a UTF-8 properties file from {@code /WEB-INF/{fileName}} on the servlet classpath.
     *
     * <p>Returns an empty {@link Properties} when the context is null, the file name is blank,
     * the resource is missing, or an I/O error occurs (errors are swallowed for optional overrides).
     *
     * @param ctx      servlet context used to locate WEB-INF resources; may be {@code null}
     * @param fileName file name only, e.g. {@code "lm.properties"} (not a full path)
     * @return loaded properties, never {@code null}
     */
    public static Properties loadWebInfProperties(ServletContext ctx, String fileName) {
        Properties p = new Properties();
        if (ctx == null || fileName == null || fileName.isEmpty()) {
            return p;
        }
        String path = "/WEB-INF/" + fileName;
        try (InputStream in = ctx.getResourceAsStream(path)) {
            if (in == null) {
                return p;
            }
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            // Non-fatal: missing or unreadable file is OK for optional local overrides
        }
        return p;
    }

    /**
     * Resolves a configuration string using the first non-blank source in priority order.
     *
     * <ol>
     *   <li>Environment variable named {@code envVar} (if {@code envVar} is non-null)</li>
     *   <li>JVM system properties listed in {@code sysPropKeys}, tried in array order</li>
     *   <li>Entry {@code fileKey} in {@code fileProps}</li>
     *   <li>{@code defaultValue}</li>
     * </ol>
     *
     * @param envVar       environment variable name, e.g. {@code "LM_ENABLED"}; may be {@code null}
     * @param sysPropKeys  system property keys tried in order, e.g. {@code "LM_ENABLED", "lm.enabled"};
     *                     may be {@code null}
     * @param fileProps    properties loaded from WEB-INF; may be {@code null}
     * @param fileKey      key within {@code fileProps}; may be {@code null}
     * @param defaultValue fallback when no source provides a non-blank value
     * @return trimmed resolved value, or {@code defaultValue} (which may be {@code null})
     */
    public static String resolve(String envVar, String[] sysPropKeys, Properties fileProps, String fileKey, String defaultValue) {
        String fromEnv = envVar != null ? System.getenv(envVar) : null;
        if (notBlank(fromEnv)) {
            return fromEnv.trim();
        }
        if (sysPropKeys != null) {
            for (String k : sysPropKeys) {
                if (k == null) continue;
                String v = System.getProperty(k);
                if (notBlank(v)) {
                    return v.trim();
                }
            }
        }
        if (fileProps != null && fileKey != null) {
            String v = fileProps.getProperty(fileKey);
            if (notBlank(v)) {
                return v.trim();
            }
        }
        return defaultValue;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
