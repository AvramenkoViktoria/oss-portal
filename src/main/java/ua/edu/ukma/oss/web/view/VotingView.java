package ua.edu.ukma.oss.web.view;

import java.util.List;
import java.util.Map;
import ua.edu.ukma.oss.voting.ChartData;
import ua.edu.ukma.oss.voting.VoteChoice;
import ua.edu.ukma.oss.voting.VotingDto;

/**
 * A voting prepared for display: both chart renderings and the names behind the seats.
 *
 * @param agendaItemTitle title of the linked agenda item, may be {@code null}
 * @param seatNames       membership id to member name for the member grid
 * @param bodySize        number of members who were entitled to vote
 * @param absent          number of absent members
 */
public record VotingView(VotingDto voting, String agendaItemTitle, ChartData summary, ChartData grid,
                         Map<Long, String> seatNames, Map<Long, VoteChoice> choiceByMember, int bodySize,
                         int absent, List<Segment> segments) {

    /**
     * One coloured part of the summary bar.
     */
    public record Segment(VoteChoice choice, int count, double percent) {
    }
}
