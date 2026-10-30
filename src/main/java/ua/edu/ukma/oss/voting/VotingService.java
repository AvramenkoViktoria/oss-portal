package ua.edu.ukma.oss.voting;

import java.util.List;

/**
 * Public API of the votings module. Votings are never created by hand: they are read from the
 * protocol document while the protocol is a draft.
 */
public interface VotingService {

    /**
     * Reads the "Результати голосування" sections of a PDF or DOCX protocol and replaces the votings of the
     * draft protocol with them. When the section names who voted how, the names are matched to the members of
     * the body and stored as per-member votes.
     *
     * @throws ua.edu.ukma.oss.shared.BusinessRuleException if the document has no such section
     */
    List<VotingDto> importFromDocument(Long protocolId, String fileName, byte[] content);

    VotingDto get(Long votingId);

    List<VotingDto> byProtocol(Long protocolId);

    /**
     * Named votes; empty when the protocol gave only totals.
     */
    List<VoteRecordDto> records(Long votingId);
}
