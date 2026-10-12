package ua.edu.ukma.oss.voting;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Totals per choice: for, against, abstained, absent.
 */
public class SummaryChartView implements VoteView {

    @Override
    public ChartData render(VotingDto voting, List<VoteRecordDto> records) {
        Map<VoteChoice, Integer> totals = new EnumMap<>(VoteChoice.class);
        Arrays.stream(VoteChoice.values()).forEach(c -> totals.put(c, 0));
        records.forEach(r -> totals.merge(r.choice(), 1, Integer::sum));
        List<ChartData.Slice> slices = Arrays.stream(VoteChoice.values())
                .map(c -> new ChartData.Slice(c, totals.get(c))).toList();
        return new ChartData(ChartData.Kind.SUMMARY, slices, List.of(), 0);
    }
}
