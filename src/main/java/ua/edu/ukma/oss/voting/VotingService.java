package ua.edu.ukma.oss.voting;

import java.util.List;

/**
 * Public API of the votings module. Votings can be created and recorded only while the
 * owning protocol is a draft.
 */
public interface VotingService {

    VotingDto create(CreateVotingCmd cmd);

    VotingDto get(Long votingId);

    /**
     * Stores every member's choice (replacing earlier ones) and returns the recounted voting.
     */
    VotingDto record(RecordCmd cmd);

    /**
     * Counts the stored votes. A question is accepted when more than half of the whole
     * composition voted for it, as in the Verkhovna Rada.
     */
    VotingResult tally(Long votingId);

    List<VotingDto> byProtocol(Long protocolId);

    List<VoteRecordDto> records(Long votingId);

    /**
     * Removes a voting together with its recorded votes.
     */
    void remove(Long votingId);
}
