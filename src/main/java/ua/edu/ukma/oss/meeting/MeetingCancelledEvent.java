package ua.edu.ukma.oss.meeting;

/**
 * Published when a planned meeting is cancelled.
 */
public record MeetingCancelledEvent(Long meetingId, Long orgId, String title) {
}
