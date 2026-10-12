package ua.edu.ukma.oss.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.oss.auth.AuthPrincipal;
import ua.edu.ukma.oss.auth.OrgAction;
import ua.edu.ukma.oss.auth.Role;

class RoleBasedOrgAccessPolicyTest {

    private final RoleBasedOrgAccessPolicy policy = new RoleBasedOrgAccessPolicy();

    private static AuthPrincipal principal(Set<Role> roles, Set<Long> orgIds) {
        return new AuthPrincipal(1L, "Test", roles, orgIds);
    }

    @Test
    void memberEditsOnlyOwnOrganization() {
        AuthPrincipal skMember = principal(Set.of(Role.STUDENT, Role.SK_MEMBER), Set.of(10L));

        assertThat(policy.allows(skMember, 10L, OrgAction.EDIT_CONTENT)).isTrue();
        assertThat(policy.allows(skMember, 20L, OrgAction.EDIT_CONTENT)).isFalse();
    }

    @Test
    void studentCannotEditAnything() {
        AuthPrincipal student = principal(Set.of(Role.STUDENT), Set.of());

        assertThat(policy.allows(student, 10L, OrgAction.EDIT_CONTENT)).isFalse();
        assertThat(policy.allows(student, 10L, OrgAction.MANAGE_COMPOSITION)).isFalse();
    }

    @Test
    void adminManagesCompositionButDoesNotEditContent() {
        AuthPrincipal admin = principal(Set.of(Role.STUDENT, Role.ADMIN), Set.of());

        assertThat(policy.allows(admin, 10L, OrgAction.MANAGE_COMPOSITION)).isTrue();
        assertThat(policy.allows(admin, 10L, OrgAction.EDIT_CONTENT)).isFalse();
    }

    @Test
    void anonymousIsDenied() {
        assertThat(policy.allows(null, 10L, OrgAction.EDIT_CONTENT)).isFalse();
    }
}
