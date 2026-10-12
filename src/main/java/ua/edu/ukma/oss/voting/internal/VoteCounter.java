package ua.edu.ukma.oss.voting.internal;

import java.util.Collection;
import ua.edu.ukma.oss.voting.VoteChoice;
import ua.edu.ukma.oss.voting.VoteResult;

/**
 * Counting rule: a question is accepted only if more than half of the whole composition
 * voted for it, so abstentions and absences count against acceptance.
 */
final class VoteCounter {

    record Counts(int votesFor, int votesAgainst, int votesAbstain, int votesAbsent, int bodySize,
                  VoteResult result) {
    }

    private VoteCounter() {
    }

    static Counts count(Collection<VoteChoice> choices) {
        int votesFor = 0;
        int votesAgainst = 0;
        int votesAbstain = 0;
        int votesAbsent = 0;
        for (VoteChoice choice : choices) {
            switch (choice) {
                case FOR -> votesFor++;
                case AGAINST -> votesAgainst++;
                case ABSTAIN -> votesAbstain++;
                case ABSENT -> votesAbsent++;
            }
        }
        int bodySize = choices.size();
        VoteResult result = votesFor * 2 > bodySize ? VoteResult.ACCEPTED : VoteResult.REJECTED;
        return new Counts(votesFor, votesAgainst, votesAbstain, votesAbsent, bodySize, result);
    }
}
