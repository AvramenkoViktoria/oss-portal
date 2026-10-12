package ua.edu.ukma.oss.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.support.IntegrationTest;

@IntegrationTest
@Transactional
class AuthServiceIT {

    @Autowired
    UserService users;
    @Autowired
    AuthService auth;
    @Autowired
    OrganizationService organizations;

    private String newEmail() {
        return "user-" + UUID.randomUUID() + "@ukma.edu.ua";
    }

    @Test
    void registeredStudentCanLoginAndGetsStudentRoleOnly() {
        String email = newEmail();
        users.register(email, "s3cret-pass", "Ivan Student");

        AuthPrincipal principal = auth.authenticate(auth.login(email, "s3cret-pass")).orElseThrow();

        assertThat(principal.fullName()).isEqualTo("Ivan Student");
        assertThat(principal.roles()).containsExactly(Role.STUDENT);
        assertThat(principal.orgIds()).isEmpty();
    }

    @Test
    void wrongPasswordAndUnknownEmailFailTheSameWay() {
        String email = newEmail();
        users.register(email, "s3cret-pass", "Ivan Student");

        assertThatThrownBy(() -> auth.login(email, "wrong")).isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> auth.login("nobody@ukma.edu.ua", "x")).isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void emailIsCaseInsensitiveAndUnique() {
        String email = newEmail();
        users.register(email, "s3cret-pass", "Ivan");

        assertThat(auth.login(email.toUpperCase(), "s3cret-pass")).isNotBlank();
        assertThatThrownBy(() -> users.register(email.toUpperCase(), "s3cret-pass", "Dup"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void grantedRoleShowsUpImmediatelyAndRevokeRemovesIt() {
        Long sk = organizations.getByCode(OrgCode.SK).id();
        String email = newEmail();
        Long id = users.register(email, "s3cret-pass", "Olena").id();
        String token = auth.login(email, "s3cret-pass");

        UserDto granted = users.grant(id, Role.SK_MEMBER, sk);
        AuthPrincipal after = auth.authenticate(token).orElseThrow();

        assertThat(auth.canEdit(after, sk)).isTrue();
        assertThat(auth.canEdit(after, organizations.getByCode(OrgCode.KS).id())).isFalse();

        users.revoke(granted.grants().getFirst().id());
        assertThat(auth.canEdit(auth.authenticate(token).orElseThrow(), sk)).isFalse();
    }

    @Test
    void disabledAccountCannotUseExistingToken() {
        String email = newEmail();
        Long id = users.register(email, "s3cret-pass", "Petro").id();
        String token = auth.login(email, "s3cret-pass");

        users.setActive(id, false);

        assertThat(auth.authenticate(token)).isEmpty();
        assertThatThrownBy(() -> auth.login(email, "s3cret-pass")).isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void roleScopeRulesAreEnforced() {
        Long id = users.register(newEmail(), "s3cret-pass", "Anna").id();

        assertThatThrownBy(() -> users.grant(id, Role.SK_MEMBER, null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> users.grant(id, Role.STUDENT, null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> users.register(newEmail(), "short", "Anna")).isInstanceOf(BusinessRuleException.class);
    }
}
