package ua.edu.ukma.oss.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import ua.edu.ukma.oss.auth.Role;
import ua.edu.ukma.oss.auth.UserDto;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.shared.NotFoundException;

class UserServiceImplTest {

    private final UserRepository users = mock(UserRepository.class);
    private final UserRoleGrantRepository grants = mock(UserRoleGrantRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final UserServiceImpl service = new UserServiceImpl(users, grants, encoder);

    private User user(long id, String email, String name) {
        User u = new User(email, "hash", name);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    @Test
    void registersWithNormalisedEmailAndEncodedPassword() {
        when(encoder.encode("s3cret-pass")).thenReturn("encoded");
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto dto = service.register("  Some@UKMA.edu.ua ", "s3cret-pass", "  Ім’я  ");

        assertThat(dto.email()).isEqualTo("some@ukma.edu.ua");
        assertThat(dto.fullName()).isEqualTo("Ім’я");
        assertThat(dto.grants()).isEmpty();
    }

    @Test
    void rejectsInvalidRegistrations() {
        assertThatThrownBy(() -> service.register("no-at", "s3cret-pass", "A")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.register("a@b.ua", "s3cret-pass", " ")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.register("a@b.ua", "short", "A")).isInstanceOf(BusinessRuleException.class);
        when(users.existsByEmail("a@b.ua")).thenReturn(true);
        assertThatThrownBy(() -> service.register("a@b.ua", "s3cret-pass", "A"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("вже існує");
        verify(users, never()).save(any());
    }

    @Test
    void getReturnsUserWithGrantsOrThrows() {
        when(users.findById(1L)).thenReturn(Optional.of(user(1, "a@b.ua", "A")));
        when(grants.findByUserId(1L)).thenReturn(List.of(new UserRoleGrant(1L, Role.ADMIN, null)));

        assertThat(service.get(1L).grants()).hasSize(1);
        assertThatThrownBy(() -> service.get(2L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void allIsSortedByNameIgnoringCase() {
        when(users.findAll()).thenReturn(List.of(user(1, "b@b.ua", "борис"), user(2, "a@b.ua", "Анна")));
        when(grants.findAll()).thenReturn(List.of(new UserRoleGrant(2L, Role.ADMIN, null)));

        List<UserDto> all = service.all();

        assertThat(all).extracting(UserDto::fullName).containsExactly("Анна", "борис");
        assertThat(all.get(0).grants()).hasSize(1);
        assertThat(all.get(1).grants()).isEmpty();
    }

    @Test
    void fullNamesMapsIdsToNames() {
        when(users.findAllById(List.of(1L))).thenReturn(List.of(user(1, "a@b.ua", "Анна")));

        assertThat(service.fullNames(List.of(1L))).containsEntry(1L, "Анна");
    }

    @Test
    void grantValidatesRoleAndOrganisation() {
        assertThatThrownBy(() -> service.grant(1L, Role.STUDENT, null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.grant(1L, Role.SK_MEMBER, null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.grant(1L, Role.ADMIN, 5L)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.grant(9L, Role.ADMIN, null)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void grantSavesOnlyNewGrants() {
        when(users.findById(1L)).thenReturn(Optional.of(user(1, "a@b.ua", "A")));
        when(grants.findByUserId(1L)).thenReturn(List.of());

        service.grant(1L, Role.ADMIN, null);
        verify(grants).save(any(UserRoleGrant.class));

        when(grants.findByUserId(1L)).thenReturn(List.of(new UserRoleGrant(1L, Role.ADMIN, null)));
        service.grant(1L, Role.ADMIN, null);
        verify(grants).save(any(UserRoleGrant.class));
    }

    @Test
    void revokeDeletesGrantOrThrows() {
        UserRoleGrant grant = new UserRoleGrant(1L, Role.ADMIN, null);
        when(grants.findById(3L)).thenReturn(Optional.of(grant));
        when(users.findById(1L)).thenReturn(Optional.of(user(1, "a@b.ua", "A")));

        service.revoke(3L);

        verify(grants).delete(grant);
        assertThatThrownBy(() -> service.revoke(4L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void setActiveTogglesAccount() {
        User u = user(1, "a@b.ua", "A");
        when(users.findById(1L)).thenReturn(Optional.of(u));

        assertThat(service.setActive(1L, false).active()).isFalse();
        assertThat(service.setActive(1L, true).active()).isTrue();
        assertThatThrownBy(() -> service.setActive(2L, true)).isInstanceOf(NotFoundException.class);
    }
}
