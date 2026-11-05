package com.bupt.ta.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LmProviderTypeTest {

    @Test
    void fromString_defaultsToDoubao() {
        assertEquals(LmProviderType.DOUBAO, LmProviderType.fromString(null));
        assertEquals(LmProviderType.DOUBAO, LmProviderType.fromString(""));
    }

    @Test
    void fromString_rejectsUnknownProvider() {
        assertThrows(IllegalArgumentException.class, () -> LmProviderType.fromString("unknown"));
    }

    @Test
    void fromString_recognisesVendorsCaseInsensitive() {
        assertEquals(LmProviderType.MOCK, LmProviderType.fromString("mock"));
        assertEquals(LmProviderType.MOCK, LmProviderType.fromString("MOCK"));
        assertEquals(LmProviderType.DOUBAO, LmProviderType.fromString("doubao"));
        assertEquals(LmProviderType.DOUBAO, LmProviderType.fromString("DOUBAO"));
        assertEquals(LmProviderType.OPENAI, LmProviderType.fromString("openai"));
        assertEquals(LmProviderType.CUSTOM, LmProviderType.fromString("custom"));
    }
}
