package ua.edu.ukma.oss.voting;

import java.util.List;

/**
 * Strategy that turns a voting and its recorded votes into chart data.
 */
public interface VoteView {

    ChartData render(VotingDto voting, List<VoteRecordDto> records);
}
