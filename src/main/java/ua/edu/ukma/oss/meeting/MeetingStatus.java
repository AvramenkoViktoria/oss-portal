package ua.edu.ukma.oss.meeting;

/**
 * Lifecycle of a meeting: {@code PLANNED} moves once to either {@code HELD} or {@code CANCELLED}.
 */
public enum MeetingStatus {
    PLANNED,
    HELD,
    CANCELLED
}
