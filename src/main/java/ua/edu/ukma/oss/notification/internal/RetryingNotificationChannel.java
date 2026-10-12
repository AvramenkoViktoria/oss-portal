package ua.edu.ukma.oss.notification.internal;

import ua.edu.ukma.oss.notification.NotificationChannel;
import ua.edu.ukma.oss.notification.SendResult;

/**
 * Decorator that repeats a failed send a few times in a row to absorb transient glitches.
 * Longer outages are handled by the outbox backoff, not here.
 */
class RetryingNotificationChannel implements NotificationChannel {

    private final NotificationChannel delegate;
    private final int attempts;

    RetryingNotificationChannel(NotificationChannel delegate, int attempts) {
        this.delegate = delegate;
        this.attempts = Math.max(1, attempts);
    }

    @Override
    public SendResult send(String channelId, String text) {
        SendResult result = SendResult.failed("not attempted");
        for (int i = 0; i < attempts; i++) {
            result = delegate.send(channelId, text);
            if (result.success()) {
                return result;
            }
        }
        return result;
    }
}
