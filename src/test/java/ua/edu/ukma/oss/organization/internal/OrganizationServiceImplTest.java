package ua.edu.ukma.oss.organization.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.oss.organization.AddMemberCmd;
import ua.edu.ukma.oss.organization.MembershipDto;
import ua.edu.ukma.oss.shared.BusinessRuleException;

class OrganizationServiceImplTest {

    private final OrganizationRepository orgs = mock(OrganizationRepository.class);
    private final MembershipRepository members = mock(MembershipRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-12T10:00:00Z"), ZoneOffset.UTC);
    private final OrganizationServiceImpl service = new OrganizationServiceImpl(orgs, members, clock);

    @BeforeEach
    void orgExists() {
        when(orgs.findById(1L)).thenReturn(Optional.of(mock(Organization.class)));
    }

    @Test
    void rejectsTermEndingBeforeItStarts() {
        AddMemberCmd cmd = new AddMemberCmd(5L, 1L, "FI", "Member",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 8, 1));

        assertThatThrownBy(() -> service.addMember(cmd)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsOverlappingTermOfTheSamePerson() {
        when(members.existsOverlapping(eq(5L), eq(1L), any(), any())).thenReturn(true);
        AddMemberCmd cmd = new AddMemberCmd(5L, 1L, "FI", "Member", LocalDate.of(2026, 9, 1), null);

        assertThatThrownBy(() -> service.addMember(cmd))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("overlaps");
    }

    @Test
    void savesValidMembership() {
        when(members.save(any(Membership.class))).thenAnswer(inv -> inv.getArgument(0));
        AddMemberCmd cmd = new AddMemberCmd(5L, 1L, "FI", "Member", LocalDate.of(2026, 9, 1), null);

        MembershipDto dto = service.addMember(cmd);

        assertThat(dto.userId()).isEqualTo(5L);
        assertThat(dto.termEnd()).isNull();
    }

    @Test
    void membershipChecksUseTodayFromTheClock() {
        when(members.existsActive(7L, LocalDate.of(2026, 10, 12))).thenReturn(true);

        assertThat(service.isMember(7L)).isTrue();
        assertThat(service.isMember(8L)).isFalse();
    }
}
