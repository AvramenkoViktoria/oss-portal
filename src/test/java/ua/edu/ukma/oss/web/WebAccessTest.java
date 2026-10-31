package ua.edu.ukma.oss.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import ua.edu.ukma.oss.auth.AuthPrincipal;
import ua.edu.ukma.oss.auth.AuthService;
import ua.edu.ukma.oss.auth.Role;
import ua.edu.ukma.oss.organization.Messenger;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.shared.NotFoundException;

class WebAccessTest {

    private final OrganizationService organizations = mock(OrganizationService.class);
    private final AuthService auth = mock(AuthService.class);
    private final WebAccess access = new WebAccess(organizations, auth);
    private final OrganizationDto sk = new OrganizationDto(1L, OrgCode.SK, "СК", Messenger.TELEGRAM, "c");
    private final AuthPrincipal user = new AuthPrincipal(5L, "Студент", Set.of(Role.STUDENT), Set.of());

    @Test
    void resolvesBodyByCaseInsensitiveCode() {
        when(organizations.getByCode(OrgCode.SK)).thenReturn(sk);

        assertThat(access.org("sk")).isEqualTo(sk);
        assertThat(access.org("SK")).isEqualTo(sk);
    }

    @Test
    void unknownCodeIsNotFound() {
        assertThatThrownBy(() -> access.org("xx")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void requireEditorDeniesNonEditors() {
        when(organizations.getByCode(OrgCode.SK)).thenReturn(sk);

        assertThatThrownBy(() -> access.requireEditor(user, "sk")).isInstanceOf(AccessDeniedException.class);

        when(auth.canEdit(user, 1L)).thenReturn(true);
        assertThat(access.requireEditor(user, "sk")).isEqualTo(sk);
    }

    @Test
    void anonymousCannotEdit() {
        when(auth.canEdit(user, 1L)).thenReturn(true);

        assertThat(access.canEdit(null, 1L)).isFalse();
        assertThat(access.canEdit(user, 1L)).isTrue();
        assertThat(access.canEdit(user, 2L)).isFalse();
    }
}
