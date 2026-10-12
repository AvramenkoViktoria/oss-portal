package ua.edu.ukma.oss.meeting;

import java.time.Instant;

/**
 * Published when a meeting is scheduled; consumed to announce it in the body's messenger channel.
 */
public record MeetingScheduledEvent(Long meetingId, Long orgId, String title, Instant scheduledAt, String streamUrl) {
}
