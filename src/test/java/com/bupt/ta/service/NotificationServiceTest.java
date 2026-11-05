package com.bupt.ta.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class NotificationServiceTest {

    @Test
    void isConfigured_withoutEnv_false() {
        NotificationService svc = new NotificationService();
        assertFalse(svc.isConfigured());
    }

    @Test
    void sendStatusChangeEmail_noSmtp_noThrow() {
        NotificationService svc = new NotificationService();
        svc.sendStatusChangeEmail("test@bupt.edu.cn", "Test", "CS101", "Accepted", "Welcome");
    }

    @Test
    void sendPlainText_noSmtp_noThrow() {
        NotificationService svc = new NotificationService();
        svc.sendPlainText("test@bupt.edu.cn", "Subject", "Body");
    }
}
