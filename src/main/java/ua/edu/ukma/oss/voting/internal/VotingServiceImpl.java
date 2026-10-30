package ua.edu.ukma.oss.voting.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.auth.UserService;
import ua.edu.ukma.oss.organization.MembershipDto;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.protocol.ProtocolDto;
import ua.edu.ukma.oss.protocol.ProtocolReader;
import ua.edu.ukma.oss.protocol.ProtocolStatus;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.shared.NotFoundException;
import ua.edu.ukma.oss.voting.VoteRecordDto;
import ua.edu.ukma.oss.voting.VotingDto;
import ua.edu.ukma.oss.voting.VotingService;

@Service
@Transactional
class VotingServiceImpl implements VotingService {

    private final VotingRepository votings;
    private final VoteRecordRepository records;
    private final ProtocolReader protocols;
    private final OrganizationService organizations;
    private final UserService users;

    VotingServiceImpl(VotingRepository votings, VoteRecordRepository records, ProtocolReader protocols,
                      OrganizationService organizations, UserService users) {
        this.votings = votings;
        this.records = records;
        this.protocols = protocols;
        this.organizations = organizations;
        this.users = users;
    }

    @Override
    public List<VotingDto> importFromDocument(Long protocolId, String fileName, byte[] content) {
        ProtocolDto protocol = protocols.get(protocolId);
        if (protocol.status() != ProtocolStatus.DRAFT) {
            throw new BusinessRuleException("Голосування можна змінювати лише доки протокол є чернеткою");
        }
        List<ParsedVoting> parsed = ProtocolVotingParser.parse(ProtocolTextExtractor.extract(fileName, content));
        Map<String, Long> seats = parsed.stream().anyMatch(p -> !p.named().isEmpty())
                ? seatsByName(protocol.orgId()) : Map.of();

        for (Voting old : votings.findByProtocolIdOrderById(protocolId)) {
            records.deleteByVotingId(old.getId());
            votings.delete(old);
        }
        votings.flush();
        return parsed.stream().map(p -> {
            Voting voting = votings.save(new Voting(protocolId, p));
            p.named().forEach((name, choice) -> records.save(new VoteRecord(voting.getId(), seat(seats, name), choice)));
            return toDto(voting);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VotingDto get(Long votingId) {
        return votings.findById(votingId).map(VotingServiceImpl::toDto)
                .orElseThrow(() -> new NotFoundException("Voting", votingId));
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

    /**
     * Every membership ever seated in the body by member name; a person seated twice maps to the latest seat.
     */
    private Map<String, Long> seatsByName(Long orgId) {
        List<MembershipDto> history = organizations.history(orgId);
        Map<Long, String> names = users.fullNames(history.stream().map(MembershipDto::userId).toList());
        Map<String, Long> seats = new HashMap<>();
        history.stream().sorted(java.util.Comparator.comparing(MembershipDto::id))
                .forEach(m -> seats.put(normalise(names.getOrDefault(m.userId(), "")), m.id()));
        return seats;
    }

    private static Long seat(Map<String, Long> seats, String name) {
        Long id = seats.get(normalise(name));
        if (id == null) {
            throw new BusinessRuleException("«" + name + "» немає у складі органу, тому голос не можна зарахувати");
        }
        return id;
    }

    private static String normalise(String name) {
        return name.toLowerCase(Locale.ROOT).replace('’', '\'').replaceAll("\\s+", " ").trim();
    }

    private static VotingDto toDto(Voting v) {
        return new VotingDto(v.getId(), v.getProtocolId(), v.getQuestion(), v.getResult(), v.getVotesFor(),
                v.getVotesAgainst(), v.getVotesAbstain(), v.getVotesAbsent(), v.getPresent());
    }
}
