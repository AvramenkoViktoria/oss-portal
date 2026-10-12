package ua.edu.ukma.oss.notification.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import ua.edu.ukma.oss.notification.OutboxStatus;
import ua.edu.ukma.oss.shared.BaseEntity;

/**
 * A message waiting for, or already past, delivery. Retry bookkeeping lives here so the
 * delivery loop stays a thin driver.
 */
@Entity
@Table(name = "notification_outbox")
class OutboxEntry extends BaseEntity {

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected OutboxEntry() {
    }

    OutboxEntry(Long orgId, String payload, Instant now) {
        this.orgId = orgId;
        this.payload = payload;
        this.nextAttemptAt = now;
    }

    Long getOrgId() {
        return orgId;
    }

    String getPayload() {
        return payload;
    }

    OutboxStatus getStatus() {
        return status;
    }

    int getAttempts() {
        return attempts;
    }

    Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    String getLastError() {
        return lastError;
    }

    Instant getSentAt() {
        return sentAt;
    }

    void markSent(Instant now) {
        status = OutboxStatus.SENT;
        sentAt = now;
        nextAttemptAt = null;
        lastError = null;
    }

    /**
     * Records a failed attempt: schedules the next one with exponential backoff, or gives up
     * once the budget is spent.
     */
    void markFailedAttempt(String error, Instant now, int maxAttempts, Duration baseBackoff) {
        attempts++;
        lastError = error;
        if (attempts >= maxAttempts) {
            status = OutboxStatus.FAILED;
            nextAttemptAt = null;
        } else {
            nextAttemptAt = now.plus(baseBackoff.multipliedBy(1L << (attempts - 1)));
        }
    }

    void requeue(Instant now) {
        status = OutboxStatus.PENDING;
        attempts = 0;
        nextAttemptAt = now;
    }
}
