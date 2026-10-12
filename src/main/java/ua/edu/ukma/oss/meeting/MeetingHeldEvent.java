package ua.edu.ukma.oss.meeting;

/**
 * Published when a meeting is marked as held. Listeners run in the publisher's transaction.
 */
public record MeetingHeldEvent(Long meetingId, Long orgId) {
}
