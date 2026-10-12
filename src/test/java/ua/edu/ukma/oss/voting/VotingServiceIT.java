package ua.edu.ukma.oss.voting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.auth.UserService;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.meeting.ScheduleCmd;
import ua.edu.ukma.oss.organization.AddMemberCmd;
import ua.edu.ukma.oss.organization.MembershipDto;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.protocol.ProtocolDto;
import ua.edu.ukma.oss.protocol.ProtocolReader;
import ua.edu.ukma.oss.protocol.ProtocolWriter;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.support.IntegrationTest;

@IntegrationTest
@Transactional
class VotingServiceIT {

    @Autowired
    VotingService votings;
    @Autowired
    ProtocolReader reader;
    @Autowired
    ProtocolWriter writer;
    @Autowired
    MeetingService meetings;
    @Autowired
    OrganizationService organizations;
    @Autowired
    UserService users;

    Long ks;
    Long editor;
    List<MembershipDto> body;
    ProtocolDto protocol;

    @BeforeEach
    void setUp() {
        ks = organizations.getByCode(OrgCode.KS).id();
        editor = newUser();
        // Three seated members; the seeded KS has none, so the body size is exactly three.
        for (int i = 0; i < 3; i++) {
            organizations.addMember(new AddMemberCmd(newUser(), ks, "FI", "Delegate " + i,
                    LocalDate.now().minusDays(10), null));
        }
        body = organizations.members(ks);
        MeetingDto meeting = meetings.schedule(new ScheduleCmd(ks, "Session", Instant.now().minus(1, ChronoUnit.HOURS), null));
        meetings.markHeld(meeting.id());
        protocol = reader.byMeeting(meeting.id()).orElseThrow();
    }

    private Long newUser() {
        return users.register("v-" + UUID.randomUUID() + "@ukma.edu.ua", "s3cret-pass", "Delegate").id();
    }

    private Map<Long, VoteChoice> choices(VoteChoice... values) {
        Map<Long, VoteChoice> map = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            map.put(body.get(i).id(), values[i]);
        }
        return map;
    }

    @Test
    void recordedVotesAreCountedAndAccepted() {
        VotingDto voting = votings.create(new CreateVotingCmd(protocol.id(), null, "Approve the budget?"));

        VotingDto recorded = votings.record(new RecordCmd(voting.id(),
                choices(VoteChoice.FOR, VoteChoice.FOR, VoteChoice.AGAINST)));

        assertThat(recorded.result()).isEqualTo(VoteResult.ACCEPTED);
        assertThat(recorded.votesFor()).isEqualTo(2);
        assertThat(recorded.votesAgainst()).isEqualTo(1);
        assertThat(votings.tally(voting.id()).bodySize()).isEqualTo(body.size());
    }

    @Test
    void missingMembersAreRecordedAsAbsent() {
        VotingDto voting = votings.create(new CreateVotingCmd(protocol.id(), null, "Q"));

        votings.record(new RecordCmd(voting.id(), choices(VoteChoice.FOR)));

        VotingResult result = votings.tally(voting.id());
        assertThat(result.votesAbsent()).isEqualTo(body.size() - 1);
        assertThat(result.result()).isEqualTo(VoteResult.REJECTED);
    }

    @Test
    void recordingAgainReplacesPreviousVotes() {
        VotingDto voting = votings.create(new CreateVotingCmd(protocol.id(), null, "Q"));
        votings.record(new RecordCmd(voting.id(), choices(VoteChoice.AGAINST, VoteChoice.AGAINST, VoteChoice.AGAINST)));

        VotingDto second = votings.record(new RecordCmd(voting.id(),
                choices(VoteChoice.FOR, VoteChoice.FOR, VoteChoice.FOR)));

        assertThat(second.result()).isEqualTo(VoteResult.ACCEPTED);
        assertThat(votings.records(voting.id())).hasSize(body.size());
    }

    @Test
    void votesOfNonMembersAreRejected() {
        VotingDto voting = votings.create(new CreateVotingCmd(protocol.id(), null, "Q"));

        assertThatThrownBy(() -> votings.record(new RecordCmd(voting.id(), Map.of(-5L, VoteChoice.FOR))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void agendaItemMustBelongToTheProtocolMeeting() {
        assertThatThrownBy(() -> votings.create(new CreateVotingCmd(protocol.id(), -1L, "Q")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void unrecordedVotingBlocksApprovalUntilRecorded() {
        writer.attachFile(protocol.id(), "minutes.pdf", new ByteArrayInputStream(new byte[] {1}));
        VotingDto voting = votings.create(new CreateVotingCmd(protocol.id(), null, "Q"));

        assertThatThrownBy(() -> writer.approve(protocol.id(), editor)).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Votes are recorded");

        votings.record(new RecordCmd(voting.id(), choices(VoteChoice.FOR, VoteChoice.FOR, VoteChoice.FOR)));
        assertThat(writer.approve(protocol.id(), editor).status().name()).isEqualTo("APPROVED");
    }

    @Test
    void votingsAreLockedOnceProtocolIsApproved() {
        VotingDto voting = votings.create(new CreateVotingCmd(protocol.id(), null, "Q"));
        votings.record(new RecordCmd(voting.id(), choices(VoteChoice.FOR, VoteChoice.FOR, VoteChoice.FOR)));
        writer.attachFile(protocol.id(), "minutes.pdf", new ByteArrayInputStream(new byte[] {1}));
        writer.approve(protocol.id(), editor);

        assertThatThrownBy(() -> votings.record(new RecordCmd(voting.id(), choices(VoteChoice.AGAINST))))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> votings.create(new CreateVotingCmd(protocol.id(), null, "Another")))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> votings.remove(voting.id())).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void removeDeletesVotingWithItsRecords() {
        VotingDto voting = votings.create(new CreateVotingCmd(protocol.id(), null, "Q"));
        votings.record(new RecordCmd(voting.id(), choices(VoteChoice.FOR)));

        votings.remove(voting.id());

        assertThat(votings.byProtocol(protocol.id())).isEmpty();
    }
}
