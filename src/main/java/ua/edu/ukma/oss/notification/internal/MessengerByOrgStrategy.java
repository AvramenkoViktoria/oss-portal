package ua.edu.ukma.oss.notification.internal;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import ua.edu.ukma.oss.notification.NotificationChannel;
import ua.edu.ukma.oss.notification.OrgChannelStrategy;
import ua.edu.ukma.oss.organization.Messenger;
import ua.edu.ukma.oss.organization.OrganizationService;

/**
 * Picks the messenger configured on the organization (SK: WhatsApp, KS: Telegram) and wraps
 * it with retries.
 */
@Component
class MessengerByOrgStrategy implements OrgChannelStrategy {

    private final OrganizationService organizations;
    private final Map<Messenger, MessengerChannel> channels = new EnumMap<>(Messenger.class);
    private final int immediateRetries;

    MessengerByOrgStrategy(OrganizationService organizations, List<MessengerChannel> available,
                           NotificationProperties properties) {
        this.organizations = organizations;
        available.forEach(c -> channels.put(c.messenger(), c));
        this.immediateRetries = properties.immediateRetries();
    }

    @Override
    public NotificationChannel resolve(Long orgId) {
        Messenger messenger = organizations.get(orgId).messenger();
        MessengerChannel channel = channels.get(messenger);
        if (channel == null) {
            throw new IllegalStateException("No channel registered for messenger " + messenger);
        }
        return new RetryingNotificationChannel(channel, immediateRetries);
    }
}
