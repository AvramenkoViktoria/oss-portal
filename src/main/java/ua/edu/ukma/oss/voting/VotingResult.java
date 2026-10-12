package ua.edu.ukma.oss.voting;

/**
 * Outcome of counting a voting.
 *
 * @param bodySize number of members who were entitled to vote
 */
public record VotingResult(Long votingId, int votesFor, int votesAgainst, int votesAbstain, int votesAbsent,
                           int bodySize, VoteResult result) {
}
