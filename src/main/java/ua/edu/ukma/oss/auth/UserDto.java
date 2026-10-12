package ua.edu.ukma.oss.auth;

import java.util.List;

/**
 * Read model of a user account; never exposes the password hash.
 */
public record UserDto(Long id, String email, String fullName, boolean active, List<RoleGrantDto> grants) {
}
