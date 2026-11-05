package com.bupt.ta.ai;

/**
 * Pluggable language-model client contract for the TA recruitment system.
 *
 * <p>{@link LmClient} is the central abstraction of the LM integration layer.
 * Callers (servlets and {@code com.bupt.ta.service.ai} services) build a
 * {@link LmRequest}, invoke {@link #generate(LmRequest)} or {@link #stream(LmRequest, LmStreamListener)},
 * and consume a vendor-neutral {@link LmResponse}. Implementations must not
 * expose provider-specific JSON or HTTP details to callers.
 *
 * <p>Concrete implementations are selected by {@link LmClientFactory} from
 * {@link LmConfig}:
 * <ul>
 *   <li>{@link MockLmClient} — offline, deterministic responses for demos and tests</li>
 *   <li>{@link HttpLmClient} — HTTPS JSON calls to OpenAI-compatible Chat Completions APIs</li>
 * </ul>
 *
 * @see LmClientFactory#create(LmConfig)
 * @see LmConfig
 * @see LmException
 */
public interface LmClient {

    /**
     * Sends a completion request and returns the full model output in one response.
     *
     * @param request unified prompt/messages, sampling parameters, and optional
     *                {@link LmRequest#getFeatureName() feature name}; must not be {@code null}
     * @return a normalised {@link LmResponse} with assistant text, provider id, model name,
     *         success flag, and optional raw payload or error message
     * @throws LmException if configuration is invalid, the HTTP transport fails, or
     *                     request/response parsing fails ({@link HttpLmClient} only)
     */
    LmResponse generate(LmRequest request) throws LmException;

    /**
     * Streams completion text incrementally to the supplied listener.
     *
     * <p>Default implementation delegates to {@link #generate(LmRequest)} and emits
     * fixed-size chunks (48 characters) via {@link LmStreamListener#onDelta(String)}.
     * HTTP providers such as {@link HttpLmClient} should override this method to use
     * real Server-Sent Events (SSE) streaming.
     *
     * @param request  the same {@link LmRequest} shape as {@link #generate(LmRequest)}
     * @param listener callbacks for partial text, completion, and errors; must not be {@code null}
     * @throws LmException if the underlying {@link #generate(LmRequest)} call throws, or if
     *                     streaming setup or I/O fails in overriding implementations
     */
    default void stream(LmRequest request, LmStreamListener listener) throws LmException {
        LmResponse r = generate(request);
        if (r.isSuccess()) {
            String t = r.getText();
            if (t != null && !t.isEmpty()) {
                int step = 48;
                for (int i = 0; i < t.length(); i += step) {
                    listener.onDelta(t.substring(i, Math.min(t.length(), i + step)));
                }
            }
            listener.onComplete(r.getModel());
        } else {
            String err = r.getErrorMessage();
            listener.onError(err != null && !err.isEmpty() ? err : "Generation failed");
        }
    }
}
