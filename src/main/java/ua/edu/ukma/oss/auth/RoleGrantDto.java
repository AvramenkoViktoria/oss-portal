package ua.edu.ukma.oss.auth;

/**
 * A role granted to a user.
 *
 * @param orgId {@code null} means the grant is not tied to one organization (administrators)
 */
public record RoleGrantDto(Long id, Role role, Long orgId) {
}
