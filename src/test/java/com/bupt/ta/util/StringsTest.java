package com.bupt.ta.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringsTest {

    @Test
    void nullToEmpty_nullReturnsEmpty() {
        assertEquals("", Strings.nullToEmpty(null));
    }

    @Test
    void nullToEmpty_nonNullUnchanged() {
        assertEquals("hello", Strings.nullToEmpty("hello"));
    }

    @Test
    void firstNonBlank_allNull_returnsEmpty() {
        assertEquals("", Strings.firstNonBlank((String[]) null));
        assertEquals("", Strings.firstNonBlank(null, null));
    }

    @Test
    void firstNonBlank_returnsFirstNonBlank() {
        assertEquals("middle", Strings.firstNonBlank(null, "  middle  ", "last"));
    }

    @Test
    void firstNonBlank_allBlank_returnsEmpty() {
        assertEquals("", Strings.firstNonBlank(" ", "\t", ""));
    }
}
