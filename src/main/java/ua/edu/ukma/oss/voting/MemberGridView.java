package ua.edu.ukma.oss.voting;

import java.util.List;

/**
 * One cell per member in seat order, like the voting screen of the Verkhovna Rada.
 */
public class MemberGridView implements VoteView {

    private static final int MAX_COLUMNS = 10;

    private final int columns;

    public MemberGridView(int bodySize) {
        this.columns = Math.max(1, Math.min(MAX_COLUMNS, bodySize));
    }

    @Override
    public ChartData render(VotingDto voting, List<VoteRecordDto> records) {
        List<ChartData.Cell> cells = records.stream()
                .map(r -> new ChartData.Cell(r.membershipId(), r.choice())).toList();
        List<ChartData.Slice> slices = new SummaryChartView().render(voting, records).slices();
        return new ChartData(ChartData.Kind.MEMBER_GRID, slices, cells, columns);
    }
}
