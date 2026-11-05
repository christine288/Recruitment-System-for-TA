package com.bupt.ta.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaWorkloadStatsTest {

    @Test
    void formatAcceptedLine_allFields() {
        Application a = new Application("a1", "u1", "CS101", "CS101", "Lab TA", "2026-05-01");
        String line = TaWorkloadStats.formatAcceptedLine(a);
        assertTrue(line.contains("CS101"));
        assertTrue(line.contains("[CS101]"));
        assertTrue(line.contains("Lab TA"));
        assertTrue(line.contains("2026-05-01"));
    }

    @Test
    void formatAcceptedLine_partialFields() {
        Application a = new Application();
        a.moduleName = "Math";
        String line = TaWorkloadStats.formatAcceptedLine(a);
        assertEquals("Math", line);
    }

    @Test
    void formatAcceptedLine_nullApplication_empty() {
        assertEquals("", TaWorkloadStats.formatAcceptedLine(null));
    }

    @Test
    void warningThreshold_isThree() {
        assertEquals(3, TaWorkloadStats.ASSIGNED_JOBS_WARNING_THRESHOLD);
    }
}
