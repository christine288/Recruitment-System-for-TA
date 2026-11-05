package com.bupt.ta.util;

/**
 * Small string helpers used across servlets and services (no external dependencies).
 *
 * <p>Thread-safe: all methods are stateless pure functions.
 */
public final class Strings {

    private Strings() {}

    /**
     * Null-safe string coalescing to empty string.
     *
     * @param s input string, possibly {@code null}
     * @return {@code s} unchanged, or empty string when {@code s} is {@code null}
     */
    public static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * Returns the first argument that is non-null and non-blank after trimming.
     *
     * @param parts variable arguments tested in order; may be {@code null} (yields {@code ""})
     * @return first trimmed non-blank part, or {@code ""} if none qualify
     */
    public static String firstNonBlank(String... parts) {
        if (parts == null) {
            return "";
        }
        for (String p : parts) {
            if (p != null && !p.trim().isEmpty()) {
                return p.trim();
            }
        }
        return "";
    }
}
