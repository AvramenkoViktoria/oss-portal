package ua.edu.ukma.oss.web.view;

import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.protocol.ProtocolDto;

/**
 * A protocol together with the meeting it belongs to, for lists.
 */
public record ProtocolEntry(ProtocolDto protocol, MeetingDto meeting) {
}
