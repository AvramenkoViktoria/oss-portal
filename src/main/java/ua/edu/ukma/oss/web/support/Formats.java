package ua.edu.ukma.oss.web.support;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Date formatting for templates, in the portal's time zone and Ukrainian locale.
 */
public class Formats {

    private static final Locale UK = Locale.forLanguageTag("uk");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", UK);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", UK);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", UK);
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final ZoneId zone;

    public Formats(ZoneId zone) {
        this.zone = zone;
    }

    public String date(Instant instant) {
        return instant == null ? "" : DATE.format(instant.atZone(zone));
    }

    public String date(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    public String dateTime(Instant instant) {
        return instant == null ? "" : DATE_TIME.format(instant.atZone(zone));
    }

    public String shortDateTime(Instant instant) {
        return instant == null ? "" : SHORT.format(instant.atZone(zone));
    }

    /**
     * Value for an {@code <input type="datetime-local">}.
     */
    public String inputValue(Instant instant) {
        return instant == null ? "" : INPUT.format(instant.atZone(zone));
    }

    /**
     * Parses the value of an {@code <input type="datetime-local">} in the portal's time zone.
     */
    public Instant parseInput(String value) {
        return LocalDateTime.parse(value, INPUT).atZone(zone).toInstant();
    }
}
