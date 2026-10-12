package ua.edu.ukma.oss.web;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import ua.edu.ukma.oss.auth.UserService;
import ua.edu.ukma.oss.meeting.AgendaItemDto;
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
import ua.edu.ukma.oss.voting.ChartData;
import ua.edu.ukma.oss.voting.VoteChoice;
import ua.edu.ukma.oss.voting.VoteRecordDto;
import ua.edu.ukma.oss.voting.VoteViewFactory;
import ua.edu.ukma.oss.voting.VotingDto;
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
    private final VoteViewFactory viewFactory;
    private final UserService users;
    private final Clock clock;

    SiteViews(OrganizationService organizations, MeetingService meetings, ProtocolReader protocols,
              PetitionService petitions, VotingService votings, VoteViewFactory viewFactory, UserService users,
              Clock clock) {
        this.organizations = organizations;
        this.meetings = meetings;
        this.protocols = protocols;
        this.petitions = petitions;
        this.votings = votings;
        this.viewFactory = viewFactory;
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
        Map<Long, String> agendaTitles = meeting.agenda().stream()
                .collect(Collectors.toMap(AgendaItemDto::id, AgendaItemDto::title));
        List<VotingView> votingViews = votings.byProtocol(protocolId).stream()
                .map(v -> votingView(v, agendaTitles, seatNames)).toList();
        Map<Long, String> actors = users.fullNames(actorIds(protocol));
        return new ProtocolPage(protocol, meeting, org, votingViews,
                withRequirements ? protocols.approvalRequirements(protocolId) : List.of(),
                actors.get(protocol.approvedBy()), actors.get(protocol.publishedBy()));
    }

    /**
     * Current members of a body, for the voting form.
     */
    List<MemberRow> currentMembers(Long orgId) {
        return memberRows(organizations.members(orgId));
    }

    private VotingView votingView(VotingDto voting, Map<Long, String> agendaTitles, Map<Long, String> seatNames) {
        List<VoteRecordDto> records = votings.records(voting.id());
        int bodySize = records.size();
        ChartData summary = viewFactory.summary().render(voting, records);
        ChartData grid = viewFactory.create(bodySize).render(voting, records);
        int absent = (int) records.stream().filter(r -> r.choice() == VoteChoice.ABSENT).count();
        List<VotingView.Segment> segments = summary.slices().stream()
                .map(s -> new VotingView.Segment(s.choice(), s.count(),
                        bodySize == 0 ? 0 : 100.0 * s.count() / bodySize))
                .toList();
        Map<Long, VoteChoice> choices = new HashMap<>();
        records.forEach(r -> choices.put(r.membershipId(), r.choice()));
        return new VotingView(voting, agendaTitles.get(voting.agendaItemId()), summary, grid, seatNames, choices,
                bodySize, absent, segments);
    }

    /**
     * Names for every membership ever seated in the body, so historic votings stay readable.
     */
    private Map<Long, String> seatNames(Long orgId) {
        List<MembershipDto> history = organizations.history(orgId);
        Map<Long, String> names = users.fullNames(history.stream().map(MembershipDto::userId).toList());
        Map<Long, String> seats = new HashMap<>();
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
