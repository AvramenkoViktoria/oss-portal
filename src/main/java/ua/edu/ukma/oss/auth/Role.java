package ua.edu.ukma.oss.auth;

/**
 * Role a user may hold. {@link #STUDENT} is implicit for every account; the others are granted
 * explicitly, per organization except for {@link #ADMIN}.
 */
public enum Role {
    STUDENT,
    SK_MEMBER,
    KS_DELEGATE,
    ADMIN;

    /**
     * Whether holding this role lets the user edit content of the organization it is granted for.
     */
    public boolean editsOrganizationContent() {
        return this == SK_MEMBER || this == KS_DELEGATE;
    }
}
