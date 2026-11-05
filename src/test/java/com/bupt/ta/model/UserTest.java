package com.bupt.ta.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void constructor_setsFields() {
        User u = new User("id1", "Alice", "2021000001", "alice@bupt.edu.cn", "hash");
        assertEquals("id1", u.id);
        assertEquals("Alice", u.name);
        assertEquals("2021000001", u.studentId);
        assertEquals("alice@bupt.edu.cn", u.email);
        assertEquals("hash", u.passwordHash);
    }

    @Test
    void defaultRoleIsTaAndActive() {
        User u = new User();
        assertEquals(Roles.TA, u.role);
        assertTrue(u.active);
    }
}
