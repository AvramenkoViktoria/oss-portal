package ua.edu.ukma.oss.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import ua.edu.ukma.oss.auth.AuthPrincipal;
import ua.edu.ukma.oss.auth.Role;
import ua.edu.ukma.oss.auth.RoleGrantDto;
import ua.edu.ukma.oss.auth.UserDto;
import ua.edu.ukma.oss.auth.UserService;
import ua.edu.ukma.oss.organization.AddMemberCmd;
import ua.edu.ukma.oss.organization.MembershipDto;
import ua.edu.ukma.oss.organization.Messenger;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.shared.BusinessRuleException;

class AdminControllerTest {

    private final UserService users = mock(UserService.class);
    private final OrganizationService organizations = mock(OrganizationService.class);
    private final WebAccess access = mock(WebAccess.class);
    private final AdminController controller = new AdminController(users, organizations, access);
    private final AuthPrincipal self = new AuthPrincipal(1L, "Адмін", Set.of(Role.ADMIN), Set.of());
    private final RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    private final OrganizationDto ks = new OrganizationDto(2L, OrgCode.KS, "КС", Messenger.TELEGRAM, "c");
    private final OrganizationDto sk = new OrganizationDto(3L, OrgCode.SK, "СК", Messenger.TELEGRAM, "c");

    private UserDto user(long id, RoleGrantDto... grants) {
        return new UserDto(id, "u" + id + "@ukma.edu.ua", "Користувач " + id, true, List.of(grants));
    }

    @Test
    void usersPageListsAccountsAndBodies() {
        when(users.all()).thenReturn(List.of(user(1)));
        ConcurrentModel model = new ConcurrentModel();

        assertThat(controller.users(model)).isEqualTo("admin/users");
        assertThat(model).containsKeys("users", "orgs");
    }

    @Test
    void grantsAdministratorRole() {
        assertThat(controller.setAdmin(5L, true, self, redirect)).isEqualTo("redirect:/admin");

        verify(users).grant(5L, Role.ADMIN, null);
        assertThat(redirect.getFlashAttributes()).containsKey("success");
    }

    @Test
    void revokesOnlyAdministratorGrantsOfAnotherUser() {
        when(users.get(5L)).thenReturn(user(5, new RoleGrantDto(10L, Role.ADMIN, null),
                new RoleGrantDto(11L, Role.KS_DELEGATE, 2L)));

        controller.setAdmin(5L, false, self, redirect);

        verify(users).revoke(10L);
        verify(users, never()).revoke(11L);
    }

    @Test
    void administratorCannotRevokeOwnRole() {
        assertThatThrownBy(() -> controller.setAdmin(1L, false, self, redirect))
                .isInstanceOf(BusinessRuleException.class);

        verify(users, never()).revoke(any());
    }

    @Test
    void togglesAccountActivity() {
        controller.setActive(5L, false, redirect);
        assertThat(redirect.getFlashAttributes().get("success")).isEqualTo("Обліковий запис вимкнено");

        controller.setActive(5L, true, redirect);
        assertThat(redirect.getFlashAttributes().get("success")).isEqualTo("Обліковий запис увімкнено");
        verify(users).setActive(5L, true);
    }

    @Test
    void compositionPageShowsHistoryWithNames() {
        when(access.org("ks")).thenReturn(ks);
        when(organizations.history(2L)).thenReturn(List.of());
        when(users.all()).thenReturn(List.of(user(1), user(2)));
        ConcurrentModel model = new ConcurrentModel();

        assertThat(controller.composition("ks", model)).isEqualTo("admin/org");
        assertThat(model).containsKeys("org", "history", "names", "users", "today", "messengers");
    }

    @Test
    void addMemberRequiresFacultyAndPosition() {
        when(access.org("ks")).thenReturn(ks);

        assertThatThrownBy(() -> controller.addMember("ks", 5L, " ", "Член", LocalDate.now(), null, false, redirect))
                .isInstanceOf(BusinessRuleException.class);
        verify(organizations, never()).addMember(any());
    }

    @Test
    void addMemberGrantsEditorRoleOfTheBody() {
        when(access.org("ks")).thenReturn(ks);
        when(access.org("sk")).thenReturn(sk);
        LocalDate start = LocalDate.of(2026, 9, 1);

        controller.addMember("ks", 5L, " ФІ ", " Делегат ", start, null, true, redirect);
        controller.addMember("sk", 6L, "ФІ", "Член", start, null, true, redirect);
        controller.addMember("sk", 7L, "ФІ", "Член", start, null, false, redirect);

        verify(organizations).addMember(new AddMemberCmd(5L, 2L, "ФІ", "Делегат", start, null));
        verify(users).grant(5L, Role.KS_DELEGATE, 2L);
        verify(users).grant(6L, Role.SK_MEMBER, 3L);
        verify(users, never()).grant(7L, Role.SK_MEMBER, 3L);
    }

    @Test
    void endingTermRevokesEditorRoleOfThatBodyOnly() {
        when(access.org("ks")).thenReturn(ks);
        LocalDate end = LocalDate.of(2026, 10, 1);
        when(organizations.membership(20L)).thenReturn(
                new MembershipDto(20L, 5L, 2L, "ФІ", "Делегат", LocalDate.of(2026, 1, 1), null));
        when(users.get(5L)).thenReturn(user(5, new RoleGrantDto(10L, Role.KS_DELEGATE, 2L),
                new RoleGrantDto(11L, Role.KS_DELEGATE, 9L), new RoleGrantDto(12L, Role.ADMIN, null)));

        assertThat(controller.endTerm("ks", 20L, end, redirect)).isEqualTo("redirect:/admin/orgs/ks");

        verify(organizations).endTerm(20L, end);
        verify(users).revoke(10L);
        verify(users, never()).revoke(11L);
        verify(users, never()).revoke(12L);
    }

    @Test
    void endingTermOfAnotherBodyIsRejected() {
        when(access.org("ks")).thenReturn(ks);
        when(organizations.membership(20L)).thenReturn(
                new MembershipDto(20L, 5L, 3L, "ФІ", "Член", LocalDate.of(2026, 1, 1), null));

        assertThatThrownBy(() -> controller.endTerm("ks", 20L, LocalDate.now(), redirect))
                .isInstanceOf(BusinessRuleException.class);
        verify(organizations, never()).endTerm(any(), any());
    }

    @Test
    void channelIsTrimmedAndMustNotBeBlank() {
        when(access.org("ks")).thenReturn(ks);

        assertThatThrownBy(() -> controller.channel("ks", Messenger.TELEGRAM, " ", redirect))
                .isInstanceOf(BusinessRuleException.class);

        controller.channel("ks", Messenger.WHATSAPP, " 123 ", redirect);
        verify(organizations).updateChannel(2L, Messenger.WHATSAPP, "123");
    }
}
