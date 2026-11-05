package com.bupt.ta.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class LmMessageTest {

    @Test
    void constructor_setsRoleAndContent() {
        LmMessage m = new LmMessage("user", "hello");
        assertEquals("user", m.role);
        assertEquals("hello", m.content);
    }

    @Test
    void constructor_nullRoleDefaultsToUser() {
        LmMessage m = new LmMessage(null, "hi");
        assertEquals("user", m.role);
        assertEquals("hi", m.content);
    }

    @Test
    void equals_sameRoleAndContent() {
        LmMessage a = new LmMessage("assistant", "ok");
        LmMessage b = new LmMessage("assistant", "ok");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentContent() {
        assertNotEquals(new LmMessage("user", "a"), new LmMessage("user", "b"));
    }
}
