package com.bupt.ta.service;

import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.testsupport.FileTestSupport;
import com.bupt.ta.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordResetServiceTest {

    private ServiceFactory factory;
    private PasswordResetService reset;
    private AuthService auth;

    @BeforeEach
    void setUp() throws Exception {
        factory = FileTestSupport.newFactory();
        reset = factory.getPasswordResetService();
        auth = factory.getAuthService();
        auth.register("Reset User", TestFixtures.validBuptStudentId(), "reset@test.local", "oldpass");
    }

    @Test
    void createTokenForEmail_unknownEmail_empty() throws Exception {
        assertFalse(reset.createTokenForEmail("nobody@test.local").isPresent());
    }

    @Test
    void createTokenForEmail_knownUser_returnsToken() throws Exception {
        assertTrue(reset.createTokenForEmail("reset@test.local").isPresent());
    }

    @Test
    void resetPassword_validToken_changesPassword() throws Exception {
        String token = reset.createTokenForEmail("reset@test.local").orElseThrow();
        assertTrue(reset.isValidToken(token));
        assertTrue(reset.resetPassword(token, "newpass456"));
        assertTrue(auth.login("reset@test.local", "newpass456").isPresent());
        assertFalse(auth.login("reset@test.local", "oldpass").isPresent());
    }

    @Test
    void resetPassword_invalidToken_false() throws Exception {
        assertFalse(reset.resetPassword("bad-token", "x"));
    }

    @Test
    void isValidToken_afterReset_false() throws Exception {
        String token = reset.createTokenForEmail("reset@test.local").orElseThrow();
        reset.resetPassword(token, "onceOnly");
        assertFalse(reset.isValidToken(token));
    }
}
