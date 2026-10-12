package ua.edu.ukma.oss.auth;

import java.util.Set;

/**
 * The authenticated caller as seen by the rest of the application.
 *
 * @param roles  every role the user holds, always including {@link Role#STUDENT}
 * @param orgIds organizations whose content the user may edit
 */
public record AuthPrincipal(Long userId, String fullName, Set<Role> roles, Set<Long> orgIds) {

    public boolean isAdmin() {
        return roles.contains(Role.ADMIN);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }
}
