package ua.edu.ukma.oss.notification;

/**
 * A message addressed to the public channel of a body.
 */
public record NotificationMessage(Long orgId, String text) {
}
