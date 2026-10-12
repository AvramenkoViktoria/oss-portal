package ua.edu.ukma.oss.organization;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MembershipTermTest {

    private static final LocalDate START = LocalDate.of(2026, 9, 1);
    private static final LocalDate END = LocalDate.of(2027, 6, 30);

    @Test
    void termBoundsAreInclusive() {
        MembershipDto dto = new MembershipDto(1L, 1L, 1L, "FI", "Member", START, END);

        assertThat(dto.isActiveOn(START)).isTrue();
        assertThat(dto.isActiveOn(END)).isTrue();
        assertThat(dto.isActiveOn(START.minusDays(1))).isFalse();
        assertThat(dto.isActiveOn(END.plusDays(1))).isFalse();
    }

    @Test
    void openEndedTermNeverExpires() {
        MembershipDto dto = new MembershipDto(1L, 1L, 1L, "FI", "Member", START, null);

        assertThat(dto.isActiveOn(LocalDate.of(2040, 1, 1))).isTrue();
    }
}
