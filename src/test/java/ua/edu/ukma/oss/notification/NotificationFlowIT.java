package ua.edu.ukma.oss.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ua.edu.ukma.oss.organization.Messenger;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.support.IntegrationTest;

/**
 * Not transactional on purpose: the delivery loop commits each entry in its own transaction,
 * so the queued rows must be committed to be visible to it.
 */
@IntegrationTest
class NotificationFlowIT {

    @Autowired
    NotificationService notifications;
    @Autowired
    NotificationAdminService admin;
    @Autowired
    OrganizationService organizations;

    @AfterEach
    void clearOutages() {
        admin.simulateOutage(Messenger.WHATSAPP, false);
        admin.simulateOutage(Messenger.TELEGRAM, false);
    }

    private OutboxEntryDto queue(OrgCode org) {
        String text = "test-" + UUID.randomUUID();
        notifications.notify(new NotificationMessage(organizations.getByCode(org).id(), text));
        return admin.recent(50).stream().filter(e -> e.payload().equals(text)).findFirst().orElseThrow();
    }

    private OutboxEntryDto reload(OutboxEntryDto entry) {
        return admin.recent(50).stream().filter(e -> e.id().equals(entry.id())).findFirst().orElseThrow();
    }

    @Test
    void skMessageIsDeliveredThroughWhatsApp() {
        OutboxEntryDto entry = queue(OrgCode.SK);
        assertThat(entry.status()).isEqualTo(OutboxStatus.PENDING);

        admin.processDueNow();

        assertThat(reload(entry).status()).isEqualTo(OutboxStatus.SENT);
        assertThat(admin.deliveredMessages())
                .anyMatch(m -> m.text().equals(entry.payload()) && m.messenger() == Messenger.WHATSAPP);
    }

    @Test
    void ksMessageIsDeliveredThroughTelegram() {
        OutboxEntryDto entry = queue(OrgCode.KS);

        admin.processDueNow();

        assertThat(admin.deliveredMessages())
                .anyMatch(m -> m.text().equals(entry.payload()) && m.messenger() == Messenger.TELEGRAM);
    }

    @Test
    void outageKeepsMessageQueuedThenFailsAndRequeueDeliversIt() {
        admin.simulateOutage(Messenger.TELEGRAM, true);
        OutboxEntryDto entry = queue(OrgCode.KS);

        admin.processDueNow();
        OutboxEntryDto afterFirst = reload(entry);
        assertThat(afterFirst.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(afterFirst.attempts()).isEqualTo(1);
        assertThat(afterFirst.lastError()).contains("outage");

        admin.processDueNow();
        admin.processDueNow();
        assertThat(reload(entry).status()).isEqualTo(OutboxStatus.FAILED);

        admin.simulateOutage(Messenger.TELEGRAM, false);
        admin.requeue(entry.id());
        admin.processDueNow();
        assertThat(reload(entry).status()).isEqualTo(OutboxStatus.SENT);
    }
}
