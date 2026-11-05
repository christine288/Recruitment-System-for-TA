package com.bupt.ta.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies configuration resolution: JVM system properties override {@link Properties} file values
 * (environment variables are not modified here to keep CI deterministic).
 */
class AppConfigTest {

    @AfterEach
    void clearLmSystemProps() {
        System.clearProperty("LM_PROVIDER");
        System.clearProperty("LM_ENABLED");
    }

    @Test
    void resolve_systemPropertyOverridesFile() {
        Properties file = new Properties();
        file.setProperty("LM_PROVIDER", "mock");
        System.setProperty("LM_PROVIDER", "openai");

        String v = AppConfig.resolve(
                "LM_PROVIDER",
                new String[]{"LM_PROVIDER"},
                file,
                "LM_PROVIDER",
                "mock");
        assertEquals("openai", v);
    }

    @Test
    void resolve_usesFileWhenSystemPropertyAbsent() {
        Properties file = new Properties();
        file.setProperty("LM_ENABLED", "false");

        String v = AppConfig.resolve(
                "LM_ENABLED",
                new String[]{"LM_ENABLED"},
                file,
                "LM_ENABLED",
                "true");
        assertEquals("false", v);
    }

    @Test
    void resolve_usesDefaultWhenMissingEverywhere() {
        Properties empty = new Properties();
        String v = AppConfig.resolve(
                "LM_MODEL",
                new String[]{"LM_MODEL"},
                empty,
                "LM_MODEL",
                "default-model");
        assertEquals("default-model", v);
    }

    @Test
    void resolve_blankStringUsesDefault() {
        Properties file = new Properties();
        file.setProperty("LM_MODEL", "   ");
        String v = AppConfig.resolve(
                "LM_MODEL",
                new String[]{"LM_MODEL"},
                file,
                "LM_MODEL",
                "fallback");
        assertEquals("fallback", v);
    }

    @Test
    void resolve_firstMatchingSysPropKey() {
        Properties empty = new Properties();
        System.setProperty("LM_ALT", "from-alt");
        String v = AppConfig.resolve(
                "LM_PROVIDER",
                new String[]{"LM_ALT", "LM_PROVIDER"},
                empty,
                "LM_PROVIDER",
                "mock");
        assertEquals("from-alt", v);
        System.clearProperty("LM_ALT");
    }
}
