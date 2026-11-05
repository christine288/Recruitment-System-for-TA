package com.bupt.ta.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationTest {

    @Test
    void constructor_setsFields() {
        Application a = new Application("a1", "u1", "CS101", "CS101", "TA", "2026-05-01");
        assertEquals("a1", a.id);
        assertEquals("u1", a.userId);
        assertEquals("CS101", a.moduleName);
        assertEquals("CS101", a.moduleCode);
        assertEquals("TA", a.role);
        assertEquals("2026-05-01", a.applicationDate);
    }

    @Test
    void defaultStatusIsPending() {
        Application a = new Application();
        assertEquals("Pending", a.status);
    }
}
