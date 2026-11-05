package com.bupt.ta.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoWorkloadSnapshotTest {

    @Test
    void defaultWarningThresholdMatchesTaStats() {
        MoWorkloadSnapshot snap = new MoWorkloadSnapshot();
        assertEquals(TaWorkloadStats.ASSIGNED_JOBS_WARNING_THRESHOLD, snap.warningThreshold);
    }
}
