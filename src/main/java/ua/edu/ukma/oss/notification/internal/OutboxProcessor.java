package ua.edu.ukma.oss.notification.internal;

import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ua.edu.ukma.oss.notification.OrgChannelStrategy;
import ua.edu.ukma.oss.notification.OutboxStatus;
import ua.edu.ukma.oss.notification.SendResult;
import ua.edu.ukma.oss.organization.OrganizationService;

/**
 * Delivers due outbox entries. Each entry is handled in its own transaction so one failing
 * message cannot roll back or block the others.
 */
@Component
class OutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);

    private final OutboxRepository outbox;
    private final OrgChannelStrategy strategy;
    private final OrganizationService organizations;
    private final NotificationProperties properties;
    private final Clock clock;
    private final TransactionTemplate tx;

    OutboxProcessor(OutboxRepository outbox, OrgChannelStrategy strategy, OrganizationService organizations,
                    NotificationProperties properties, Clock clock, PlatformTransactionManager txManager) {
        this.outbox = outbox;
        this.strategy = strategy;
        this.organizations = organizations;
        this.properties = properties;
        this.clock = clock;
        this.tx = new TransactionTemplate(txManager);
    }

    @Scheduled(fixedDelayString = "${oss.notification.poll-interval:10s}")
    void poll() {
        processDue();
    }

    /**
     * @return number of entries attempted
     */
    int processDue() {
        List<Long> ids = outbox.findDueIds(clock.instant());
        ids.forEach(id -> tx.executeWithoutResult(status -> deliver(id)));
        return ids.size();
    }

    private void deliver(Long id) {
        OutboxEntry entry = outbox.findById(id).orElse(null);
        if (entry == null || entry.getStatus() != OutboxStatus.PENDING) {
            return;
        }
        SendResult result;
        try {
            String channelId = organizations.get(entry.getOrgId()).channelId();
            result = strategy.resolve(entry.getOrgId()).send(channelId, entry.getPayload());
        } catch (RuntimeException e) {
            log.warn("Delivery of outbox entry {} failed unexpectedly", id, e);
            result = SendResult.failed(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        if (result.success()) {
            entry.markSent(clock.instant());
        } else {
            entry.markFailedAttempt(result.error(), clock.instant(), properties.maxAttempts(), properties.backoff());
        }
    }
}
