package com.bupt.ta.service;

import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.testsupport.FileTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuditServiceTest {

    private AuditService audit;

    @BeforeEach
    void setUp() throws Exception {
        ServiceFactory factory = FileTestSupport.newFactory();
        audit = factory.getAuditService();
    }

    @Test
    void log_doesNotThrow() throws Exception {
        audit.log("actor-1", "TEST_ACTION", "USER", "target-1", "details");
    }

    @Test
    void log_multipleEntries() throws Exception {
        audit.log("a1", "APPROVE_APPLICATION", "APPLICATION", "app-1", "");
        audit.log("a2", "REJECT_APPLICATION", "APPLICATION", "app-2", "no fit");
    }

    @Test
    void log_nullDetails_ok() throws Exception {
        audit.log("admin", "DELETE_USER", "USER", "u1", null);
    }
}
