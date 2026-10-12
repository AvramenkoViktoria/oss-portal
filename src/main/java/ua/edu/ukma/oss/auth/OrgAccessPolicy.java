package ua.edu.ukma.oss.auth;

/**
 * Decides whether a principal may perform an action on an organization.
 */
public interface OrgAccessPolicy {

    boolean allows(AuthPrincipal principal, Long orgId, OrgAction action);
}
