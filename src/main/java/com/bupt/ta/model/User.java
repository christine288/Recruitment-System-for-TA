package com.bupt.ta.model;

/**
 * Registered system user (TA applicant, module owner, or administrator).
 *
 * <p>Persisted in {@code WEB-INF/data/users.json} via {@link com.bupt.ta.service.AuthService}.
 * Public fields are used for JSON binding. Passwords are stored as hashes, never plaintext.
 * New users default to role {@link Roles#TA} and {@link #active} {@code true}.
 *
 * <p>Not thread-safe; account updates should go through the auth service with file-level locking.
 */
public class User {
    /** Unique user identifier (UUID or similar). */
    public String id;
    /** Display name shown in UI and emails. */
    public String name;
    /** BUPT-style student number; may be empty for non-student roles. */
    public String studentId;
    /** Login email address; must be unique across users. */
    public String email;
    /** Hashed password; never expose to clients or JSP. */
    public String passwordHash;
    /**
     * Role constant: {@link Roles#TA}, {@link Roles#MO}, or {@link Roles#ADMIN}.
     * Defaults to {@link Roles#TA}.
     */
    public String role = Roles.TA;
    /**
     * When {@code false}, the user cannot sign in (deactivated by admin).
     * Defaults to {@code true} for new accounts.
     */
    public boolean active = true;

    /** Creates a user with default role {@link Roles#TA} and {@link #active} {@code true}. */
    public User() {}

    /**
     * Creates a user with core account fields; role and active use defaults.
     *
     * @param id           unique user id
     * @param name         display name
     * @param studentId    student number
     * @param email        login email
     * @param passwordHash stored password hash
     */
    public User(String id, String name, String studentId, String email, String passwordHash) {
        this.id           = id;
        this.name         = name;
        this.studentId    = studentId;
        this.email        = email;
        this.passwordHash = passwordHash;
    }
}
