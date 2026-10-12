package ua.edu.ukma.oss.voting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import org.junit.jupiter.api.Test;

class VoteViewTest {

    private final VoteViewFactory factory = new VoteViewFactory();
    private final VotingDto voting = new VotingDto(1L, 1L, null, "Q", VoteResult.ACCEPTED, 2, 1, 0);
    private final List<VoteRecordDto> records = List.of(
            new VoteRecordDto(10L, VoteChoice.FOR),
            new VoteRecordDto(11L, VoteChoice.FOR),
            new VoteRecordDto(12L, VoteChoice.AGAINST),
            new VoteRecordDto(13L, VoteChoice.ABSENT));

    @Test
    void summaryHasTotalsForEveryChoiceAndNoCells() {
        ChartData data = factory.summary().render(voting, records);

        assertThat(data.kind()).isEqualTo(ChartData.Kind.SUMMARY);
        assertThat(data.cells()).isEmpty();
        assertThat(data.slices()).extracting(ChartData.Slice::choice, ChartData.Slice::count).containsExactly(
                tuple(VoteChoice.FOR, 2),
                tuple(VoteChoice.AGAINST, 1),
                tuple(VoteChoice.ABSTAIN, 0),
                tuple(VoteChoice.ABSENT, 1));
    }

    @Test
    void gridHasOneCellPerMemberInOrder() {
        ChartData data = factory.create(4).render(voting, records);

        assertThat(data.kind()).isEqualTo(ChartData.Kind.MEMBER_GRID);
        assertThat(data.cells()).extracting(ChartData.Cell::membershipId).containsExactly(10L, 11L, 12L, 13L);
        assertThat(data.columns()).isEqualTo(4);
    }

    @Test
    void gridColumnsAreCappedForLargeBodies() {
        assertThat(factory.create(300).render(voting, records).columns()).isEqualTo(10);
    }
}
