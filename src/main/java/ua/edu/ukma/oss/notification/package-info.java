/**
 * Notifications: transactional outbox of announcements and protocols, delivered to the
 * messenger of the owning body (WhatsApp for SK, Telegram for KS) with retries.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Notifications")
package ua.edu.ukma.oss.notification;
