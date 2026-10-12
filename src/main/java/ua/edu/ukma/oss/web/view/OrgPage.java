package ua.edu.ukma.oss.web.view;

import java.util.List;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.petition.PetitionDto;

/**
 * Everything the public page of a body shows. Protocols are reached only through a meeting.
 */
public record OrgPage(OrganizationDto org, List<MemberRow> members, CalendarView calendar,
                      List<MeetingDto> upcoming, List<PetitionDto> petitions) {
}
