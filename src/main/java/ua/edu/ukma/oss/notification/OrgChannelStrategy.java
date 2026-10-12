package ua.edu.ukma.oss.notification;

/**
 * Chooses the channel through which a body speaks.
 */
public interface OrgChannelStrategy {

    NotificationChannel resolve(Long orgId);
}
