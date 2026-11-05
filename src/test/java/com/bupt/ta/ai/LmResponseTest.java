package com.bupt.ta.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LmResponseTest {

    @Test
    void successResponse() {
        LmResponse r = new LmResponse("hello", "mock", "m1", true, null, null);
        assertTrue(r.isSuccess());
        assertEquals("hello", r.getText());
        assertEquals("mock", r.getProvider());
        assertEquals("m1", r.getModel());
    }

    @Test
    void failureResponse() {
        LmResponse r = new LmResponse("", "mock", "m1", false, null, "error msg");
        assertFalse(r.isSuccess());
        assertEquals("error msg", r.getErrorMessage());
    }

    @Test
    void nullText_becomesEmpty() {
        LmResponse r = new LmResponse(null, null, null, true, null, null);
        assertEquals("", r.getText());
        assertEquals("", r.getProvider());
    }
}
