package com.bupt.ta.service.ai;

import com.bupt.ta.ai.LmConfig;
import com.bupt.ta.util.Strings;

/**
<<<<<<< Updated upstream
 * Shared defaults for AI feature services (avoids duplicating pickModel / null guards).
 */
final class AiLmDefaults {

    static final String FALLBACK_MODEL = LmModelDefaults.CHAT_FALLBACK;

=======
 * Shared defaults and null-safe helpers for AI feature services.
 * <p>
 * Centralises model selection and string normalisation so individual feature
 * services ({@link SkillMatchService}, {@link MissingSkillService}, etc.) do not duplicate
 * {@link LmConfig#getModel()} handling or empty-string guards. Not intended for use
 * outside the {@code com.bupt.ta.service.ai} package.
 */
final class AiLmDefaults {

    /**
     * Returns an empty string for {@code null} input.
     *
     * @param s input text
     * @return {@code s} or {@code ""} when {@code s} is null
     */
>>>>>>> Stashed changes
    static String nz(String s) {
        return Strings.nullToEmpty(s);
    }

<<<<<<< Updated upstream
    static String modelOrFallback(LmConfig config) {
        String m = config.getModel();
        return (m != null && !m.isEmpty()) ? m : FALLBACK_MODEL;
=======
    /**
     * Returns the configured chat model without inventing a provider-specific fallback.
     *
     * @param config LM configuration
     * @return configured model identifier, or empty string for an explicit HTTP error
     */
    static String configuredModel(LmConfig config) {
        return Strings.nullToEmpty(config.getModel());
>>>>>>> Stashed changes
    }

    private AiLmDefaults() {}
}
