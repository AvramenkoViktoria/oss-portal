package ua.edu.ukma.oss.web.view;

import java.util.List;
import ua.edu.ukma.oss.voting.VoteChoice;
import ua.edu.ukma.oss.voting.VotingDto;

/**
 * A voting prepared for display: the totals stated in the protocol as a bar and, when the protocol names them,
 * who voted how.
 */
public record VotingView(VotingDto voting, List<Segment> segments, List<Voter> voters) {

    /**
     * One coloured part of the summary bar.
     */
    public record Segment(VoteChoice choice, int count, double percent) {
    }

    /**
     * One member's vote.
     */
    public record Voter(String name, VoteChoice choice) {
    }

    public static VotingView of(VotingDto v, List<Voter> voters) {
        int total = Math.max(v.present(), v.votesFor() + v.votesAgainst() + v.votesAbstain() + v.votesAbsent());
        List<Segment> segments = List.of(
                segment(VoteChoice.FOR, v.votesFor(), total),
                segment(VoteChoice.AGAINST, v.votesAgainst(), total),
                segment(VoteChoice.ABSTAIN, v.votesAbstain(), total),
                segment(VoteChoice.ABSENT, v.votesAbsent(), total));
        return new VotingView(v, segments, voters);
    }

    private static Segment segment(VoteChoice choice, int count, int total) {
        return new Segment(choice, count, total == 0 ? 0 : 100.0 * count / total);
    }
}
