package ua.edu.ukma.oss.voting.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static ua.edu.ukma.oss.voting.VoteChoice.ABSENT;
import static ua.edu.ukma.oss.voting.VoteChoice.ABSTAIN;
import static ua.edu.ukma.oss.voting.VoteChoice.AGAINST;
import static ua.edu.ukma.oss.voting.VoteChoice.FOR;

import java.util.List;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.oss.voting.VoteResult;

class VoteCounterTest {

    @Test
    void countsEveryChoice() {
        VoteCounter.Counts c = VoteCounter.count(List.of(FOR, FOR, AGAINST, ABSTAIN, ABSENT));

        assertThat(c.votesFor()).isEqualTo(2);
        assertThat(c.votesAgainst()).isEqualTo(1);
        assertThat(c.votesAbstain()).isEqualTo(1);
        assertThat(c.votesAbsent()).isEqualTo(1);
        assertThat(c.bodySize()).isEqualTo(5);
    }

    @Test
    void acceptedWhenMoreThanHalfOfCompositionVotedFor() {
        assertThat(VoteCounter.count(List.of(FOR, FOR, FOR, AGAINST, ABSENT)).result()).isEqualTo(VoteResult.ACCEPTED);
    }

    @Test
    void exactlyHalfIsNotEnough() {
        assertThat(VoteCounter.count(List.of(FOR, FOR, AGAINST, AGAINST)).result()).isEqualTo(VoteResult.REJECTED);
    }

    @Test
    void absenceCountsAgainstAcceptance() {
        // Two of five voted for, the rest are absent or abstained: not a majority of the composition.
        assertThat(VoteCounter.count(List.of(FOR, FOR, ABSENT, ABSENT, ABSTAIN)).result()).isEqualTo(VoteResult.REJECTED);
    }
}
