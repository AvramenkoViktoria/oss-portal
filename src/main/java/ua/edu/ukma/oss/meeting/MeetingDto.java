package ua.edu.ukma.oss.meeting;

import java.time.Instant;
import java.util.List;

/**
 * Read model of a meeting together with its agenda.
 *
 * @param streamUrl link to the live broadcast, may be {@code null}
 */
public record MeetingDto(Long id, Long orgId, String title, Instant scheduledAt, String streamUrl,
                         MeetingStatus status, List<AgendaItemDto> agenda) {
}
