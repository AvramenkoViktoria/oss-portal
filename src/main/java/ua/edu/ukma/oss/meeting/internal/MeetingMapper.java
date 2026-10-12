package ua.edu.ukma.oss.meeting.internal;

import java.util.List;
import org.springframework.stereotype.Component;
import ua.edu.ukma.oss.meeting.AgendaItemDto;
import ua.edu.ukma.oss.meeting.MeetingDto;

/**
 * Converts entities to the DTOs exposed by the module.
 */
@Component
class MeetingMapper {

    MeetingDto toDto(Meeting m, List<AgendaItem> agenda) {
        return new MeetingDto(m.getId(), m.getOrgId(), m.getTitle(), m.getScheduledAt(), m.getStreamUrl(),
                m.getStatus(), agenda.stream().map(this::toDto).toList());
    }

    AgendaItemDto toDto(AgendaItem a) {
        return new AgendaItemDto(a.getId(), a.getMeetingId(), a.getPosition(), a.getTitle());
    }
}
