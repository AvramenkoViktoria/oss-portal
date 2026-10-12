package ua.edu.ukma.oss.notification.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings bound from {@code oss.notification.*}.
 *
 * @param maxAttempts      outbox attempts before an entry is marked failed
 * @param immediateRetries sends tried back-to-back by the retrying channel within one attempt
 * @param backoff          delay before the second attempt; doubles with each further one
 * @param publicBaseUrl    site address used in links inside messages
 */
@ConfigurationProperties("oss.notification")
record NotificationProperties(int maxAttempts, int immediateRetries, Duration backoff, String publicBaseUrl) {
}
