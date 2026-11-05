package com.bupt.ta.ai;

/**
 * Identifies which language-model backend the application should use.
 *
 * <p>{@link LmProviderType} is resolved from the {@code LM_PROVIDER} configuration key
 * (see {@link LmConfig#load}) and drives {@link LmClientFactory#create(LmConfig)}:
 * <ul>
 *   <li>{@link #DOUBAO} — Volcengine Ark's OpenAI-compatible chat endpoint</li>
 *   <li>{@link #MOCK} — {@link MockLmClient}, explicitly selected for offline tests</li>
 *   <li>{@link #OPENAI} — {@link HttpLmClient} against an OpenAI-compatible endpoint</li>
 *   <li>{@link #CUSTOM} — same HTTP pipeline with configurable base URL and path</li>
 * </ul>
 *
 * <p>Missing provider settings select {@link #DOUBAO}; unrecognised values fail fast.
 * HTTP providers never silently switch to mock output when configuration is incomplete.
 *
 * @see LmConfig#getProviderType()
 * @see LmClientFactory
 */
public enum LmProviderType {

    /** Volcengine Ark (Doubao) Chat Completions API. */
    DOUBAO,

    /** Offline deterministic client, only when explicitly configured. */
    MOCK,

    /** OpenAI-compatible Chat Completions over HTTPS POST with JSON body. */
    OPENAI,

    /**
     * Same HTTP pipeline as {@link #OPENAI}; intended for custom endpoints or vendor adapters.
     * Vendor-specific differences should remain inside {@link HttpLmClient}.
     */
    CUSTOM;

    /**
     * Parses a configuration string into a provider type.
     *
     * <p>Matching is case-insensitive. Recognised values: {@code doubao}, {@code mock},
     * {@code openai}, {@code custom}. {@code null} or blank selects {@link #DOUBAO};
     * unknown values throw {@link IllegalArgumentException}.
     *
     * @param raw value from {@code LM_PROVIDER} / {@code lm.provider}; may be {@code null}
     * @return the corresponding {@link LmProviderType}, never {@code null}
     */
    public static LmProviderType fromString(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return DOUBAO;
        }
        String s = raw.trim().toLowerCase();
        switch (s) {
            case "doubao":
                return DOUBAO;
            case "openai":
                return OPENAI;
            case "custom":
                return CUSTOM;
            case "mock":
                return MOCK;
            default:
                throw new IllegalArgumentException("Unsupported LM_PROVIDER: " + raw);
        }
    }
}
