package ua.edu.ukma.oss.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ua.edu.ukma.oss.meeting.MeetingCancelledEvent;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingScheduledEvent;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.meeting.MeetingStatus;
import ua.edu.ukma.oss.notification.NotificationMessage;
import ua.edu.ukma.oss.protocol.ProtocolPublishedEvent;

class NotificationServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    private final OutboxRepository outbox = mock(OutboxRepository.class);
    private final MeetingService meetings = mock(MeetingService.class);
    private final NotificationServiceImpl service = new NotificationServiceImpl(outbox, meetings,
            new NotificationProperties(3, 2, Duration.ofSeconds(1), "https://oss.example"),
            Clock.fixed(NOW, ZoneOffset.UTC));

    private String queuedText() {
        ArgumentCaptor<OutboxEntry> captor = ArgumentCaptor.forClass(OutboxEntry.class);
        verify(outbox).save(captor.capture());
        return (String) org.springframework.test.util.ReflectionTestUtils.getField(captor.getValue(), "payload");
    }

    @Test
    void notifyQueuesTheMessage() {
        service.notify(new NotificationMessage(1L, "Привіт"));

        assertThat(queuedText()).isEqualTo("Привіт");
    }

    @Test
    void scheduledMeetingIsAnnouncedWithStreamLink() {
        service.onMeetingScheduled(new MeetingScheduledEvent(4L, 1L, "Засідання", NOW, "https://stream"));

        assertThat(queuedText()).contains("Анонс засідання «Засідання»").contains("Трансляція: https://stream");
    }

    @Test
    void scheduledMeetingWithoutStreamHasNoLink() {
        service.onMeetingScheduled(new MeetingScheduledEvent(4L, 1L, "Засідання", NOW, null));

        assertThat(queuedText()).doesNotContain("Трансляція");
    }

    @Test
    void cancelledMeetingIsAnnounced() {
        service.onMeetingCancelled(new MeetingCancelledEvent(4L, 1L, "Засідання"));

        assertThat(queuedText()).contains("скасовано");
    }

    @Test
    void publishedProtocolLinksToTheMeetingPage() {
        when(meetings.get(4L)).thenReturn(new MeetingDto(4L, 1L, "Засідання", NOW, null, MeetingStatus.HELD, List.of()));

        service.onProtocolPublished(new ProtocolPublishedEvent(9L, 1L, 4L));

        assertThat(queuedText()).contains("«Засідання»").contains("https://oss.example/meetings/4");
    }
}
