package ua.edu.ukma.oss.web.view;

import java.util.List;
import java.util.Map;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.petition.PetitionDto;
import ua.edu.ukma.oss.protocol.ProtocolDto;

/**
 * Everything the public page of a body shows.
 *
 * @param publishedByMeeting published protocol per meeting id, to link past meetings to their minutes
 */
public record OrgPage(OrganizationDto org, List<MemberRow> members, List<MeetingDto> upcoming,
                      List<MeetingDto> past, Map<Long, ProtocolDto> publishedByMeeting,
                      List<ProtocolEntry> protocols, List<PetitionDto> petitions) {
}
