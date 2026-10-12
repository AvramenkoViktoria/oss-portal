package ua.edu.ukma.oss.voting;

import java.util.Map;

/**
 * Votes of the body for one voting.
 *
 * @param choices membership id to choice; members that are missing are recorded as absent
 */
public record RecordCmd(Long votingId, Map<Long, VoteChoice> choices) {
}
