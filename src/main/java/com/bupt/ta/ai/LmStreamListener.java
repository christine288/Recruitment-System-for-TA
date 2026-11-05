package com.bupt.ta.ai;

/**
 * Callback interface for incremental streaming completions from a {@link LmClient}.
 *
 * <p>Implementations receive partial assistant text as it arrives ({@link #onDelta(String)}),
 * a terminal success signal with the resolved model name ({@link #onComplete(String)}), or
 * a terminal error message ({@link #onError(String)}). Used by {@link LmClient#stream(LmRequest, LmStreamListener)}
 * and servlet endpoints that push Server-Sent Events to the browser.
 *
 * <p>{@link HttpLmClient} invokes these callbacks from real SSE lines; {@link MockLmClient}
 * and the default {@link LmClient#stream} implementation simulate streaming by chunking
 * the full {@link LmResponse#getText()}.
 *
 * @see LmClient#stream(LmRequest, LmStreamListener)
 * @see HttpLmClient#stream(LmRequest, LmStreamListener)
 */
public interface LmStreamListener {

    /**
     * Called when a new fragment of generated text is available.
     *
     * @param text incremental assistant content; may be empty but is never {@code null}
     *             from well-behaved {@link LmClient} implementations
     */
    void onDelta(String text);

    /**
     * Called when streaming finishes successfully.
     *
     * @param model the model identifier reported by the provider, or the request/config
     *              fallback model name when the provider does not echo one
     */
    void onComplete(String model);

    /**
     * Called when streaming terminates due to an error.
     *
     * <p>After this callback, {@link #onComplete(String)} must not be invoked.
     *
     * @param message human-readable error description suitable for display or logging
     */
    void onError(String message);
}
