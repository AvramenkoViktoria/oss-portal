package ua.edu.ukma.oss.auth;

/**
 * Kind of operation checked by {@link OrgAccessPolicy}.
 */
public enum OrgAction {
    /** Meetings, petitions handling, protocols and votings of an organization. */
    EDIT_CONTENT,
    /** Membership and terms of office; reserved for site administrators. */
    MANAGE_COMPOSITION
}
