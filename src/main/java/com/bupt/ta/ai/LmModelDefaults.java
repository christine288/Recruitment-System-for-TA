package com.bupt.ta.ai;

/**
 * Default model identifiers used when no explicit model is configured or requested.
 *
 * <p>These constants provide a safe fallback in the LM integration layer so that
 * {@link HttpLmClient} can still issue requests when {@link LmConfig#getModel()} and
 * {@link LmRequest#getModel()} are both empty. They do not override values set in
 * {@code WEB-INF/lm.properties} or environment variables loaded by {@link LmConfig#load}.
 *
 * @see LmConfig#getModel()
 * @see LmRequest#getModel()
 * @see HttpLmClient
 */
public final class LmModelDefaults {

    /**
     * OpenAI-style chat model name used when neither {@link LmRequest#getModel()} nor
     * {@link LmConfig#getModel()} supplies a non-blank value.
     *
     * <p>Applied by {@link HttpLmClient} via {@code Strings.firstNonBlank(request model,
     * config model, CHAT_FALLBACK)}.
     */
    public static final String CHAT_FALLBACK = "gpt-4o-mini";

    /** Prevents instantiation of this constants-only utility class. */
    private LmModelDefaults() {}
}
