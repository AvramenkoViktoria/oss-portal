package ua.edu.ukma.oss.notification;

import java.time.Instant;

/**
 * Read model of a queued message for the administrator's monitor.
 */
public record OutboxEntryDto(Long id, Long orgId, String payload, OutboxStatus status, int attempts,
                             Instant nextAttemptAt, String lastError, Instant createdAt, Instant sentAt) {
}
