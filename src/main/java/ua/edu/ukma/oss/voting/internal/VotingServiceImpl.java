package ua.edu.ukma.oss.voting.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.organization.MembershipDto;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.protocol.ProtocolDto;
import ua.edu.ukma.oss.protocol.ProtocolReader;
import ua.edu.ukma.oss.protocol.ProtocolStatus;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.shared.NotFoundException;
import ua.edu.ukma.oss.voting.CreateVotingCmd;
import ua.edu.ukma.oss.voting.RecordCmd;
import ua.edu.ukma.oss.voting.VoteChoice;
import ua.edu.ukma.oss.voting.VoteRecordDto;
import ua.edu.ukma.oss.voting.VotingDto;
import ua.edu.ukma.oss.voting.VotingResult;
import ua.edu.ukma.oss.voting.VotingService;

@Service
@Transactional
class VotingServiceImpl implements VotingService {

    private final VotingRepository votings;
    private final VoteRecordRepository records;
    private final ProtocolReader protocols;
    private final MeetingService meetings;
    private final OrganizationService organizations;

    VotingServiceImpl(VotingRepository votings, VoteRecordRepository records, ProtocolReader protocols,
                      MeetingService meetings, OrganizationService organizations) {
        this.votings = votings;
        this.records = records;
        this.protocols = protocols;
        this.meetings = meetings;
        this.organizations = organizations;
    }

    @Override
    public VotingDto create(CreateVotingCmd cmd) {
        if (cmd.question() == null || cmd.question().isBlank()) {
            throw new BusinessRuleException("The question is required");
        }
        ProtocolDto protocol = requireDraft(cmd.protocolId());
        if (cmd.agendaItemId() != null) {
            boolean belongs = meetings.get(protocol.meetingId()).agenda().stream()
                    .anyMatch(item -> item.id().equals(cmd.agendaItemId()));
            if (!belongs) {
                throw new BusinessRuleException("The agenda item does not belong to the meeting of this protocol");
            }
        }
        return toDto(votings.save(new Voting(cmd.protocolId(), cmd.agendaItemId(), cmd.question().trim())));
    }

    @Override
    @Transactional(readOnly = true)
    public VotingDto get(Long votingId) {
        return toDto(find(votingId));
    }

    @Override
    public VotingDto record(RecordCmd cmd) {
        Voting voting = find(cmd.votingId());
        ProtocolDto protocol = requireDraft(voting.getProtocolId());
        List<MembershipDto> body = organizations.members(protocol.orgId());
        Set<Long> memberIds = body.stream().map(MembershipDto::id).collect(Collectors.toSet());
        if (!memberIds.containsAll(cmd.choices().keySet())) {
            throw new BusinessRuleException("Votes may only be recorded for current members of the body");
        }
        if (memberIds.isEmpty()) {
            throw new BusinessRuleException("The body has no members to vote");
        }

        Map<Long, VoteRecord> existing = records.findByVotingIdOrderByMembershipId(voting.getId()).stream()
                .collect(Collectors.toMap(VoteRecord::getMembershipId, r -> r));
        Map<Long, VoteChoice> effective = new HashMap<>();
        for (Long memberId : memberIds) {
            VoteChoice choice = cmd.choices().getOrDefault(memberId, VoteChoice.ABSENT);
            effective.put(memberId, choice);
            VoteRecord record = existing.get(memberId);
            if (record == null) {
                records.save(new VoteRecord(voting.getId(), memberId, choice));
            } else {
                record.setChoice(choice);
            }
        }
        voting.applyTally(VoteCounter.count(effective.values()));
        return toDto(voting);
    }

    @Override
    @Transactional(readOnly = true)
    public VotingResult tally(Long votingId) {
        find(votingId);
        VoteCounter.Counts c = VoteCounter.count(records.findByVotingIdOrderByMembershipId(votingId).stream()
                .map(VoteRecord::getChoice).toList());
        return new VotingResult(votingId, c.votesFor(), c.votesAgainst(), c.votesAbstain(), c.votesAbsent(),
                c.bodySize(), c.result());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VotingDto> byProtocol(Long protocolId) {
        return votings.findByProtocolIdOrderById(protocolId).stream().map(VotingServiceImpl::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoteRecordDto> records(Long votingId) {
        return records.findByVotingIdOrderByMembershipId(votingId).stream()
                .map(r -> new VoteRecordDto(r.getMembershipId(), r.getChoice())).toList();
    }

    @Override
    public void remove(Long votingId) {
        Voting voting = find(votingId);
        requireDraft(voting.getProtocolId());
        records.deleteByVotingId(votingId);
        votings.delete(voting);
    }

    private ProtocolDto requireDraft(Long protocolId) {
        ProtocolDto protocol = protocols.get(protocolId);
        if (protocol.status() != ProtocolStatus.DRAFT) {
            throw new BusinessRuleException("Votings can only be changed while the protocol is a draft");
        }
        return protocol;
    }

    private Voting find(Long id) {
        return votings.findById(id).orElseThrow(() -> new NotFoundException("Voting", id));
    }

    private static VotingDto toDto(Voting v) {
        return new VotingDto(v.getId(), v.getProtocolId(), v.getAgendaItemId(), v.getQuestion(), v.getResult(),
                v.getVotesFor(), v.getVotesAgainst(), v.getVotesAbstain());
    }
}
