package ua.edu.ukma.oss.meeting;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Public API of the meetings module.
 */
public interface MeetingService {

    MeetingDto schedule(ScheduleCmd cmd);

    MeetingDto get(Long meetingId);

    /**
     * Meetings of the body, newest first.
     */
    List<MeetingDto> byOrg(Long orgId);

    /**
     * Updates title, time and stream link of a planned meeting.
     */
    MeetingDto reschedule(Long meetingId, String title, Instant scheduledAt, String streamUrl);

    /**
     * Appends an item to the agenda of a planned meeting.
     */
    AgendaItemDto addAgendaItem(Long meetingId, String title);

    /**
     * Marks a started meeting as held and publishes {@link MeetingHeldEvent}.
     */
    MeetingDto markHeld(Long meetingId);

    /**
     * Planned meetings that start within {@code lead} from now and were not reminded yet; each is marked as
     * reminded, so a meeting is returned once until it is rescheduled. Meetings created closer to the start than
     * {@code lead} are returned right away.
     */
    List<MeetingDto> claimReminders(Duration lead);

    /**
     * Cancels a planned meeting and publishes {@link MeetingCancelledEvent}.
     */
    MeetingDto cancel(Long meetingId);
}
