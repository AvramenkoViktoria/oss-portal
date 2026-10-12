package ua.edu.ukma.oss.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-0123";

    private static JwtTokenProvider provider(String secret, Instant now, Duration ttl) {
        return new JwtTokenProvider(new SecurityProperties(secret, ttl), Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void roundTripsUserId() {
        JwtTokenProvider provider = provider(SECRET, Instant.now(), Duration.ofHours(1));

        assertThat(provider.parse(provider.issue(42L))).contains(42L);
    }

    @Test
    void rejectsExpiredToken() {
        JwtTokenProvider issuer = provider(SECRET, Instant.now().minus(Duration.ofHours(3)), Duration.ofHours(1));

        assertThat(provider(SECRET, Instant.now(), Duration.ofHours(1)).parse(issuer.issue(1L))).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        JwtTokenProvider other = provider("another-secret-another-secret-0123456", Instant.now(), Duration.ofHours(1));

        assertThat(provider(SECRET, Instant.now(), Duration.ofHours(1)).parse(other.issue(1L))).isEmpty();
    }

    @Test
    void rejectsGarbage() {
        assertThat(provider(SECRET, Instant.now(), Duration.ofHours(1)).parse("not-a-token")).isEmpty();
    }

    @Test
    void refusesShortSecret() {
        assertThatThrownBy(() -> provider("short", Instant.now(), Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class);
    }
}
