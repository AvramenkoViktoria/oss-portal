package ua.edu.ukma.oss.auth.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Token settings bound from {@code oss.security.*}.
 *
 * @param jwtSecret HMAC key, at least 32 bytes
 * @param tokenTtl  lifetime of an issued token
 */
@ConfigurationProperties("oss.security")
record SecurityProperties(String jwtSecret, Duration tokenTtl) {
}
