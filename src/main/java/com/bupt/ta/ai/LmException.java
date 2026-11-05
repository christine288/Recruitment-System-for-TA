package com.bupt.ta.ai;

/**
 * Checked exception for failures originating in the language-model integration layer.
 *
 * <p>Thrown by {@link LmClient} implementations (primarily {@link HttpLmClient}) when
 * configuration is incomplete, outbound HTTP requests fail, or request/response JSON
 * cannot be built or parsed. Callers in servlets and AI services should catch this
 * exception and surface a user-safe message without leaking API keys or raw provider
 * payloads.
 *
 * <p>{@link MockLmClient} typically does not throw this exception; it encodes errors
 * in {@link LmResponse} instead (for example when {@link LmConfig#isEnabled()} is
 * {@code false}).
 *
 * @see LmClient#generate(LmRequest)
 * @see LmClient#stream(LmRequest, LmStreamListener)
 */
public class LmException extends Exception {

    /**
     * Creates an exception with a descriptive message and no underlying cause.
     *
     * @param message human-readable description of the failure
     */
    public LmException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a descriptive message and a wrapped cause
     * (for example an I/O or JSON-building failure from {@link HttpLmClient}).
     *
     * @param message human-readable description of the failure
     * @param cause   the underlying throwable that triggered this exception
     */
    public LmException(String message, Throwable cause) {
        super(message, cause);
    }
}
