package ua.edu.ukma.oss.meeting;

import java.time.Instant;

public record ScheduleCmd(Long orgId, String title, Instant scheduledAt, String streamUrl) {
}
