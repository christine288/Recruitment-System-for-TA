package com.bupt.ta.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void hashAndVerify_bcrypt() {
        String hash = PasswordHasher.hash("secret123");
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"));
        assertTrue(PasswordHasher.verify("secret123", hash));
        assertFalse(PasswordHasher.verify("wrong", hash));
    }

    @Test
    void verify_legacyPlaintext() {
        assertTrue(PasswordHasher.verify("admin123", "admin123"));
    }

    @Test
    void hash_producesDifferentSalts() {
        String h1 = PasswordHasher.hash("same");
        String h2 = PasswordHasher.hash("same");
        assertNotEquals(h1, h2);
    }

    @Test
    void verify_nullPassword_false() {
        assertFalse(PasswordHasher.verify(null, "$2a$10$abcdefghijklmnopqrstuv"));
    }

    @Test
    void verify_emptyStored_false() {
        assertFalse(PasswordHasher.verify("x", ""));
        assertFalse(PasswordHasher.verify("x", null));
    }
}
