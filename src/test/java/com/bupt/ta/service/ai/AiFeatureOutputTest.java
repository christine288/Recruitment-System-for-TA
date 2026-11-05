package com.bupt.ta.service.ai;

import com.bupt.ta.ai.LmResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiFeatureOutputTest {

    @Test
    void fromResponse_mapsSuccess() {
        LmResponse r = new LmResponse("Hello", "mock", "m", true, null, null);
        AiFeatureOutput o = AiFeatureOutput.fromResponse(r);
        assertTrue(o.isSuccess());
        assertEquals("Hello", o.getText());
        assertNull(o.getErrorMessage());
        assertEquals("mock", o.getProvider());
    }

    @Test
    void fromResponse_mapsFailure() {
        LmResponse r = new LmResponse("", "http", "gpt", false, "{}", "provider error");
        AiFeatureOutput o = AiFeatureOutput.fromResponse(r);
        assertFalse(o.isSuccess());
        assertEquals("provider error", o.getErrorMessage());
    }

    @Test
    void fromResponse_nullResponse_isError() {
        AiFeatureOutput o = AiFeatureOutput.fromResponse(null);
        assertFalse(o.isSuccess());
        assertTrue(o.getErrorMessage().contains("Empty"));
    }

    @Test
    void disabled_returnsExpectedMessage() {
        AiFeatureOutput o = AiFeatureOutput.disabled();
        assertFalse(o.isSuccess());
        assertTrue(o.getErrorMessage().contains("disabled"));
    }

    @Test
    void error_customMessage() {
        AiFeatureOutput o = AiFeatureOutput.error("custom failure");
        assertFalse(o.isSuccess());
        assertEquals("custom failure", o.getErrorMessage());
    }
}
