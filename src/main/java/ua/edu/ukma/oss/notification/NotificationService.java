package ua.edu.ukma.oss.notification;

/**
 * Queues messages for delivery. The message is stored in the caller's transaction and sent
 * later, so a publication is never lost and never announced for a rolled-back change.
 */
public interface NotificationService {

    void notify(NotificationMessage message);
}
