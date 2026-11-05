package com.bupt.ta.ai;

import com.bupt.ta.testsupport.LmTestSupport;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletContext;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link LmClientFactory#create(LmConfig)} — explicit mock vs real HTTP providers.
 */
class LmClientFactoryTest {

    @Test
    void nullConfig_isAConfigurationError() {
        assertThrows(IllegalArgumentException.class, () -> LmClientFactory.create(null));
    }

    @Test
    void disabled_returnsMock() {
        ServletContext ctx = LmTestSupport.servletContextWithLmProperties("LM_ENABLED=false\n");
        LmConfig cfg = LmConfig.load(ctx);
        assertInstanceOf(MockLmClient.class, LmClientFactory.create(cfg));
    }

    @Test
    void providerMock_returnsMock() {
        ServletContext ctx = LmTestSupport.servletContextWithLmProperties("LM_PROVIDER=mock\n");
        LmConfig cfg = LmConfig.load(ctx);
        assertInstanceOf(MockLmClient.class, LmClientFactory.create(cfg));
    }

    @Test
    void providerDoubao_withoutCredentials_remainsHttpClient() {
        ServletContext ctx = LmTestSupport.servletContextWithLmProperties(
                "LM_PROVIDER=doubao\nLM_API_KEY=\nLM_MODEL=ep-test\n");
        LmConfig cfg = LmConfig.load(ctx);
        assertInstanceOf(HttpLmClient.class, LmClientFactory.create(cfg));
    }

    @Test
    void defaultProvider_withCredentials_returnsHttpClient() {
        ServletContext ctx = LmTestSupport.servletContextWithLmProperties(
                "LM_API_KEY=ark-test-key\nLM_MODEL=ep-test\n");
        LmConfig cfg = LmConfig.load(ctx);
        assertInstanceOf(HttpLmClient.class, LmClientFactory.create(cfg));
    }
}
