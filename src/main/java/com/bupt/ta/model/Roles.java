package com.bupt.ta.model;

/**
 * Canonical user role string constants stored on {@link User#role}.
 *
 * <p>Used by authentication, servlet authorization, and registration defaults. Values are
 * persisted verbatim in {@code WEB-INF/data/users.json}. This class is not instantiable.
 *
 * <p>Thread-safe: all members are immutable static constants.
 */
public class Roles {
    /** Teaching assistant (student applicant) role. */
    public static final String TA    = "TA";
    /** System administrator with full management access. */
    public static final String ADMIN = "ADMIN";
    /** Module owner who creates and manages job postings. */
    public static final String MO    = "MO";

    private Roles() {}
}
