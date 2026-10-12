package ua.edu.ukma.oss.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingStatus;

class CalendarViewTest {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    private static MeetingDto meeting(long id, String instant) {
        return new MeetingDto(id, 1L, "M" + id, Instant.parse(instant), null, MeetingStatus.PLANNED, List.of());
    }

    @Test
    void gridConsistsOfWholeMondayFirstWeeks() {
        CalendarView view = CalendarView.of(YearMonth.of(2026, 10), KYIV, LocalDate.of(2026, 10, 12), List.of());

        assertThat(view.weeks()).allSatisfy(week -> assertThat(week).hasSize(7));
        assertThat(view.weeks().getFirst().getFirst().date().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(view.weeks().getLast().getLast().date().getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        // October 2026 starts on Thursday and ends on Saturday: five weeks.
        assertThat(view.weeks()).hasSize(5);
    }

    @Test
    void marksDaysOutsideTheMonthAndToday() {
        CalendarView view = CalendarView.of(YearMonth.of(2026, 10), KYIV, LocalDate.of(2026, 10, 12), List.of());

        var all = view.weeks().stream().flatMap(List::stream).toList();
        assertThat(all.getFirst().inMonth()).isFalse();
        assertThat(all).filteredOn(CalendarView.Day::inMonth).hasSize(31);
        assertThat(all).filteredOn(CalendarView.Day::today).singleElement()
                .extracting(CalendarView.Day::date).isEqualTo(LocalDate.of(2026, 10, 12));
    }

    @Test
    void placesMeetingsOnTheirLocalDay() {
        // 22:30 UTC on the 14th is already the 15th in Kyiv (UTC+3 in October).
        MeetingDto late = meeting(1, "2026-10-14T22:30:00Z");
        MeetingDto morning = meeting(2, "2026-10-15T06:00:00Z");

        CalendarView view = CalendarView.of(YearMonth.of(2026, 10), KYIV, LocalDate.of(2026, 10, 1),
                List.of(morning, late));

        var day15 = view.weeks().stream().flatMap(List::stream)
                .filter(d -> d.date().equals(LocalDate.of(2026, 10, 15))).findFirst().orElseThrow();
        assertThat(day15.meetings()).extracting(MeetingDto::id).containsExactly(1L, 2L);
    }

    @Test
    void navigationPointsToNeighbourMonthsAcrossYearEnd() {
        CalendarView view = CalendarView.of(YearMonth.of(2026, 12), KYIV, LocalDate.of(2026, 12, 1), List.of());

        assertThat(view.next()).isEqualTo(YearMonth.of(2027, 1));
        assertThat(view.previous()).isEqualTo(YearMonth.of(2026, 11));
    }

    @Test
    void parsesMonthParameterWithFallback() {
        YearMonth fallback = YearMonth.of(2026, 10);

        assertThat(CalendarView.parseMonth("2027-03", fallback)).isEqualTo(YearMonth.of(2027, 3));
        assertThat(CalendarView.parseMonth("nonsense", fallback)).isEqualTo(fallback);
        assertThat(CalendarView.parseMonth(null, fallback)).isEqualTo(fallback);
    }
}
