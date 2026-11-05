package com.bupt.ta.service.ai;

import com.bupt.ta.ai.LmResponse;

/**
 * Safe data transfer object for AI feature results exposed to JSP pages and servlets.
 * <p>
 * Wraps language-model output without stack traces or low-level exception details.
 * Factory methods normalise {@link LmResponse} instances and provide consistent error
 * messages when AI is disabled or the model call fails.
 */
public final class AiFeatureOutput {
    private final boolean success;
    private final String text;
    private final String errorMessage;
    private final String provider;
    private final String model;

    private AiFeatureOutput(boolean success, String text, String errorMessage, String provider, String model) {
        this.success = success;
        this.text = text != null ? text : "";
        this.errorMessage = errorMessage;
        this.provider = provider != null ? provider : "";
        this.model = model != null ? model : "";
    }

    /**
     * Returns whether the AI call completed successfully.
     *
     * @return {@code true} when model text is available; {@code false} on error or disabled AI
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the generated Markdown or plain text from the model.
     *
     * @return response body; empty string when not successful
     */
    public String getText() {
        return text;
    }

    /**
     * Returns a user-safe error message when {@link #isSuccess()} is {@code false}.
     *
     * @return error description, or {@code null} on success
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns the LM provider identifier (e.g. mock, openai).
     *
     * @return provider name; empty when unknown or on generic errors
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Returns the model id used for the request.
     *
     * @return model name; empty when unknown or on generic errors
     */
    public String getModel() {
        return model;
    }

    /**
     * Converts a raw {@link LmResponse} into a servlet-safe output object.
     *
     * @param r language-model response; {@code null} yields a generic error output
     * @return success output with text, or error output with message from the response
     */
    public static AiFeatureOutput fromResponse(LmResponse r) {
        if (r == null) {
            return error("Empty model response.");
        }
        if (!r.isSuccess()) {
            String err = r.getErrorMessage() != null ? r.getErrorMessage() : "Model request failed.";
            return new AiFeatureOutput(false, "", err, r.getProvider(), r.getModel());
        }
        return new AiFeatureOutput(true, r.getText(), null, r.getProvider(), r.getModel());
    }

    /**
     * Creates a failed output with the given message and no provider metadata.
     *
     * @param message user-visible error text
     * @return error {@link AiFeatureOutput}
     */
    public static AiFeatureOutput error(String message) {
        return new AiFeatureOutput(false, "", message, "", "");
    }

    /**
     * Creates an output indicating AI features are turned off in configuration.
     *
     * @return error output with a standard disabled message
     */
    public static AiFeatureOutput disabled() {
        return error("AI features are disabled (LM_ENABLED=false).");
    }
}
