package ua.edu.ukma.oss.petition.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.oss.meeting.MeetingCancelledEvent;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingHeldEvent;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.meeting.MeetingStatus;
import ua.edu.ukma.oss.meeting.ScheduleCmd;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.petition.AuthorEligibilityPolicy;
import ua.edu.ukma.oss.petition.PetitionDto;
import ua.edu.ukma.oss.petition.PetitionLink;
import ua.edu.ukma.oss.petition.PetitionStatus;
import ua.edu.ukma.oss.petition.SubmitCmd;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.shared.NotFoundException;

class PetitionServiceImplTest {

    private final PetitionRepository repo = mock(PetitionRepository.class);
    private final AuthorEligibilityPolicy policy = mock(AuthorEligibilityPolicy.class);
    private final MeetingService meetings = mock(MeetingService.class);
    private final OrganizationService organizations = mock(OrganizationService.class);
    private final PetitionServiceImpl service = new PetitionServiceImpl(repo, policy, meetings, organizations);

    private Petition petition() {
        Petition p = new Petition("Title", "Text", 7L, 1L);
        when(repo.findById(5L)).thenReturn(Optional.of(p));
        return p;
    }

    private MeetingDto meeting(long id, long orgId, MeetingStatus status) {
        return new MeetingDto(id, orgId, "M", Instant.parse("2026-11-01T10:00:00Z"), null, status, List.of());
    }

    @Test
    void submitValidatesInput() {
        assertThatThrownBy(() -> service.submit(new SubmitCmd(7L, 1L, " ", "t"))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.submit(new SubmitCmd(7L, 1L, "x".repeat(256), "t")))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.submit(new SubmitCmd(7L, 1L, "T", null))).isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void submitChecksEligibilityAndSavesTrimmed() {
        when(repo.save(any(Petition.class))).thenAnswer(inv -> inv.getArgument(0));

        PetitionDto dto = service.submit(new SubmitCmd(7L, 1L, " Title ", " Text "));

        verify(organizations).get(1L);
        verify(policy).ensureEligible(7L, 1L);
        assertThat(dto.title()).isEqualTo("Title");
        assertThat(dto.text()).isEqualTo("Text");
        assertThat(dto.status()).isEqualTo(PetitionStatus.SUBMITTED);
    }

    @Test
    void getThrowsForUnknownPetition() {
        petition();
        assertThat(service.get(5L).title()).isEqualTo("Title");
        assertThatThrownBy(() -> service.get(6L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void attachAsAgendaItemAddsItemToMeeting() {
        Petition p = petition();
        when(meetings.get(9L)).thenReturn(meeting(9, 1, MeetingStatus.PLANNED));

        PetitionDto dto = service.attach(5L, 9L, PetitionLink.AGENDA_ITEM);

        assertThat(dto.status()).isEqualTo(PetitionStatus.SCHEDULED);
        assertThat(p.getMeetingId()).isEqualTo(9L);
        verify(meetings).addAgendaItem(9L, "Петиція: Title");
    }

    @Test
    void attachRejectsForeignOrNotPlannedMeeting() {
        petition();
        when(meetings.get(9L)).thenReturn(meeting(9, 2, MeetingStatus.PLANNED));
        assertThatThrownBy(() -> service.attach(5L, 9L, PetitionLink.AGENDA_ITEM))
                .isInstanceOf(BusinessRuleException.class);

        when(meetings.get(9L)).thenReturn(meeting(9, 1, MeetingStatus.HELD));
        assertThatThrownBy(() -> service.attach(5L, 9L, PetitionLink.AGENDA_ITEM))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void separateMeetingSchedulesNewMeetingWithAgendaItem() {
        Petition p = petition();
        Instant at = Instant.parse("2026-11-01T10:00:00Z");
        when(meetings.schedule(any(ScheduleCmd.class))).thenReturn(meeting(11, 1, MeetingStatus.PLANNED));

        PetitionDto dto = service.scheduleSeparateMeeting(5L, "Окреме", at, null);

        assertThat(dto.meetingId()).isEqualTo(11L);
        assertThat(p.getLink()).isEqualTo(PetitionLink.SEPARATE_MEETING);
        verify(meetings).addAgendaItem(eq(11L), any());
    }

    @Test
    void separateMeetingOnlyForSubmittedPetition() {
        Petition p = petition();
        p.schedule(3L, PetitionLink.AGENDA_ITEM);

        assertThatThrownBy(() -> service.scheduleSeparateMeeting(5L, "T", Instant.now(), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectRequiresReason() {
        Petition p = petition();
        assertThatThrownBy(() -> service.reject(5L, " ")).isInstanceOf(BusinessRuleException.class);

        PetitionDto dto = service.reject(5L, " Не підтримано ");

        assertThat(dto.status()).isEqualTo(PetitionStatus.REJECTED);
        assertThat(dto.rejectionReason()).isEqualTo("Не підтримано");
        assertThat(p.getStatus()).isEqualTo(PetitionStatus.REJECTED);
    }

    @Test
    void listsAreMappedToDtos() {
        Petition p = new Petition("Title", "Text", 7L, 1L);
        when(repo.findByOrgIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(p));
        when(repo.findByAuthorIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(p));
        when(repo.findByMeetingIdOrderByCreatedAt(9L)).thenReturn(List.of(p));

        assertThat(service.byOrg(1L)).hasSize(1);
        assertThat(service.byAuthor(7L)).hasSize(1);
        assertThat(service.byMeeting(9L)).hasSize(1);
    }

    @Test
    void meetingEventsMoveScheduledPetitions() {
        Petition held = new Petition("A", "T", 7L, 1L);
        held.schedule(9L, PetitionLink.AGENDA_ITEM);
        Petition cancelled = new Petition("B", "T", 7L, 1L);
        cancelled.schedule(8L, PetitionLink.AGENDA_ITEM);
        when(repo.findByMeetingIdAndStatus(9L, PetitionStatus.SCHEDULED)).thenReturn(List.of(held));
        when(repo.findByMeetingIdAndStatus(8L, PetitionStatus.SCHEDULED)).thenReturn(List.of(cancelled));

        service.onMeetingHeld(new MeetingHeldEvent(9L, 1L));
        service.onMeetingCancelled(new MeetingCancelledEvent(8L, 1L, "M"));

        assertThat(held.getStatus()).isEqualTo(PetitionStatus.CONSIDERED);
        assertThat(cancelled.getStatus()).isEqualTo(PetitionStatus.SUBMITTED);
    }
}
