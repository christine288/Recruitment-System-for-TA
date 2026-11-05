package com.bupt.ta.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt password hashing with backward-compatible plaintext verification for legacy records.
 *
 * <p>New passwords and registrations are hashed with cost factor 10. Seed/demo accounts in
 * {@code users.json} may still store plaintext; {@link #verify} accepts both BCrypt hashes
 * (prefix {@code $2a$}, {@code $2b$}, {@code $2y$}) and legacy plain strings for coursework migration.
 *
 * <p>This class is not instantiable; use static {@link #hash} and {@link #verify} only.
 */
public final class PasswordHasher {

    private PasswordHasher() {}

    /**
     * Hashes a plaintext password with BCrypt (cost 10).
     *
     * @param plainPassword non-null password
     * @return BCrypt hash string
     * @throws IllegalArgumentException if {@code plainPassword} is null
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null) {
            throw new IllegalArgumentException("Password is required");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
    }

    /**
     * Verifies a plaintext password against a stored value (BCrypt hash or legacy plaintext).
     *
     * @param plainPassword candidate password (may be null → false)
     * @param storedHash    value from {@code users.json}
     * @return {@code true} if the password matches
     */
    public static boolean verify(String plainPassword, String storedHash) {
        if (storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        if (isBcryptHash(storedHash)) {
            return plainPassword != null && BCrypt.checkpw(plainPassword, storedHash);
        }
        return plainPassword != null && plainPassword.equals(storedHash);
    }

    /**
     * Detects BCrypt hash prefix on a stored password value.
     *
     * @param stored value from persistence
     * @return {@code true} if {@code stored} looks like a BCrypt hash
     */
    public static boolean isBcryptHash(String stored) {
        return stored != null
                && (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"));
    }
}
