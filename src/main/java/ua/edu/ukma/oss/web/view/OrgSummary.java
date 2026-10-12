package ua.edu.ukma.oss.web.view;

import java.util.List;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.organization.OrganizationDto;

/**
 * Card of a body on the home page.
 *
 * @param nextMeeting nearest planned meeting, may be {@code null}
 */
public record OrgSummary(OrganizationDto org, int memberCount, MeetingDto nextMeeting,
                         List<ProtocolEntry> latestProtocols) {
}
