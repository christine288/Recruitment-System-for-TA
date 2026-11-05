package com.bupt.ta.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LmExceptionTest {

    @Test
    void messageIsPreserved() {
        LmException ex = new LmException("network down");
        assertEquals("network down", ex.getMessage());
    }
}
