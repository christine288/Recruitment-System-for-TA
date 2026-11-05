package com.bupt.ta.ai;

/**
 * Vendor-neutral result of a language-model completion request.
 *
 * <p>{@link LmResponse} is the standard return type of {@link LmClient#generate(LmRequest)}.
 * Servlet and service layers should consume only this shape—assistant text, provider label,
 * model name, success flag, and optional diagnostic fields—without parsing provider JSON.
 *
 * <p>On success, {@link #isSuccess()} is {@code true} and {@link #getText()} holds the
 * assistant output. On failure, {@link #isSuccess()} is {@code false} and
 * {@link #getErrorMessage()} explains why; {@link #getRawResponse()} may still contain
 * the provider payload for debugging ({@link HttpLmClient}).
 *
 * @see LmClient#generate(LmRequest)
 * @see MockLmClient
 * @see HttpLmClient
 */
public final class LmResponse {
    private final String text;
    private final String provider;
    private final String model;
    private final boolean success;
    private final String rawResponse;
    private final String errorMessage;

    /**
     * Constructs a normalised LM response.
     *
     * @param text         assistant-generated text; {@code null} is stored as empty string
     * @param provider     short provider label (for example {@code "mock"} or {@code "http"})
     * @param model        model identifier used or reported for this completion
     * @param success      {@code true} when generation succeeded and {@code text} is meaningful
     * @param rawResponse  optional unparsed provider body for diagnostics; may be {@code null}
     * @param errorMessage human-readable failure reason when {@code success} is {@code false};
     *                     may be {@code null} on success
     */
    public LmResponse(String text, String provider, String model, boolean success,
                      String rawResponse, String errorMessage) {
        this.text = text != null ? text : "";
        this.provider = provider != null ? provider : "";
        this.model = model != null ? model : "";
        this.success = success;
        this.rawResponse = rawResponse;
        this.errorMessage = errorMessage;
    }

    /**
     * Returns the generated assistant text.
     *
     * @return completion text; empty string when none was produced or on some failure paths
     */
    public String getText() {
        return text;
    }

    /**
     * Returns a short provider identifier for logging and UI labelling.
     *
     * @return provider label (for example {@code "mock"} from {@link MockLmClient} or
     *         {@code "http"} from {@link HttpLmClient})
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Returns the model name associated with this completion.
     *
     * @return model identifier from the request, {@link LmConfig}, provider response, or fallback
     */
    public String getModel() {
        return model;
    }

    /**
     * Indicates whether the completion succeeded.
     *
     * @return {@code true} when assistant text was produced; {@code false} when disabled,
     *         credentials missing, HTTP error, or parsing failure occurred
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the raw provider response body when available.
     *
     * <p>Intended for debugging and error diagnosis in {@link HttpLmClient}; typically
     * {@code null} for {@link MockLmClient} successes.
     *
     * @return unparsed JSON or stream payload, or {@code null} if not captured
     */
    public String getRawResponse() {
        return rawResponse;
    }

    /**
     * Returns a human-readable error message when {@link #isSuccess()} is {@code false}.
     *
     * @return error description suitable for user display or logging, or {@code null} on success
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}
