package ua.edu.ukma.oss.auth;

import java.util.Optional;

/**
 * Login, token validation and authorization checks.
 */
public interface AuthService {

    /**
     * Verifies credentials and returns a signed token.
     *
     * @throws AuthenticationFailedException when the credentials are wrong or the account is disabled
     */
    String login(String email, String password);

    /**
     * Resolves a token to a principal with the roles the user holds right now, or empty if the token
     * is malformed, expired or belongs to a disabled account.
     */
    Optional<AuthPrincipal> authenticate(String token);

    /**
     * Whether the principal may edit content of the organization.
     */
    boolean canEdit(AuthPrincipal principal, Long orgId);
}
