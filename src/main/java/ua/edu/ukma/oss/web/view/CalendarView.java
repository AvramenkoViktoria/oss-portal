package ua.edu.ukma.oss.web.view;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import ua.edu.ukma.oss.meeting.MeetingDto;

/**
 * Month grid of meetings, Monday first. Days of the neighbouring months that complete the first
 * and last week are included and flagged so the template can dim them.
 *
 * @param weeks always whole weeks of seven days
 */
public record CalendarView(YearMonth month, YearMonth previous, YearMonth next, List<List<Day>> weeks) {

    /**
     * One cell of the grid.
     */
    public record Day(LocalDate date, boolean inMonth, boolean today, List<MeetingDto> meetings) {
    }

    /**
     * Places meetings on the days of the month, using the portal's time zone to decide the day.
     */
    public static CalendarView of(YearMonth month, ZoneId zone, LocalDate today, List<MeetingDto> meetings) {
        Map<LocalDate, List<MeetingDto>> byDay = meetings.stream()
                .sorted(Comparator.comparing(MeetingDto::scheduledAt))
                .collect(Collectors.groupingBy(m -> m.scheduledAt().atZone(zone).toLocalDate()));

        LocalDate first = month.atDay(1);
        LocalDate cursor = first.minusDays(first.getDayOfWeek().getValue() - 1L);
        LocalDate last = month.atEndOfMonth();
        List<List<Day>> weeks = new ArrayList<>();
        while (!cursor.isAfter(last)) {
            List<Day> week = new ArrayList<>(7);
            for (int i = 0; i < 7; i++) {
                week.add(new Day(cursor, YearMonth.from(cursor).equals(month), cursor.equals(today),
                        byDay.getOrDefault(cursor, List.of())));
                cursor = cursor.plusDays(1);
            }
            weeks.add(week);
        }
        return new CalendarView(month, month.minusMonths(1), month.plusMonths(1), weeks);
    }

    /**
     * Reads a {@code yyyy-MM} request parameter; anything missing or malformed falls back to the given month.
     */
    public static YearMonth parseMonth(String value, YearMonth fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }
}
