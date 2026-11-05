package com.bupt.ta.ai;

import com.bupt.ta.util.AppConfig;
import com.bupt.ta.util.Strings;

import javax.servlet.ServletContext;
import java.util.Properties;

/**
<<<<<<< Updated upstream
 * LM integration settings. Safe defaults: mock provider, no outbound HTTP required.
=======
 * Immutable snapshot of language-model integration settings for the TA application.
 *
 * <p>{@link LmConfig} is the configuration hub of the LM layer. Settings are loaded once
 * per web application from {@code WEB-INF/lm.properties} and environment variables via
 * {@link AppConfig#resolve}, then consumed by {@link LmClientFactory} to choose between
 * {@link MockLmClient} and {@link HttpLmClient}.
 *
 * <p><strong>Configuration keys</strong> (each also accepts a dotted {@code lm.*} property
 * name in {@code lm.properties}; see {@link #load}):
 * <ul>
 *   <li>{@code LM_ENABLED} — default {@code true}; when {@code false}, factory returns
 *       {@link MockLmClient} with {@code forceDisabled} and AI features report as disabled</li>
 *   <li>{@code LM_PROVIDER} — default {@code doubao}; unknown values fail fast</li>
 *   <li>{@code LM_API_KEY} or {@code ARK_API_KEY} — required for HTTP providers</li>
 *   <li>{@code LM_BASE_URL} — defaults to the Ark API root for Doubao</li>
 *   <li>{@code LM_MODEL} or {@code ARK_MODEL} — activated model or endpoint ID;
 *       required for HTTP providers unless a request overrides it</li>
 *   <li>{@code LM_TIMEOUT_MS} — default {@code 30000}; HTTP connect and non-streaming
 *       request timeout in milliseconds. Streaming requests allow at least 180,000 ms.</li>
 *   <li>{@code LM_HTTP_CHAT_PATH} — default {@code /chat/completions}; appended to {@link #getBaseUrl()}
 *       for OpenAI-compatible chat endpoints</li>
 * </ul>
 *
 * <p>Missing credentials or model IDs produce a visible error instead of mock output.
 * Boolean and integer parsing uses documented defaults when values are malformed.
 *
 * @see LmClientFactory#create(LmConfig)
 * @see HttpLmClient
>>>>>>> Stashed changes
 */
public final class LmConfig {
    public static final String DOUBAO_BASE_URL = "https://ark.cn-beijing.volces.com/api/v3";

    private final boolean enabled;
    private final LmProviderType providerType;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int timeoutMs;
    /** Relative to {@link #baseUrl}, e.g. /chat/completions for OpenAI-compatible APIs. */
    private final String httpChatPath;

    private LmConfig(boolean enabled, LmProviderType providerType, String apiKey, String baseUrl,
                     String model, int timeoutMs, String httpChatPath) {
        this.enabled = enabled;
        this.providerType = providerType;
        this.apiKey = apiKey != null ? apiKey : "";
        this.baseUrl = baseUrl != null ? baseUrl.trim() : "";
        this.model = model != null ? model.trim() : "";
        this.timeoutMs = timeoutMs;
        this.httpChatPath = httpChatPath != null && !httpChatPath.isEmpty() ? httpChatPath : "/chat/completions";
    }

<<<<<<< Updated upstream
=======
    /**
     * Loads LM settings from {@code WEB-INF/lm.properties} and environment variables.
     *
     * <p>Resolution order for each key is handled by {@link AppConfig#resolve}: environment
     * variables take precedence over servlet properties. Property aliases include both
     * uppercase env-style names and lowercase dotted keys (for example {@code LM_PROVIDER}
     * and {@code lm.provider}).
     *
     * @param ctx servlet context used to locate {@code WEB-INF/lm.properties}; must not be {@code null}
     * @return a fully constructed {@link LmConfig} with defaults for missing optional values
     */
>>>>>>> Stashed changes
    public static LmConfig load(ServletContext ctx) {
        Properties p = AppConfig.loadWebInfProperties(ctx, "lm.properties");
        String enabledStr = AppConfig.resolve(
                "LM_ENABLED",
                new String[]{"LM_ENABLED", "lm.enabled"},
                p,
                "LM_ENABLED",
                "true");
        boolean enabled = parseBoolean(enabledStr, true);

        String providerRaw = AppConfig.resolve(
                "LM_PROVIDER",
                new String[]{"LM_PROVIDER", "lm.provider"},
                p,
                "LM_PROVIDER",
                "doubao");
        LmProviderType type = LmProviderType.fromString(providerRaw);

        String apiKey = type == LmProviderType.DOUBAO
                ? resolveArkAlias(p, "LM_API_KEY", "lm.apiKey", "ARK_API_KEY", "ark.apiKey")
                : AppConfig.resolve("LM_API_KEY", new String[]{"LM_API_KEY", "lm.apiKey"},
                        p, "LM_API_KEY", "");

        String baseUrl = AppConfig.resolve(
                "LM_BASE_URL",
                new String[]{"LM_BASE_URL", "lm.baseUrl"},
                p,
                "LM_BASE_URL",
                type == LmProviderType.DOUBAO ? DOUBAO_BASE_URL : "");

        String model = type == LmProviderType.DOUBAO
                ? resolveArkAlias(p, "LM_MODEL", "lm.model", "ARK_MODEL", "ark.model")
                : AppConfig.resolve("LM_MODEL", new String[]{"LM_MODEL", "lm.model"},
                        p, "LM_MODEL", "");

        String timeoutStr = AppConfig.resolve(
                "LM_TIMEOUT_MS",
                new String[]{"LM_TIMEOUT_MS", "lm.timeoutMs"},
                p,
                "LM_TIMEOUT_MS",
                "30000");
        int timeoutMs = parseInt(timeoutStr, 30_000);

        String httpPath = AppConfig.resolve(
                "LM_HTTP_CHAT_PATH",
                new String[]{"LM_HTTP_CHAT_PATH", "lm.httpChatPath"},
                p,
                "LM_HTTP_CHAT_PATH",
                "/chat/completions");

        return new LmConfig(enabled, type, apiKey, baseUrl, model, timeoutMs, httpPath);
    }

    private static String resolveArkAlias(Properties p, String lmKey, String lmDotted,
                                          String arkKey, String arkDotted) {
        return Strings.firstNonBlank(
                System.getenv(lmKey), System.getenv(arkKey),
                System.getProperty(lmKey), System.getProperty(lmDotted),
                System.getProperty(arkKey), System.getProperty(arkDotted),
                p.getProperty(lmKey), p.getProperty(lmDotted),
                p.getProperty(arkKey), p.getProperty(arkDotted));
    }

    private static boolean parseBoolean(String s, boolean defaultVal) {
        if (s == null || s.isEmpty()) return defaultVal;
        if ("1".equals(s) || "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s)) {
            return true;
        }
        if ("0".equals(s) || "false".equalsIgnoreCase(s) || "no".equalsIgnoreCase(s)) {
            return false;
        }
        return defaultVal;
    }

    private static int parseInt(String s, int defaultVal) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LmProviderType getProviderType() {
        return providerType;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

<<<<<<< Updated upstream
=======
    /**
     * Returns the default model name ({@code LM_MODEL}) for requests that do not specify one.
     *
     * <p>When empty, an HTTP request must specify its own model or fails visibly.
     *
     * @return trimmed model identifier, or empty string when not configured
     */
>>>>>>> Stashed changes
    public String getModel() {
        return model;
    }

<<<<<<< Updated upstream
=======
    /**
     * Returns the connect and non-streaming request timeout in milliseconds
     * ({@code LM_TIMEOUT_MS}). Streaming requests have a 180-second minimum.
     *
     * @return timeout value; defaults to {@code 30000} when unset or unparsable
     */
>>>>>>> Stashed changes
    public int getTimeoutMs() {
        return timeoutMs;
    }

    public String getHttpChatPath() {
        return httpChatPath;
    }

<<<<<<< Updated upstream
    /** True when HTTP provider could attempt a real call (still subject to runtime errors). */
=======
    /**
     * Indicates whether an HTTP provider has the minimum credentials to attempt a real call.
     *
     * <p>Requires both non-empty {@link #getApiKey()} and {@link #getBaseUrl()}. A {@code true}
     * result does not guarantee success at runtime (the model may be missing or a network error
     * may occur). {@link HttpLmClient} reports missing settings when invoked.
     *
     * @return {@code true} when both API key and base URL are configured
     */
>>>>>>> Stashed changes
    public boolean hasHttpCredentials() {
        return !apiKey.isEmpty() && !baseUrl.isEmpty();
    }
}
