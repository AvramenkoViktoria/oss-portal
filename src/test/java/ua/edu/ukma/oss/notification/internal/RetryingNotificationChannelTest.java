package ua.edu.ukma.oss.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.oss.notification.NotificationChannel;
import ua.edu.ukma.oss.notification.SendResult;

class RetryingNotificationChannelTest {

    private static NotificationChannel failingTimes(int failures, AtomicInteger calls) {
        return (channelId, text) -> calls.incrementAndGet() <= failures
                ? SendResult.failed("boom " + calls.get())
                : SendResult.ok();
    }

    @Test
    void succeedsAfterTransientFailures() {
        AtomicInteger calls = new AtomicInteger();
        RetryingNotificationChannel channel = new RetryingNotificationChannel(failingTimes(2, calls), 3);

        assertThat(channel.send("c", "t").success()).isTrue();
        assertThat(calls).hasValue(3);
    }

    @Test
    void givesUpAfterConfiguredAttemptsAndReportsLastError() {
        AtomicInteger calls = new AtomicInteger();
        RetryingNotificationChannel channel = new RetryingNotificationChannel(failingTimes(10, calls), 3);

        SendResult result = channel.send("c", "t");

        assertThat(result.success()).isFalse();
        assertThat(result.error()).isEqualTo("boom 3");
        assertThat(calls).hasValue(3);
    }

    @Test
    void successfulSendIsNotRepeated() {
        AtomicInteger calls = new AtomicInteger();
        new RetryingNotificationChannel(failingTimes(0, calls), 5).send("c", "t");

        assertThat(calls).hasValue(1);
    }
}
