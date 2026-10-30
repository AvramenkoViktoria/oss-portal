package ua.edu.ukma.oss.meeting.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.meeting.AgendaItemDto;
import ua.edu.ukma.oss.meeting.MeetingCancelledEvent;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingHeldEvent;
import ua.edu.ukma.oss.meeting.MeetingScheduledEvent;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.meeting.MeetingStatus;
import ua.edu.ukma.oss.meeting.ScheduleCmd;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.shared.NotFoundException;

@Service
@Transactional
class MeetingServiceImpl implements MeetingService {

    private final MeetingRepository meetings;
    private final AgendaItemRepository agenda;
    private final MeetingMapper mapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    MeetingServiceImpl(MeetingRepository meetings, AgendaItemRepository agenda, MeetingMapper mapper,
                       ApplicationEventPublisher events, Clock clock) {
        this.meetings = meetings;
        this.agenda = agenda;
        this.mapper = mapper;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public MeetingDto schedule(ScheduleCmd cmd) {
        requireText(cmd.title(), "Назва");
        if (cmd.scheduledAt() == null) {
            throw new BusinessRuleException("Вкажіть дату й час засідання");
        }
        Meeting meeting = meetings.save(new Meeting(cmd.orgId(), cmd.title().trim(), cmd.scheduledAt(),
                blankToNull(cmd.streamUrl())));
        events.publishEvent(new MeetingScheduledEvent(meeting.getId(), meeting.getOrgId(), meeting.getTitle(),
                meeting.getScheduledAt(), meeting.getStreamUrl()));
        return mapper.toDto(meeting, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingDto get(Long meetingId) {
        Meeting meeting = find(meetingId);
        return mapper.toDto(meeting, agenda.findByMeetingIdOrderByPosition(meetingId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingDto> byOrg(Long orgId) {
        List<Meeting> found = meetings.findByOrgIdOrderByScheduledAtDesc(orgId);
        Map<Long, List<AgendaItem>> items = agenda
                .findByMeetingIdInOrderByPosition(found.stream().map(Meeting::getId).toList()).stream()
                .collect(Collectors.groupingBy(AgendaItem::getMeetingId));
        return found.stream().map(m -> mapper.toDto(m, items.getOrDefault(m.getId(), List.of()))).toList();
    }

    @Override
    public MeetingDto reschedule(Long meetingId, String title, Instant scheduledAt, String streamUrl) {
        requireText(title, "Назва");
        Meeting meeting = find(meetingId);
        meeting.reschedule(title.trim(), scheduledAt, blankToNull(streamUrl));
        return mapper.toDto(meeting, agenda.findByMeetingIdOrderByPosition(meetingId));
    }

    @Override
    public AgendaItemDto addAgendaItem(Long meetingId, String title) {
        requireText(title, "Назва пункту порядку денного");
        find(meetingId).ensurePlanned();
        int position = agenda.countByMeetingId(meetingId) + 1;
        return mapper.toDto(agenda.save(new AgendaItem(meetingId, position, title.trim())));
    }

    @Override
    public MeetingDto markHeld(Long meetingId) {
        Meeting meeting = find(meetingId);
        meeting.markHeld(clock.instant());
        events.publishEvent(new MeetingHeldEvent(meeting.getId(), meeting.getOrgId()));
        return mapper.toDto(meeting, agenda.findByMeetingIdOrderByPosition(meetingId));
    }

    @Override
    public List<MeetingDto> claimReminders(Duration lead) {
        Instant now = clock.instant();
        List<Meeting> due = meetings
                .findByStatusAndReminderSentAtIsNullAndScheduledAtAfterAndScheduledAtLessThanEqualOrderByScheduledAt(
                        MeetingStatus.PLANNED, now, now.plus(lead));
        due.forEach(m -> m.markReminded(now));
        return due.stream().map(m -> mapper.toDto(m, List.of())).toList();
    }

    @Override
    public MeetingDto cancel(Long meetingId) {
        Meeting meeting = find(meetingId);
        meeting.cancel();
        events.publishEvent(new MeetingCancelledEvent(meeting.getId(), meeting.getOrgId(), meeting.getTitle()));
        return mapper.toDto(meeting, agenda.findByMeetingIdOrderByPosition(meetingId));
    }

    private Meeting find(Long id) {
        return meetings.findById(id).orElseThrow(() -> new NotFoundException("Meeting", id));
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(field + " — обов’язкове поле");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
