package ua.edu.ukma.oss.web;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import ua.edu.ukma.oss.auth.UserService;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.meeting.MeetingStatus;
import ua.edu.ukma.oss.organization.MembershipDto;
import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.petition.PetitionService;
import ua.edu.ukma.oss.protocol.ProtocolDto;
import ua.edu.ukma.oss.protocol.ProtocolReader;
import ua.edu.ukma.oss.protocol.ProtocolStatus;
import ua.edu.ukma.oss.voting.VotingService;
import ua.edu.ukma.oss.web.view.CalendarView;
import ua.edu.ukma.oss.web.view.MeetingPage;
import ua.edu.ukma.oss.web.view.MemberRow;
import ua.edu.ukma.oss.web.view.OrgPage;
import ua.edu.ukma.oss.web.view.OrgSummary;
import ua.edu.ukma.oss.web.view.ProtocolPage;
import ua.edu.ukma.oss.web.view.VotingView;

/**
 * Assembles page view models from several modules so controllers stay thin.
 */
@Component
class SiteViews {

    private static final int UPCOMING = 3;

    private final OrganizationService organizations;
    private final MeetingService meetings;
    private final ProtocolReader protocols;
    private final PetitionService petitions;
    private final VotingService votings;
    private final UserService users;
    private final Clock clock;

    SiteViews(OrganizationService organizations, MeetingService meetings, ProtocolReader protocols,
              PetitionService petitions, VotingService votings, UserService users, Clock clock) {
        this.organizations = organizations;
        this.meetings = meetings;
        this.protocols = protocols;
        this.petitions = petitions;
        this.votings = votings;
        this.users = users;
        this.clock = clock;
    }

    OrgSummary summary(OrganizationDto org) {
        MeetingDto next = meetings.byOrg(org.id()).stream()
                .filter(m -> m.status() == MeetingStatus.PLANNED && m.scheduledAt().isAfter(clock.instant()))
                .min(Comparator.comparing(MeetingDto::scheduledAt)).orElse(null);
        return new OrgSummary(org, organizations.members(org.id()).size(), next);
    }

    OrgPage orgPage(OrganizationDto org, YearMonth month) {
        List<MeetingDto> all = meetings.byOrg(org.id());
        List<MeetingDto> upcoming = all.stream()
                .filter(m -> m.status() == MeetingStatus.PLANNED && m.scheduledAt().isAfter(clock.instant()))
                .sorted(Comparator.comparing(MeetingDto::scheduledAt)).limit(UPCOMING).toList();
        return new OrgPage(org, memberRows(organizations.members(org.id())), calendar(month, all), upcoming,
                petitions.byOrg(org.id()));
    }

    /**
     * Month grid of the given meetings in the portal's time zone.
     */
    CalendarView calendar(YearMonth month, List<MeetingDto> meetingList) {
        return CalendarView.of(month, clock.getZone(), LocalDate.now(clock), meetingList);
    }

    YearMonth currentMonth() {
        return YearMonth.now(clock);
    }

    MeetingPage meetingPage(Long meetingId) {
        MeetingDto meeting = meetings.get(meetingId);
        ProtocolDto published = protocols.byMeeting(meetingId)
                .filter(p -> p.status() == ProtocolStatus.PUBLISHED).orElse(null);
        return new MeetingPage(meeting, organizations.get(meeting.orgId()), published,
                petitions.byMeeting(meetingId));
    }

    /**
     * @param withRequirements include the approval checklist (editors only)
     */
    ProtocolPage protocolPage(Long protocolId, boolean withRequirements) {
        ProtocolDto protocol = protocols.get(protocolId);
        MeetingDto meeting = meetings.get(protocol.meetingId());
        OrganizationDto org = organizations.get(protocol.orgId());
        Map<Long, String> seatNames = seatNames(org.id());
        List<VotingView> votingViews = votings.byProtocol(protocolId).stream()
                .map(v -> VotingView.of(v, votings.records(v.id()).stream()
                        .map(r -> new VotingView.Voter(seatNames.getOrDefault(r.membershipId(), "—"), r.choice()))
                        .toList()))
                .toList();
        Map<Long, String> actors = users.fullNames(actorIds(protocol));
        return new ProtocolPage(protocol, meeting, org, votingViews,
                withRequirements ? protocols.approvalRequirements(protocolId) : List.of(),
                actors.get(protocol.approvedBy()), actors.get(protocol.publishedBy()));
    }

    /**
     * Names for every membership ever seated in the body, so historic votings stay readable.
     */
    private Map<Long, String> seatNames(Long orgId) {
        List<MembershipDto> history = organizations.history(orgId);
        Map<Long, String> names = users.fullNames(history.stream().map(MembershipDto::userId).toList());
        Map<Long, String> seats = new java.util.HashMap<>();
        history.forEach(m -> seats.put(m.id(), names.getOrDefault(m.userId(), "—")));
        return seats;
    }

    private List<MemberRow> memberRows(List<MembershipDto> members) {
        Map<Long, String> names = users.fullNames(members.stream().map(MembershipDto::userId).toList());
        return members.stream().map(m -> new MemberRow(m.id(), names.getOrDefault(m.userId(), "—"), m.faculty(),
                m.position(), m.termStart(), m.termEnd())).toList();
    }

    private static Set<Long> actorIds(ProtocolDto p) {
        Set<Long> ids = new java.util.HashSet<>();
        if (p.approvedBy() != null) {
            ids.add(p.approvedBy());
        }
        if (p.publishedBy() != null) {
            ids.add(p.publishedBy());
        }
        return ids;
    }
}
