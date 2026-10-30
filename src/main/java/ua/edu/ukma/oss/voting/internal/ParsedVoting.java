package ua.edu.ukma.oss.voting.internal;

import java.util.Map;
import ua.edu.ukma.oss.voting.VoteChoice;
import ua.edu.ukma.oss.voting.VoteResult;

/**
 * One "Результати голосування" section of a protocol.
 *
 * @param named who voted how, in the order of the protocol; empty when the protocol gives only totals
 */
record ParsedVoting(String question, VoteResult result, int votesFor, int votesAgainst, int votesAbstain,
                    int votesAbsent, int present, Map<String, VoteChoice> named) {
}
