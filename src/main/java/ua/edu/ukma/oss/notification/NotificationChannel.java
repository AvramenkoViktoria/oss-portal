package ua.edu.ukma.oss.notification;

/**
 * Abstraction over an external messenger. Callers never see which messenger sits behind it.
 */
public interface NotificationChannel {

    SendResult send(String channelId, String text);
}
