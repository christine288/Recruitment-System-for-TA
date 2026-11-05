package com.bupt.ta.ai;

import java.util.logging.Logger;

/**
 * Factory for creating the appropriate {@link LmClient} implementation from {@link LmConfig}.
 *
 * <p>This is the wiring entry point of the LM integration layer. Application code should
 * load {@link LmConfig} once (via {@link LmConfig#load}) and call {@link #create(LmConfig)}
 * rather than instantiating {@link HttpLmClient} or {@link MockLmClient} directly.
 *
 * <p><strong>Selection rules:</strong>
 * <ul>
 *   <li>{@code null} config → error</li>
 *   <li>{@link LmConfig#isEnabled()} {@code false} → {@link MockLmClient} with
 *       {@code forceDisabled=true} (AI reported as disabled)</li>
 *   <li>{@link LmProviderType#MOCK} → {@link MockLmClient}, only when explicitly selected</li>
 *   <li>HTTP providers → {@link HttpLmClient}; missing settings fail on request</li>
 * </ul>
 *
 * @see LmConfig
 * @see LmClient
 */
public final class LmClientFactory {
    private static final Logger LOG = Logger.getLogger(LmClientFactory.class.getName());

    private LmClientFactory() {}

    /**
     * Creates an {@link LmClient} matching the supplied configuration.
     *
     * @param config LM settings from {@link LmConfig#load}; must not be {@code null}
     * @return a non-null {@link LmClient} ready for {@link LmClient#generate(LmRequest)} or
     *         {@link LmClient#stream(LmRequest, LmStreamListener)}
     */
    public static LmClient create(LmConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("LmConfig must not be null.");
        }
        if (!config.isEnabled()) {
            LOG.info("LM_ENABLED=false — mock client will report AI as disabled.");
            return new MockLmClient(true);
        }
        switch (config.getProviderType()) {
            case MOCK:
                return new MockLmClient();
            case DOUBAO:
            case OPENAI:
            case CUSTOM:
                return new HttpLmClient(config);
        }
        throw new IllegalStateException("Unsupported LM provider: " + config.getProviderType());
    }
}
