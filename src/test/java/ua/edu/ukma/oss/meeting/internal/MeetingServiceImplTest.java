package ua.edu.ukma.oss.meeting.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import ua.edu.ukma.oss.meeting.AgendaItemDto;
import ua.edu.ukma.oss.meeting.MeetingCancelledEvent;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingHeldEvent;
import ua.edu.ukma.oss.meeting.MeetingScheduledEvent;
import ua.edu.ukma.oss.meeting.MeetingStatus;
import ua.edu.ukma.oss.meeting.ScheduleCmd;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.shared.NotFoundException;

class MeetingServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    private final MeetingRepository meetings = mock(MeetingRepository.class);
    private final AgendaItemRepository agenda = mock(AgendaItemRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final MeetingServiceImpl service =
            new MeetingServiceImpl(meetings, agenda, new MeetingMapper(), events, clock);

    private Meeting meeting(long id, Instant at) {
        Meeting m = new Meeting(1L, "Засідання", at, null);
        ReflectionTestUtils.setField(m, "id", id);
        when(meetings.findById(id)).thenReturn(Optional.of(m));
        return m;
    }

    @Test
    void scheduleValidatesAndPublishesEvent() {
        assertThatThrownBy(() -> service.schedule(new ScheduleCmd(1L, " ", NOW, null)))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.schedule(new ScheduleCmd(1L, "T", null, null)))
                .isInstanceOf(BusinessRuleException.class);
        when(meetings.save(any(Meeting.class))).thenAnswer(inv -> inv.getArgument(0));

        MeetingDto dto = service.schedule(new ScheduleCmd(1L, " Назва ", NOW.plusSeconds(60), " "));

        assertThat(dto.title()).isEqualTo("Назва");
        assertThat(dto.streamUrl()).isNull();
        assertThat(dto.status()).isEqualTo(MeetingStatus.PLANNED);
        verify(events).publishEvent(any(MeetingScheduledEvent.class));
    }

    @Test
    void getIncludesAgendaAndThrowsWhenMissing() {
        meeting(1, NOW.plusSeconds(60));
        when(agenda.findByMeetingIdOrderByPosition(1L)).thenReturn(List.of(new AgendaItem(1L, 1, "Пункт")));

        assertThat(service.get(1L).agenda()).extracting(AgendaItemDto::title).containsExactly("Пункт");
        assertThatThrownBy(() -> service.get(2L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void byOrgGroupsAgendaPerMeeting() {
        Meeting a = meeting(1, NOW.plusSeconds(60));
        Meeting b = meeting(2, NOW.plusSeconds(120));
        AgendaItem item = new AgendaItem(1L, 1, "Пункт");
        when(meetings.findByOrgIdOrderByScheduledAtDesc(1L)).thenReturn(List.of(b, a));
        when(agenda.findByMeetingIdInOrderByPosition(List.of(2L, 1L))).thenReturn(List.of(item));

        List<MeetingDto> result = service.byOrg(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).agenda()).isEmpty();
        assertThat(result.get(1).agenda()).hasSize(1);
    }

    @Test
    void rescheduleChangesTitleAndTime() {
        meeting(1, NOW.plusSeconds(60));

        MeetingDto dto = service.reschedule(1L, " Нова ", NOW.plusSeconds(3600), "http://stream");

        assertThat(dto.title()).isEqualTo("Нова");
        assertThat(dto.streamUrl()).isEqualTo("http://stream");
        assertThatThrownBy(() -> service.reschedule(1L, " ", NOW, null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void addAgendaItemNumbersItemsSequentially() {
        meeting(1, NOW.plusSeconds(60));
        when(agenda.countByMeetingId(1L)).thenReturn(2);
        when(agenda.save(any(AgendaItem.class))).thenAnswer(inv -> inv.getArgument(0));

        AgendaItemDto dto = service.addAgendaItem(1L, " Пункт ");

        assertThat(dto.position()).isEqualTo(3);
        assertThat(dto.title()).isEqualTo("Пункт");
        assertThatThrownBy(() -> service.addAgendaItem(1L, "")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void markHeldAndCancelPublishEvents() {
        meeting(1, NOW.minusSeconds(60));
        meeting(2, NOW.plusSeconds(60));

        assertThat(service.markHeld(1L).status()).isEqualTo(MeetingStatus.HELD);
        assertThat(service.cancel(2L).status()).isEqualTo(MeetingStatus.CANCELLED);

        verify(events).publishEvent(any(MeetingHeldEvent.class));
        verify(events).publishEvent(any(MeetingCancelledEvent.class));
    }

    @Test
    void claimRemindersMarksDueMeetings() {
        Meeting due = meeting(1, NOW.plusSeconds(1800));
        when(meetings.findByStatusAndReminderSentAtIsNullAndScheduledAtAfterAndScheduledAtLessThanEqualOrderByScheduledAt(
                MeetingStatus.PLANNED, NOW, NOW.plus(Duration.ofHours(1)))).thenReturn(List.of(due));

        List<MeetingDto> claimed = service.claimReminders(Duration.ofHours(1));

        assertThat(claimed).hasSize(1);
        assertThat(ReflectionTestUtils.getField(due, "reminderSentAt")).isEqualTo(NOW);
    }
}
