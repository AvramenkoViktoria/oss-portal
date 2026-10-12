package ua.edu.ukma.oss.notification;

import java.time.Instant;
import ua.edu.ukma.oss.organization.Messenger;

/**
 * A message "delivered" by a stub messenger, kept in memory for inspection.
 */
public record StubMessageDto(Messenger messenger, String channelId, String text, Instant sentAt) {
}
