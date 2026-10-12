package ua.edu.ukma.oss.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.oss.notification.OutboxStatus;

class OutboxEntryTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");
    private static final Duration BACKOFF = Duration.ofSeconds(30);

    @Test
    void failedAttemptsBackOffExponentially() {
        OutboxEntry entry = new OutboxEntry(1L, "text", NOW);

        entry.markFailedAttempt("e", NOW, 5, BACKOFF);
        assertThat(entry.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(30));
        entry.markFailedAttempt("e", NOW, 5, BACKOFF);
        assertThat(entry.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(60));
        entry.markFailedAttempt("e", NOW, 5, BACKOFF);
        assertThat(entry.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(120));
        assertThat(entry.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    void entryFailsWhenAttemptBudgetIsSpent() {
        OutboxEntry entry = new OutboxEntry(1L, "text", NOW);

        entry.markFailedAttempt("e1", NOW, 2, BACKOFF);
        entry.markFailedAttempt("e2", NOW, 2, BACKOFF);

        assertThat(entry.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(entry.getNextAttemptAt()).isNull();
        assertThat(entry.getLastError()).isEqualTo("e2");
    }

    @Test
    void sentEntryClearsErrorAndSchedule() {
        OutboxEntry entry = new OutboxEntry(1L, "text", NOW);
        entry.markFailedAttempt("e", NOW, 5, BACKOFF);

        entry.markSent(NOW.plusSeconds(40));

        assertThat(entry.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(entry.getLastError()).isNull();
        assertThat(entry.getSentAt()).isEqualTo(NOW.plusSeconds(40));
    }

    @Test
    void requeueRestoresFullBudget() {
        OutboxEntry entry = new OutboxEntry(1L, "text", NOW);
        entry.markFailedAttempt("e", NOW, 1, BACKOFF);

        entry.requeue(NOW);

        assertThat(entry.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(entry.getAttempts()).isZero();
    }
}
