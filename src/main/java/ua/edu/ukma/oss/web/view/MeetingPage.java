package ua.edu.ukma.oss.web.view;

import java.util.List;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.petition.PetitionDto;
import ua.edu.ukma.oss.protocol.ProtocolDto;

/**
 * @param protocol the protocol of the meeting, only when it is published
 */
public record MeetingPage(MeetingDto meeting, OrganizationDto org, ProtocolDto protocol, List<PetitionDto> petitions) {
}
