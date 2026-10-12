package ua.edu.ukma.oss.web.view;

import java.util.List;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.protocol.ProtocolDto;
import ua.edu.ukma.oss.protocol.Requirement;

/**
 * A protocol with its meeting, votings and, for editors, the approval checklist.
 *
 * @param requirements approval preconditions; empty on the public page
 * @param approvedByName  name of the approver, {@code null} if not approved yet
 * @param publishedByName name of the publisher, {@code null} if not published yet
 */
public record ProtocolPage(ProtocolDto protocol, MeetingDto meeting, OrganizationDto org, List<VotingView> votings,
                           List<Requirement> requirements, String approvedByName, String publishedByName) {

    public boolean requirementsMet() {
        return requirements.stream().allMatch(Requirement::met);
    }
}
