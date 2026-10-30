package ua.edu.ukma.oss.voting;

/**
 * Read model of a voting as stated in the protocol.
 *
 * @param result      outcome announced in the protocol
 * @param votesAbsent members who did not vote
 * @param present     members present when the decision was taken
 */
public record VotingDto(Long id, Long protocolId, String question, VoteResult result, int votesFor,
                        int votesAgainst, int votesAbstain, int votesAbsent, int present) {
}
