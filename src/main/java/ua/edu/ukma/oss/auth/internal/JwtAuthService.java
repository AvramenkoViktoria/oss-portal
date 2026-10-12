package ua.edu.ukma.oss.auth.internal;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.auth.AuthPrincipal;
import ua.edu.ukma.oss.auth.AuthService;
import ua.edu.ukma.oss.auth.AuthenticationFailedException;
import ua.edu.ukma.oss.auth.OrgAccessPolicy;
import ua.edu.ukma.oss.auth.OrgAction;
import ua.edu.ukma.oss.auth.Role;

@Service
@Transactional(readOnly = true)
class JwtAuthService implements AuthService {

    private final UserRepository users;
    private final UserRoleGrantRepository grants;
    private final JwtTokenProvider tokens;
    private final PasswordEncoder encoder;
    private final OrgAccessPolicy policy;
    /** Hash of an unrelated string; verified against for unknown emails to equalize timing. */
    private final String dummyHash;

    JwtAuthService(UserRepository users, UserRoleGrantRepository grants, JwtTokenProvider tokens,
                   PasswordEncoder encoder, OrgAccessPolicy policy) {
        this.users = users;
        this.grants = grants;
        this.tokens = tokens;
        this.encoder = encoder;
        this.policy = policy;
        this.dummyHash = encoder.encode("timing-equalizer");
    }

    @Override
    public String login(String email, String password) {
        User user = users.findByEmail(normalize(email)).orElse(null);
        // Hash against a dummy value for unknown emails so response time does not reveal which exist.
        String hash = user != null ? user.getPasswordHash() : dummyHash;
        boolean matches = encoder.matches(password == null ? "" : password, hash);
        if (user == null || !matches || !user.isActive()) {
            throw new AuthenticationFailedException();
        }
        return tokens.issue(user.getId());
    }

    @Override
    public Optional<AuthPrincipal> authenticate(String token) {
        return tokens.parse(token)
                .flatMap(users::findById)
                .filter(User::isActive)
                .map(user -> toPrincipal(user, grants.findByUserId(user.getId())));
    }

    @Override
    public boolean canEdit(AuthPrincipal principal, Long orgId) {
        return policy.allows(principal, orgId, OrgAction.EDIT_CONTENT);
    }

    static AuthPrincipal toPrincipal(User user, List<UserRoleGrant> userGrants) {
        Set<Role> roles = EnumSet.of(Role.STUDENT);
        Set<Long> orgIds = new HashSet<>();
        for (UserRoleGrant grant : userGrants) {
            roles.add(grant.getRole());
            if (grant.getRole().editsOrganizationContent() && grant.getOrgId() != null) {
                orgIds.add(grant.getOrgId());
            }
        }
        return new AuthPrincipal(user.getId(), user.getFullName(), Set.copyOf(roles), Set.copyOf(orgIds));
    }

    static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
